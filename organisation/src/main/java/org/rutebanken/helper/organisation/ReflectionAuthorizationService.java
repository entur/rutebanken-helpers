package org.rutebanken.helper.organisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.rutebanken.helper.organisation.AuthorizationConstants.*;

@Service
public abstract class ReflectionAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionAuthorizationService.class);


    public boolean authorized(RoleAssignment roleAssignment, Object entity, String requiredRole) {


        if (roleAssignment.getEntityClassifications() == null) {
            logger.warn("Role assignment entity classifications cannot be null: {}", roleAssignment);
            return false;
        }

        if (!roleAssignment.getRole().equals(requiredRole)) {
            logger.debug("No role match for required role {}, {}", requiredRole, roleAssignment);
            return false;
        }

        // Organization check ?

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

        boolean anyMatch = classificationsForEntityType.stream()
                .anyMatch(classification -> classificationMatchesObjectValue(classification, value));


        if(!anyMatch) {
            logger.debug("Classification does not match on object value. Not authorized." +
                    " EntityType: {}. Entity: {}", entityType, entity);
            return false;
        }
        return true;
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

    private boolean classificationMatchesObjectValue(String classification, Object value) {
        boolean negate = classification.startsWith("!");

        if (negate) {
            classification = classification.substring(1);
        }

        if (value.toString().equalsIgnoreCase(classification)) {
            return !negate;
        }
        return negate;
    }


    public boolean checkAdministrativeZone(RoleAssignment roleAssignment, Object entity) {
        if (roleAssignment.getAdministrativeZone() == null || roleAssignment.getAdministrativeZone().isEmpty()) {
            return true;
        }

        return entityAllowedInAdministrativeZone(roleAssignment, entity);
    }

    abstract boolean entityAllowedInAdministrativeZone(RoleAssignment roleAssignment, Object entity);

    private boolean containsEntityTypeOrAll(RoleAssignment roleAssignment, String entityTypeName) {

        for (String entityType : roleAssignment.getEntityClassifications().get(ENTITY_TYPE)) {
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
