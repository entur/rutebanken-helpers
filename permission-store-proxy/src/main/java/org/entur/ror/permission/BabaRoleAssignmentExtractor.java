package org.entur.ror.permission;

import java.util.List;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Base class for role assignment extractors that retrieve Role assignments from the user repository Baba.
 * Concrete implementations retrieve the role assignments either directly from the database, when running within the
 * Baba service, or through the REST API exposed by Baba.
 * The user is matched by the preferred named claim in the JWT token.
 * In case of a machine-to-machine token, the preferred name is not present in the token and roles are extracted
 * from the Auth0 claim "permissions".
 */
public abstract class BabaRoleAssignmentExtractor
  implements RoleAssignmentExtractor {

  private static final String DEFAULT_ADMIN_ORG = "RB";

  @Override
  public final List<RoleAssignment> getRoleAssignmentsForUser(
    Authentication authentication
  ) {
    if (
      !(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
    ) {
      throw new AccessDeniedException("Not authenticated with token");
    }

    AuthenticatedUser authenticatedUser = AuthenticatedUser.of(
      jwtAuthenticationToken
    );

    if (authenticatedUser.isInternal()) {
      return parsePermissionsClaim(authenticatedUser.permissions());
    }

    return userRoleAssignments(authenticatedUser);
  }

  protected abstract List<RoleAssignment> userRoleAssignments(
    AuthenticatedUser subject
  );

  /**
   * Extract RoleAssignments from the permission claim.
   * Internal tokens (from Entur Internal) contain cross-organization roles under this claim.
   */
  private List<RoleAssignment> parsePermissionsClaim(
    List<String> permissionsClaim
  ) {
    return permissionsClaim
      .stream()
      .map(role ->
        RoleAssignment
          .builder()
          .withRole(role)
          .withOrganisation(DEFAULT_ADMIN_ORG)
          .build()
      )
      .toList();
  }
}
