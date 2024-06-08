package org.rutebanken.helper.organisation.authorization;

/**
 * Fallback implementation giving full access to all operations for authenticated users.
 * Used for testing purpose.
 */
public class FullAccessAuthorizationService implements AuthorizationService<Long> {
    @Override
    public boolean isRouteDataAdmin() {
        return true;
    }

    @Override
    public boolean isOrganisationAdmin() {
        return true;
    }

    @Override
    public boolean canViewRouteData(Long providerId) {
        return true;
    }

    @Override
    public boolean canEditRouteData(Long providerId) {
        return true;
    }

    @Override
    public boolean canViewBlockData(Long providerId) {
        return true;
    }

}
