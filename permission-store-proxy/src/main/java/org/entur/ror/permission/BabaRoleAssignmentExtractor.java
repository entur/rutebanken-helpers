package org.entur.ror.permission;

import java.util.List;
import org.entur.oauth2.RoROAuth2Claims;
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

  private static final String OAUTH2_CLAIM_PREFERRED_USERNAME =
    "https://ror.entur.io/preferred_username";

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

    String preferredUserName = (String) jwtAuthenticationToken
      .getTokenAttributes()
      .get(OAUTH2_CLAIM_PREFERRED_USERNAME);

    // if the preferred userName is set, this is a user account
    if (preferredUserName != null) {
      return userRoleAssignments(preferredUserName);
    }

    // otherwise this is a machine-to-machine token
    return parsePermissionsClaim(
      jwtAuthenticationToken
        .getTokenAttributes()
        .get(RoROAuth2Claims.OAUTH2_CLAIM_PERMISSIONS)
    );
  }

  protected abstract List<RoleAssignment> userRoleAssignments(
    String preferredUserName
  );

  private List<RoleAssignment> parsePermissionsClaim(Object permissionsClaim) {
    if (permissionsClaim instanceof List claimPermissionAsList) {
      List<String> claimPermissionAsStringList = claimPermissionAsList;
      return claimPermissionAsStringList
        .stream()
        .map(role ->
          RoleAssignment
            .builder()
            .withRole(role)
            .withOrganisation(DEFAULT_ADMIN_ORG)
            .build()
        )
        .toList();
    } else {
      throw new IllegalArgumentException(
        "Unsupported claim type: " + permissionsClaim
      );
    }
  }
}
