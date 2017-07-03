package org.rutebanken.helper.organisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.rutebanken.helper.organisation.AuthorizationConstants.*;

@Service
public abstract class ReflectionAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionAuthorizationService.class);

    private final RoleAssignmentExtractor roleAssignmentExtractor;

    private final boolean authorizationEnabled;

    public ReflectionAuthorizationService(RoleAssignmentExtractor roleAssignmentExtractor, boolean authorizationEnabled) {
        this.roleAssignmentExtractor = roleAssignmentExtractor;
        this.authorizationEnabled = authorizationEnabled;
    }

    public abstract boolean entityMatchesAdministrativeZone(RoleAssignment roleAssignment, Object entity);

    public abstract boolean entityMatchesOrganisationRef(RoleAssignment roleAssignment, Object entity);

    /**
     * If entity itself cannot be checked for authorization, but the owning entity can.
     * For instance, if a Quay belongs to StopPlace, the Quay cannot be checked, but the StopPlace can.
     *
     * @param entity child entity
     * @return the parent entity to check for authorization
     */
    public abstract Object resolveCorrectEntity(Object entity);

    public void assertAuthorized(String requiredRole, Collection<?> entities) {

        final boolean allowed = isAuthorized(requiredRole, entities);
        if (!allowed) {
            throw new AccessDeniedException("Insufficient privileges for operation");
        }
    }


    public boolean isAuthorized(String requiredRole, Collection<?> entities) {
        if (!authorizationEnabled) {
            return true;
        }

        logger.debug("Checking if authorized for entities: {}", entities);

        List<RoleAssignment> relevantRoles = roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .filter(roleAssignment -> requiredRole.equals(roleAssignment.r))
                .collect(toList());

        for (Object entity : entities) {
            boolean allowed = entity == null ||
                    relevantRoles
                            .stream()
                            // Only one of the role assignments needs to match for the given entity and required role
                            .anyMatch(roleAssignment -> authorized(roleAssignment, entity, requiredRole));
            if(!allowed) {
                // No need to loop further, if not authorized with required role for one of the entities in collection.
                logger.info("User is not authorized for entity with role: {}. Relevant roles: {}. Entity: {}", requiredRole, relevantRoles, entity);
                return false;
            }

        }
        return true;
    }

    public Set<String> getRelevantRolesForEntity(Object entity) {
        return roleAssignmentExtractor.getRoleAssignmentsForUser().stream()
                .filter(roleAssignment -> roleAssignment.getEntityClassifications().get(ENTITY_TYPE).stream()
                        .anyMatch(entityTypeString -> entityTypeString.toLowerCase().equals(entity.getClass().getSimpleName().toLowerCase())
                                || entityTypeString.contains(ENTITY_CLASSIFIER_ALL_TYPES)))
                .map(roleAssignment -> roleAssignment.getRole())
                .collect(Collectors.toSet());
    }

    public boolean authorized(RoleAssignment roleAssignment, Object entity, String requiredRole) {

        entity = resolveCorrectEntity(entity);

        if (roleAssignment.getEntityClassifications() == null) {
            logger.warn("Role assignment entity classifications cannot be null: {}", roleAssignment);
            return false;
        }

        if (!roleAssignment.getRole().equals(requiredRole)) {
            logger.debug("No role match for required role {}, {}", requiredRole, roleAssignment);
            return false;
        }

        if(!entityMatchesOrganisationRef(roleAssignment, entity)) {
            logger.debug("Entity does not match organization ref. RoleAssignment: {}, Entity: {}", roleAssignment, entity);
            return false;
        }

        String entityTypename = entity.getClass().getSimpleName();

        if (!checkEntityClassifications(entityTypename, entity, roleAssignment, requiredRole)) {
            logger.debug("Entity classification. Not authorized: {}, {}", requiredRole, roleAssignment);
            return false;
        }

        if (!checkAdministrativeZone(roleAssignment, entity)) {
            logger.debug("Entity type administrative zone no match: {} entity: {}", roleAssignment, entity);
            return false;
        }

        return true;
    }

    private boolean checkEntityClassifications(String entityTypename, Object entity, RoleAssignment roleAssignment, String requiredRole) {

        if (!containsEntityTypeOrAll(roleAssignment, entityTypename)) {
            logger.debug("No match for entity type {} for required role {}. Role assignment: {}",
                    entity.getClass().getSimpleName(), requiredRole, roleAssignment);
            return false;
        }

        for (String entityType : roleAssignment.getEntityClassifications().keySet()) {
            boolean authorized = checkEntityClassification(entityType, entity, roleAssignment.getEntityClassifications().get(entityType));

            if (!authorized) {
                logger.info ("Not authorized for entity {} and role assignment {}", entity, roleAssignment);
                return false;
            }

        }
        return true;
    }

    private boolean checkEntityClassification(String entityType, Object entity, List<String> classificationsForEntityType) {
        if (entityType.equals(ENTITY_TYPE)) {
            // Already checked
            return true;
        }

        if (classificationsForEntityType.contains(ENTITY_CLASSIFIER_ALL_ATTRIBUTES)) {
            logger.debug("Contains {} for {}", ENTITY_CLASSIFIER_ALL_ATTRIBUTES, entityType);
            return true;

        }

        Optional<Field> optionalField = findFieldFromClassifier(entityType, entity);

        if (!optionalField.isPresent()) {
            logger.debug("Cannot fetch field for entity type {}. entity: {}", entityType, entity);
            return true;
        }

        Field field = optionalField.get();

        logger.debug("Found field {} from classifier {}", field, entityType);

        field.setAccessible(true);
        Optional<Object> optionalValue = getFieldValue(field, entity);

        if (!optionalValue.isPresent()) {
            logger.debug("Cannot resolve value for {}, entity: {}", field, entity);
            return true;
        }

        Object value = optionalValue.get();
        String stringValue = removeUnderscore(value.toString());

        boolean isBlacklist = classificationsForEntityType.stream().anyMatch(classifier -> classifier.startsWith("!"));
        boolean isWhiteList = classificationsForEntityType.stream().noneMatch(classifier -> classifier.startsWith("!"));

        if(isBlacklist && isWhiteList) {
            logger.warn("The list of classifiers contains values with both black list values (values prefixed with !) and white list values. This is not supported");
            return false;
        }

        boolean isAllowed;
        if(isBlacklist) {

            isAllowed = classificationsForEntityType.stream()
                    .noneMatch(classification ->
                            removeUnderscore(classification.substring(1))
                                    .equalsIgnoreCase(stringValue));



        } else {
            isAllowed = classificationsForEntityType.stream()
                    .anyMatch(classification ->
                            removeUnderscore(classification)
                                    .equalsIgnoreCase(stringValue));


        }

        if(isAllowed) {
            logger.info("Not allowed for value {} for entity {}", value, entity);
            return true;
        }
        return isAllowed;
    }

    private Optional<Field> findFieldFromClassifier(String classifier, Object entity) {
        return Stream.of(entity.getClass().getDeclaredFields())
                .filter(field -> classifier.equalsIgnoreCase(field.getName()))
                .findFirst();
    }

    private Optional<Object> getFieldValue(Field field, Object object) {
        try {
            return Optional.ofNullable(field.get(object));
        } catch (IllegalAccessException e) {
            logger.warn("Could not access field value {} - {}", field, object);
            return Optional.empty();
        }
    }

    private String removeUnderscore(String string) {
        return string.replaceAll("_", "");
    }

    public boolean checkAdministrativeZone(RoleAssignment roleAssignment, Object entity) {
        return roleAssignment.getAdministrativeZone() == null
                || roleAssignment.getAdministrativeZone().isEmpty()
                || entityMatchesAdministrativeZone(roleAssignment, entity);

    }

    private boolean containsEntityTypeOrAll(RoleAssignment roleAssignment, String entityTypeName) {

        List<String> classifiers = roleAssignment.getEntityClassifications().get(ENTITY_TYPE);

        if(classifiers == null || classifiers.isEmpty()) {
            logger.warn("Classifiers is empty for {}", ENTITY_TYPE);
            return false;
        }

        for (String entityType : classifiers) {
            if (entityType.equalsIgnoreCase(entityTypeName)) {
                return true;
            }
            if (ENTITY_CLASSIFIER_ALL_TYPES.equals(entityType)) {
                return true;
            }
        }

        return false;
    }
}
