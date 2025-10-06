package org.entur.oauth2;

import java.util.List;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.security.core.Authentication;

/**
 * @deprecated use BabaRoleAssignmentExtractor
 */
@Deprecated
public class JwtRoleAssignmentExtractor implements RoleAssignmentExtractor {

  public JwtRoleAssignmentExtractor() {}

  public JwtRoleAssignmentExtractor(String adminOrganisation) {}

  @Override
  public List<RoleAssignment> getRoleAssignmentsForUser(Authentication auth) {
    throw new UnsupportedOperationException(
      "JwtRoleAssignmentExtractor is not supported anymore"
    );
  }
}
