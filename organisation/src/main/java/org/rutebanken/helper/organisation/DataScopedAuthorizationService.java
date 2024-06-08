package org.rutebanken.helper.organisation;

import org.springframework.security.access.AccessDeniedException;

import java.util.Collection;
import java.util.Set;

/**
 * Authorization service that makes access decisions based on the data requested.
 */
public interface DataScopedAuthorizationService {

    /**
     * Does the current user have the given role on all the given entities?
     */
    boolean isAuthorized(String requiredRole, Collection<?> entities);

    /**
     * Verify that the current user has the given role on all the given entities.
     * @throws AccessDeniedException if not.
     */
    void assertAuthorized(String requiredRole, Collection<?> entities);

    /**
     * Return the subset of the roles that the current user holds that apply to this entity.
     * */
    Set<String> getRelevantRolesForEntity(Object entity);

    /**
     * Does the role assignment give edit right on the given entity?
     * (for unit tests only)
     */
    boolean authorized(RoleAssignment roleAssignment, Object entity, String requiredRole);
}
