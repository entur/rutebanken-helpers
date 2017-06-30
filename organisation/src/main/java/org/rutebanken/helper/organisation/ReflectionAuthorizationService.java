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
public class ReflectionAuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionAuthorizationService.class);


    public boolean authorized(RoleAssignment roleAssignment, Object entity, String requiredRole) {


        if (roleAssignment.getEntityClassifications() == null) {
            logger.info("Role assignment entity classifications cannot be null");
            return false;
        }

        if (!roleAssignment.getRole().equals(requiredRole)) {
            logger.debug("No role match for required role {}, {}", requiredRole, roleAssignment);
            return false;
        }

        // Org check ?

        String entityTypename = entity.getClass().getSimpleName();

        if (!checkEntityClassifications(entityTypename, entity, roleAssignment, requiredRole)) {
            logger.debug("Entity classification. Not authorized: {}, {}", requiredRole, roleAssignment);
            return false;
        }


        if (!checkAdministrativeZone(roleAssignment, entity)) {
            logger.info("Entity type does not match");
            return false;
        }

        return true;
    }

    private boolean checkEntityClassifications(String entityTypename, Object entity, RoleAssignment roleAssignment, String requiredRole) {

        if (!containsEntityTypeOrAll(roleAssignment, entityTypename)) {
            logger.info("No match for entity type {} for required role {}. Role assignment: {}",
                    entity.getClass().getSimpleName(), requiredRole, roleAssignment);
            return false;
        }

        for (String entityType : roleAssignment.getEntityClassifications().keySet()) {
            boolean authorized = checkEntityClassification(entityType, entity, roleAssignment.getEntityClassifications().get(entityType));

            if (!authorized) {
                logger.info("Not authorized for entity {} and role assignment {}", entity, roleAssignment);
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
            logger.info("Contains {} means true", ENTITY_CLASSIFIER_ALL_ATTRIBUTES);
            return true;

        }

        Optional<Field> optionalField = findFieldFromClassifier(entityType, entity);

        if (!optionalField.isPresent()) {
            return true;
        }

        Field field = optionalField.get();
        field.setAccessible(true);
        Optional<Object> optionalValue = getFieldValue(field, entity);

        if (!optionalValue.isPresent()) {
            logger.info("Cannot resolve value for {}, entity: {}", field, entity);
            return true;
        }

        Object value = optionalValue.get();

        for (String classification : classificationsForEntityType) {
            logger.trace("Matches on field name {}", classification);
            boolean match = classificationMatchesObjectValue(classification, value);
            if (!match) {
                return false;
            }
        }

        return true;
    }

    private Optional<Field> findFieldFromClassifier(String entityType, Object entity) {
        return Stream.of(entity.getClass().getDeclaredFields())
                .filter(field -> entityType.equalsIgnoreCase(field.getName()))
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

    // Todo override by relevant module
    public boolean entityAllowedInAdministrativeZone(RoleAssignment roleAssignment, Object entity) {
        return true;
    }

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
