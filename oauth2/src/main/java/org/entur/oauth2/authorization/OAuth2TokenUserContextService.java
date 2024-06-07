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
 * Implementation of the UserContextService that retrieves user privileges from an OAuth2 token.
 * <ul>
 *     <li>A {@link RoleAssignmentExtractor} is used to extract the roles from the token.</li>
 *     <li>A mapping function is used to convert the internal provider id used by the client application to
 *     the organisation codespace of this provider.</li>
 * </ul>
 *
 */
public class OAuth2TokenUserContextService<T> implements UserContextService<T> {

    private static final String ENTUR_ORG = "RB";

    private final Function<T, String> getProviderOrganisationById;
    private final RoleAssignmentExtractor roleAssignmentExtractor;

    /**
     * Create a user context service with a default role assignment extractor.
     */
    public OAuth2TokenUserContextService() {
        this(t -> null, new JwtRoleAssignmentExtractor());
    }

    /**
     * Create a user context service with a default role assignment extractor and a codespace mapping function.
     */
    public OAuth2TokenUserContextService(Function<T, String> getProviderOrganisationById) {
        this(getProviderOrganisationById, new JwtRoleAssignmentExtractor());
    }

    /**
     * Create a user context service with the given role assignment extractor and a codespace mapping function.
     */
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
    public boolean canViewRouteData(T providerId) {
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
    public boolean canEditRouteData(T providerId) {
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
    public boolean canViewBlockData(T providerId) {
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
     * Return true if the role assignment gives access to the given role for the Entur organisation.
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
