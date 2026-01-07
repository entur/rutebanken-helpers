package org.entur.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Extract {@link RoleAssignment}s from {@link JwtAuthenticationToken}.
 * <ul>
 *     <li>For user tokens, role assignments are expected to be defined in the claim
 *   "role_assignments", in JSON format.</li>
 *     <li>For internal machine-to-machine tokens, the role assignments are derived from the claim
 *   "permissions" that Auth0 builds from the internal client configuration:
 *   each permission is converted into a role assignment tied to the admin organisation ("RB").
 *   Permission granted to an internal client are always cross-organisation (not limited to a given provider)</li>
 * </ul>
 * @deprecated This class relies on Entur-specific OAuth2 claims that are not in use anymore.
 * To be removed after permission store migration.
 */
@Deprecated
public class JwtRoleAssignmentExtractor implements RoleAssignmentExtractor {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String DEFAULT_ADMIN_ORG = "RB";

  private static final String OAUTH2_CLAIM_ROLE_ASSIGNMENTS =
    "role_assignments";
  private static final String OAUTH2_CLAIM_PERMISSIONS = "permissions";

  private final String adminOrganisation;

  public JwtRoleAssignmentExtractor() {
    this(DEFAULT_ADMIN_ORG);
  }

  public JwtRoleAssignmentExtractor(String adminOrganisation) {
    this.adminOrganisation = adminOrganisation;
  }

  @Override
  public List<RoleAssignment> getRoleAssignmentsForUser(Authentication auth) {
    if (!(auth instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
      throw new AccessDeniedException("Not authenticated with token");
    }

    Jwt jwt = (Jwt) jwtAuthenticationToken.getPrincipal();

    Object roleAssignmentsClaim = jwt.getClaim(OAUTH2_CLAIM_ROLE_ASSIGNMENTS);
    if (roleAssignmentsClaim != null) {
      return parseRoleAssignmentsClaim(roleAssignmentsClaim);
    }

    Object rolesClaim = jwt.getClaim(OAUTH2_CLAIM_PERMISSIONS);
    if (rolesClaim != null) {
      return parsePermissionsClaim(rolesClaim);
    }

    return List.of();
  }

  /**
   * Extract RoleAssignments from the role_assignments claim.
   * User tokens contain json-encoded RoleAssignments under this claim.
   */
  private static List<RoleAssignment> parseRoleAssignmentsClaim(
    Object roleAssignmentClaim
  ) {
    if (roleAssignmentClaim instanceof List roleAssignmentAsList) {
      List<String> roleAssignmentAsStringList = roleAssignmentAsList;
      return roleAssignmentAsStringList
        .stream()
        .map(JwtRoleAssignmentExtractor::parse)
        .toList();
    } else {
      throw new IllegalArgumentException(
        "Unsupported claim type: " + roleAssignmentClaim
      );
    }
  }

  /**
   * Extract RoleAssignments from the permissions claim.
   * Internal machine-to-machine tokens contain cross-organisation roles under this claim.
   */
  private List<RoleAssignment> parsePermissionsClaim(Object permissionsClaim) {
    if (permissionsClaim instanceof List claimPermissionAsList) {
      List<String> claimPermissionAsStringList = claimPermissionAsList;
      return claimPermissionAsStringList
        .stream()
        .map(role ->
          RoleAssignment
            .builder()
            .withRole(role)
            .withOrganisation(adminOrganisation)
            .build()
        )
        .toList();
    } else {
      throw new IllegalArgumentException(
        "Unsupported claim type: " + permissionsClaim
      );
    }
  }

  /**
   * Parse a JSON-encoded role assignment.
   */
  private static RoleAssignment parse(Object roleAssignment) {
    if (roleAssignment instanceof Map) {
      return MAPPER.convertValue(roleAssignment, RoleAssignment.class);
    }
    try {
      return MAPPER.readValue((String) roleAssignment, RoleAssignment.class);
    } catch (IOException e) {
      throw new IllegalArgumentException(
        "Exception while parsing role assignments from JSON",
        e
      );
    }
  }
}
