package org.rutebanken.helper.organisation;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface RoleAssignmentExtractor {

    /**
     * Extract role assignments for user from security context.
     *
     * @return
     */
    List<RoleAssignment> getRoleAssignmentsForUser();

    List<RoleAssignment> getRoleAssignmentsForUser(Authentication authentication);

}
