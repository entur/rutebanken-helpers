package org.entur.oauth2.authorization;

import org.entur.oauth2.JwtRoleAssignmentExtractor;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.function.Function;

import static org.rutebanken.helper.organisation.AuthorizationConstants.*;

/**
 * Implementation of the UserContextService that retrieves user privileges from a OAuth2 user token
 */
public class OAuth2TokenUserContextService<T> implements UserContextService<T> {

    private static final String ENTUR_ORG = "RB";

    private final Function<T, String> getProviderOrganisationById;
    private final RoleAssignmentExtractor roleAssignmentExtractor;

    public OAuth2TokenUserContextService(Function<T, String> getProviderOrganisationById) {
        this(getProviderOrganisationById, new JwtRoleAssignmentExtractor());
    }

    public OAuth2TokenUserContextService(Function<T, String> getProviderOrganisationById,
                                         RoleAssignmentExtractor roleAssignmentExtractor) {
        this.getProviderOrganisationById = getProviderOrganisationById;
        this.roleAssignmentExtractor = roleAssignmentExtractor;
    }

    @Override
    public boolean isRouteDataAdmin() {
        return isAdminFor(ROLE_ROUTE_DATA_ADMIN);
    }

    @Override
    public boolean isOrganisationAdmin() {
        // ROLE_ORGANISATION_EDIT provides admin privilege on all organisations
        return isAdminFor(ROLE_ORGANISATION_EDIT);
    }

    @Override
    public boolean canViewProvider(T providerId) {
        String providerOrganisation = getProviderOrganisationById.apply(providerId);
        if (providerOrganisation == null) {
            return false;
        }
        return roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .anyMatch(roleAssignment -> matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_ADMIN)
                        || matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_VIEW_ALL)
                        || matchProviderRole(roleAssignment, ROLE_ROUTE_DATA_EDIT, providerOrganisation)
                );
    }

    @Override
    public boolean canEditProvider(T providerId) {
        String providerOrganisation = getProviderOrganisationById.apply(providerId);
        if (providerOrganisation == null) {
            return false;
        }
        return roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .anyMatch(roleAssignment -> matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_ADMIN)
                        || matchProviderRole(roleAssignment, ROLE_ROUTE_DATA_EDIT, providerOrganisation)
                );
    }

    @Override
    public boolean canViewBlocks(T providerId) {
        String providerOrganisation = getProviderOrganisationById.apply(providerId);
        if (providerOrganisation == null) {
            return false;
        }
        return roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .anyMatch(roleAssignment -> matchAdminRole(roleAssignment, ROLE_ROUTE_DATA_ADMIN)
                        || matchProviderRole(roleAssignment, ROLE_NETEX_BLOCKS_DATA_VIEW, providerOrganisation)
                        || matchProviderRole(roleAssignment, ROLE_ROUTE_DATA_EDIT, providerOrganisation)
                );
    }

    @Override
    public String getPreferredName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) auth;
        Jwt jwt = (Jwt) jwtAuthenticationToken.getPrincipal();
        return jwt.getClaimAsString("https://ror.entur.io/preferred_name");
    }


    private boolean isAdminFor(String role) {
        return roleAssignmentExtractor.getRoleAssignmentsForUser()
                .stream()
                .anyMatch(roleAssignment -> matchAdminRole(roleAssignment, role));
    }

    /**
     * Return true if the role assignment gives access to the given role for the Entur organisation
     */
    private static boolean matchAdminRole(RoleAssignment roleAssignment, String role) {
        return matchProviderRole(roleAssignment, role, ENTUR_ORG);
    }

    /**
     * Return true if the role assignment gives access to the given role for the given provider.
     */
    private static boolean matchProviderRole(RoleAssignment roleAssignment, String role, String providerOrganisation) {
        return (
                role.equals(roleAssignment.getRole()) &&
                        providerOrganisation.equals(roleAssignment.getOrganisation())
        );
    }


}
