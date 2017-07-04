package org.rutebanken.helper.organisation;

public interface EntityResolver {
    /**
     * If entity itself cannot be checked for authorization, but the owning entity can.
     * For instance, if a Quay belongs to StopPlace, the Quay cannot be checked, but the StopPlace can.
     *
     * @param entity child entity
     * @return the parent entity to check for authorization
     */
    Object resolveCorrectEntity(Object entity);
}
