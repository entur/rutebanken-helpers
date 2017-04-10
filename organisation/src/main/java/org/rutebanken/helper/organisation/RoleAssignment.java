package org.rutebanken.helper.organisation;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of RoleAssignments. A list of these should be included in JWT as attribute "roles" under other claims.
 * <p>
 * Short attr names to keep JWT small.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleAssignment {

    /**
     * Private code for role, required
     */
    public String r;

    /**
     * Private code for organisation, required
     */
    public String o;

    /**
     * Private code for administrative zone, optional
     */
    public String z;


    /**
     * Map of entity types (Stop place, PlaceOfInterest ... ) mapped against classifiers for the type (tramStop etc), each represented by private code. Optional.
     */
    public Map<String, List<String>> e;

    @JsonIgnore
    public String getRole() {
        return r;
    }
    @JsonIgnore
    public String getOrganisation() {
        return o;
    }
    @JsonIgnore
    public String getAdministrativeZone() {
        return z;
    }
    @JsonIgnore
    public Map<String, List<String>> getE() {
        return e;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        protected RoleAssignment roleAssignment = new RoleAssignment();

        private Builder() {
        }

        public Builder withRole(String role) {
            roleAssignment.r = role;
            return this;
        }

        public Builder withOrganisation(String organisation) {
            roleAssignment.o = organisation;
            return this;
        }

        public Builder withAdministrativeZone(String administrativeZone) {
            roleAssignment.z = administrativeZone;
            return this;
        }

        public Builder withEntityClassification(String entityType, String entityClassification) {
            if (roleAssignment.e == null) {
                roleAssignment.e = new HashMap<>();
            }
            List<String> classificationsForType = roleAssignment.e.get(entityType);
            if (classificationsForType == null) {
                classificationsForType = new ArrayList<>();
                roleAssignment.e.put(entityType, classificationsForType);
            }
            classificationsForType.add(entityClassification);
            return this;
        }

        public RoleAssignment build() {
            if (roleAssignment.r == null) {
                throw new IllegalArgumentException("No role (r) set");
            }
            if (roleAssignment.o == null) {
                throw new IllegalArgumentException("No organisation (o) set");
            }
            return roleAssignment;
        }
    }
}
