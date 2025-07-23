package org.rutebanken.helper.organisation.authorization;

import static org.rutebanken.helper.organisation.AuthorizationConstants.*;

import java.util.Arrays;
import java.util.function.Function;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;

/**
 * Default implementation of the {@link AuthorizationService}.
 * <ul>
 *     <li>A {@link RoleAssignmentExtractor} is used to extract the roles from the token.</li>
 *     <li>A mapping function is used to convert the internal provider id used by the client application to
 *     the organisation codespace of this provider.</li>
 * </ul>
 */
public class DefaultAuthorizationService<T> implements AuthorizationService<T> {

  private static final String ENTUR_ORG = "RB";

  private final Function<T, String> getProviderOrganisationById;
  private final RoleAssignmentExtractor roleAssignmentExtractor;

  /**
   * Create a user context service with the given role assignment extractor and user info extractor.
   */
  public DefaultAuthorizationService(
    RoleAssignmentExtractor roleAssignmentExtractor
  ) {
    this(t -> null, roleAssignmentExtractor);
  }

  /**
   * Create a user context service with the given role assignment extractor, user info extractor and a codespace mapping function.
   */
  public DefaultAuthorizationService(
    Function<T, String> getProviderOrganisationById,
    RoleAssignmentExtractor roleAssignmentExtractor
  ) {
    this.getProviderOrganisationById = getProviderOrganisationById;
    this.roleAssignmentExtractor = roleAssignmentExtractor;
  }

  @Override
  public boolean isRouteDataAdmin() {
    return isAdminFor(ROLE_ROUTE_DATA_ADMIN);
  }

  @Override
  public boolean isOrganisationAdmin() {
    // ROLE_ORGANISATION_EDIT provides admin privilege on all organisations
    return isAdminFor(ROLE_ORGANISATION_EDIT);
  }

  @Override
  public boolean canViewAllOrganisationData() {
    return isAdminForAny(
      ROLE_ORGANISATION_EDIT,
      ROLE_ORGANISATION_DATA_VIEW_ALL
    );
  }

  @Override
  public boolean canViewRouteData(T providerId) {
    String providerOrganisation = getProviderOrganisationById.apply(providerId);
    if (providerOrganisation == null) {
      return false;
    }
    return roleAssignmentExtractor
      .getRoleAssignmentsForUser()
      .stream()
      .anyMatch(roleAssignment ->
        matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_ADMIN) ||
        matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_VIEW_ALL) ||
        matchProviderRole(
          roleAssignment,
          ROLE_ROUTE_DATA_EDIT,
          providerOrganisation
        )
      );
  }

  @Override
  public boolean canEditRouteData(T providerId) {
    String providerOrganisation = getProviderOrganisationById.apply(providerId);
    if (providerOrganisation == null) {
      return false;
    }
    return roleAssignmentExtractor
      .getRoleAssignmentsForUser()
      .stream()
      .anyMatch(roleAssignment ->
        matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_ADMIN) ||
        matchProviderRole(
          roleAssignment,
          ROLE_ROUTE_DATA_EDIT,
          providerOrganisation
        )
      );
  }

  @Override
  public boolean canViewBlockData(T providerId) {
    String providerOrganisation = getProviderOrganisationById.apply(providerId);
    if (providerOrganisation == null) {
      return false;
    }
    return roleAssignmentExtractor
      .getRoleAssignmentsForUser()
      .stream()
      .anyMatch(roleAssignment ->
        matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_ADMIN) ||
        matchProviderRole(
          roleAssignment,
          ROLE_NETEX_BLOCKS_DATA_VIEW,
          providerOrganisation
        ) ||
        matchProviderRole(
          roleAssignment,
          ROLE_NETEX_PRIVATE_DATA_VIEW,
          providerOrganisation
        ) ||
        matchProviderRole(
          roleAssignment,
          ROLE_ROUTE_DATA_EDIT,
          providerOrganisation
        )
      );
  }

  @Override
  public boolean canViewRoleAssignments() {
    return roleAssignmentExtractor
      .getRoleAssignmentsForUser()
      .stream()
      .anyMatch(roleAssignment ->
        matchAdminRole(roleAssignment, ROLE_ROLE_ASSIGNMENTS_VIEW)
      );
  }

  private boolean isAdminFor(String role) {
    return roleAssignmentExtractor
      .getRoleAssignmentsForUser()
      .stream()
      .anyMatch(roleAssignment -> matchAdminRole(roleAssignment, role));
  }

  private boolean isAdminForAny(String... roles) {
    return roleAssignmentExtractor
      .getRoleAssignmentsForUser()
      .stream()
      .anyMatch(roleAssignment ->
        Arrays
          .stream(roles)
          .anyMatch(role -> matchAdminRole(roleAssignment, role))
      );
  }

  /**
   * Return true if the role assignment gives access to the given role for the Entur organisation.
   */
  private static boolean matchAdminRole(
    RoleAssignment roleAssignment,
    String role
  ) {
    return matchProviderRole(roleAssignment, role, ENTUR_ORG);
  }

  /**
   * Return true if the role assignment gives access to the given role for the given provider.
   */
  private static boolean matchProviderRole(
    RoleAssignment roleAssignment,
    String role,
    String providerOrganisation
  ) {
    return (
      role.equals(roleAssignment.getRole()) &&
      providerOrganisation.equals(roleAssignment.getOrganisation())
    );
  }
}
