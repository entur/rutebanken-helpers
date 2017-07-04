package org.rutebanken.helper.organisation;

public interface AdministrativeZoneChecker {
    boolean entityMatchesAdministrativeZone(RoleAssignment roleAssignment, Object entity);

}
