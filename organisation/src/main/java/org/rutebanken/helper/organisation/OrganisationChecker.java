package org.rutebanken.helper.organisation;

public interface OrganisationChecker {
    boolean entityMatchesOrganisationRef(RoleAssignment roleAssignment, Object entity);
}
