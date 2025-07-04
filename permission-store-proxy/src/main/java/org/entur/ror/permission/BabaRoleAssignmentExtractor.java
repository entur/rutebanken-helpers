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
 * The user is matched by the subject claim in the JWT token.
 */
public abstract class BabaRoleAssignmentExtractor
  implements RoleAssignmentExtractor {

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

    return userRoleAssignments(authenticatedUser);
  }

  protected abstract List<RoleAssignment> userRoleAssignments(
    AuthenticatedUser subject
  );
}
