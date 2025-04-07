package org.rutebanken.helper.organisation.authorization;

import org.springframework.security.access.AccessDeniedException;

/**
 * Service returning the privileges of the current user.
 * @param <T> the type of the provider unique id
 */
public interface AuthorizationService<T> {
  /**
   * Is the current user a route data administrator?
   */
  boolean isRouteDataAdmin();

  /**
   * Validate that the current user is a route data administrator.
   * @throws org.springframework.security.access.AccessDeniedException if the role is missing.
   */
  default void validateRouteDataAdmin() {
    if (!isRouteDataAdmin()) {
      throw new AccessDeniedException("Insufficient privileges for operation");
    }
  }

  /**
   * Is the current user an organisation administrator?
   */
  boolean isOrganisationAdmin();

  /**
   * Validate that the current user is an organisation administrator.
   * @throws org.springframework.security.access.AccessDeniedException if the role is missing.
   */
  default void validateOrganisationAdmin() {
    if (!isOrganisationAdmin()) {
      throw new AccessDeniedException("Insufficient privileges for operation");
    }
  }

  /**
   * Whether the current user can view route data belonging to a given provider.
   * @param providerId The internal code of the provider.
   * @return true if the user has access.
   */
  boolean canViewRouteData(T providerId);

  /**
   * Whether the current user can edit route data belonging to a given provider.
   * @param providerId The internal code of the provider.
   * @return true if the user has access.
   */
  boolean canEditRouteData(T providerId);

  /**
   * Validate that the current user can edit route data belonging to a given provider.
   * @param providerId The internal code of the provider.
   * @throws org.springframework.security.access.AccessDeniedException if the role is missing.
   */
  default void validateEditRouteData(T providerId) {
    if (!canEditRouteData(providerId)) {
      throw new AccessDeniedException("Insufficient privileges for operation");
    }
  }

  /**
   * Whether the current user can view block data belonging to a given provider.
   * @param providerId The internal code of the provider.
   * @return true if the user has access.
   */
  boolean canViewBlockData(T providerId);

  /**
   * Whether the current user can view role assignments for all users.
   */
  boolean canViewRoleAssignments();

  /**
   * Validate that the current user can view block data belonging to a given provider.
   * @param providerId The internal code of the provider.
   * @throws org.springframework.security.access.AccessDeniedException if the role is missing.
   */
  default void validateViewBlockData(T providerId) {
    if (!canViewBlockData(providerId)) {
      throw new AccessDeniedException("Insufficient privileges for operation");
    }
  }
}
