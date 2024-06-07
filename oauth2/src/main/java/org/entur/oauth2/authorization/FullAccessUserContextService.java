package org.entur.oauth2.authorization;

/**
 * Fallback implementation giving full access to all operations for authenticated users.
 * Used for testing purpose.
 */
public class FullAccessUserContextService implements UserContextService<Long> {
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

    @Override
    public String getPreferredName() {
        return "";
    }
}
