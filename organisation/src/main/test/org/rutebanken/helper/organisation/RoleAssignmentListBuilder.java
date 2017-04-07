package org.rutebanken.helper.organisation;

import java.util.ArrayList;
import java.util.List;

public class RoleAssignmentListBuilder {

    private List<RoleAssignment> roleAssignments = new ArrayList<>();

    private String DEFAULT_ORG = "DefaultOrg";

    public static RoleAssignmentListBuilder builder() {
        return new RoleAssignmentListBuilder();
    }

    public List<RoleAssignment> build() {
        return roleAssignments;
    }

    public RoleAssignmentListBuilder withAccessAllAreas() {
        return withStopPlaceOfType(AuthorizationConstants.ENTITY_CLASSIFIER_ALL_TYPES)
                       .withRole(AuthorizationConstants.ROLE_ORGANISATION_EDIT, DEFAULT_ORG)
                       .withRole(AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN, DEFAULT_ORG);
    }

    public RoleAssignmentListBuilder withStopPlaceOfType(String type) {
        return withStopPlaceOfType(type, DEFAULT_ORG);
    }

    public RoleAssignmentListBuilder withStopPlaceOfType(String type, String org) {
        RoleAssignment allStopPlaceAccess = RoleAssignment.builder().withRole(AuthorizationConstants.ROLE_EDIT_STOPS)
                                                    .withOrganisation(org)
                                                    .withEntityClassification("StopPlace", type).build();

        roleAssignments.add(allStopPlaceAccess);
        return this;
    }

    public RoleAssignmentListBuilder withRole(String role, String org) {
        RoleAssignment roleAssignment = RoleAssignment.builder().withRole(role)
                                                .withOrganisation(org).build();
        roleAssignments.add(roleAssignment);
        return this;
    }


}
