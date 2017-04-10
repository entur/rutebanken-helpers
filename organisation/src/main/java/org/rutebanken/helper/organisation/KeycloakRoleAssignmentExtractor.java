package org.rutebanken.helper.organisation;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Extract RoleAssignments from KeycloakAuthenticationToken.
 */
public class KeycloakRoleAssignmentExtractor implements RoleAssignmentExtractor {

    private static final String ATTRIBUTE_NAME_ROLE_ASSIGNMENT = "roles";
    private static ObjectMapper mapper = new ObjectMapper();

    public List<RoleAssignment> getRoleAssignmentsForUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getRoleAssignmentsForUser(auth);
    }

    @Override
    public List<RoleAssignment> getRoleAssignmentsForUser(Authentication auth) {
        if (auth instanceof KeycloakAuthenticationToken) {
            KeycloakPrincipal<KeycloakSecurityContext> principal = (KeycloakPrincipal<KeycloakSecurityContext>) auth.getPrincipal();
            AccessToken token = principal.getKeycloakSecurityContext().getToken();
            Object roleAssignments = token.getOtherClaims().get(ATTRIBUTE_NAME_ROLE_ASSIGNMENT);

            List<Object> roleAssignmentList;
            if (roleAssignments == null){
                return new ArrayList<>();
            }
            else if (roleAssignments instanceof List) {
                roleAssignmentList = (List) roleAssignments;
            } else if (roleAssignments instanceof String) {
                roleAssignmentList = Arrays.asList(((String) roleAssignments).split("##"));
            } else {
                throw new IllegalArgumentException("Unsupported 'roles' claim type: " + roleAssignments);
            }

            return roleAssignmentList.stream().map(m -> parse(m)).collect(Collectors.toList());
        } else {
            throw new NotAuthenticatedException("Not authenticated with token");
        }
    }

    private RoleAssignment parse(Object roleAssignment) {
        if (roleAssignment instanceof Map) {
            return mapper.convertValue(roleAssignment, RoleAssignment.class);
        }
        try {
            return mapper.readValue((String) roleAssignment, RoleAssignment.class);
        } catch (IOException ioE) {
            throw new RuntimeException("Exception while parsing role assignments from JSON: " + ioE.getMessage(), ioE);
        }
    }
}
