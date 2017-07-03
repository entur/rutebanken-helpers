package org.rutebanken.helper.organisation;

import org.junit.Test;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.rutebanken.helper.organisation.AuthorizationConstants.ENTITY_TYPE;

/**
 * Example content for token's role assignment.
 * [
 * {
 * “r”: “editStops”,
 * “o”: “OST”,
 * “z”: “01",
 * “e”: {
 * “EntityType”: [
 * “StopPlace”, "Parking" {
 * ],
 * “StopPlaceType”: [
 * “!airport”,
 * “!railStation”
 * ],
 * versionComment
 * “Submode”: [
 * “!railReplacementBus”
 * ]
 * }
 * }
 * ]
 */
public class ReflectionAuthorizationServiceTest {


    private RoleAssignmentExtractor roleAssignmentExtractor = new RoleAssignmentExtractor() {
        @Override
        public List<RoleAssignment> getRoleAssignmentsForUser() {
            return null;
        }

        @Override
        public List<RoleAssignment> getRoleAssignmentsForUser(Authentication authentication) {
            return null;
        }
    };

    private ReflectionAuthorizationService reflectionAuthorizationService = new ReflectionAuthorizationService(roleAssignmentExtractor, true) {
        @Override
        public boolean entityMatchesAdministrativeZone(RoleAssignment roleAssignment, Object entity) {
            return true;
        }

        @Override
        public boolean entityMatchesOrganisationRef(RoleAssignment roleAssignment, Object entity) {
            return true;
        }

        @Override
        public Object resolveCorrectEntity(Object entity) {
            return entity;
        }
    };

    @Test
    public void authorizedForLegalStopPlaceTypesWhenOthersBlacklisted() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("StopPlaceType", "!airport")
                .withEntityClassification("StopPlaceType", "!railStation")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.stopPlaceType = StopPlace.StopPlaceType.ONSTREET_BUS;

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
        assertThat(authorized, is(true));
    }



    @Test
    public void authorizedForLegalSubmodeTypesWhenStarValue() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withOrganisation("OST")
                .withAdministrativeZone("01")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("Submode", "*")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.submode = "someValue";

        assertThat(reflectionAuthorizationService.authorized(roleAssignment, stopPlace, "editStops"), is(true));
    }

    @Test
    public void shouldBeAuthorizedForSubmodeTypesWhenExplicitWhiteListed() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withOrganisation("OST")
                .withAdministrativeZone("01")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("Submode", "someValue")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.submode = "someValue";

        assertThat(reflectionAuthorizationService.authorized(roleAssignment, stopPlace, "editStops"), is(true));
    }

    @Test
    public void authorizedWhenAllTypesEntityType() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("viewStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "*")
                .build();

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, new Object(), roleAssignment.r);
        assertThat(authorized, is(true));
    }

    @Test
    public void notAuthorizedWhenRolesMismatch() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("viewStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "*")
                .build();

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, new Object(), "somethingElse");
        assertThat(authorized, is(false));
    }

    @Test
    public void notAuthorizedWhenEmptyRoleAssignmentEntityClassifications() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("viewStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .build();

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, new Object(), roleAssignment.r);
        assertThat(authorized, is(false));
    }

    @Test
    public void notAuthorizedForIncorrectSubMode() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("Submode", "!railReplacementBus")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.stopPlaceType = StopPlace.StopPlaceType.AIRPORT;

        // Submode is not allowed
        stopPlace.submode = "railReplacementBus";

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
        assertThat(authorized, is(false));
    }

    @Test
    public void notAuthorizedForIncorrectEntityType() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("viewStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .build();

        // Not stop place
        Object object = new Object();

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, object, roleAssignment.r);
        assertThat(authorized, is(false));
    }

    @Test
    public void notAuthorizedToEditAirport() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("StopPlaceType", "!airport")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.stopPlaceType = StopPlace.StopPlaceType.AIRPORT;

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
        assertThat(authorized, is(false));
    }

    /**
     * Onstreet bus does does contain underscore.
     */
    @Test
    public void notAuthorizedToEditOnstreetBus() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("StopPlaceType", "!onstreetBus")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.stopPlaceType = StopPlace.StopPlaceType.ONSTREET_BUS;

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
        assertThat(authorized, is(false));
    }

    @Test
    public void notAuthorizedToEditWhenOneBlacklisted() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("StopPlaceType", "!airport")
                .withEntityClassification("StopPlaceType", "!onstreetBus")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.stopPlaceType = StopPlace.StopPlaceType.ONSTREET_BUS;

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
        assertThat(authorized, is(false));
    }

    @Test
    public void notAuthorizedToEditWhenEnumValueContainsUnderscore() {
        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("StopPlaceType", "!onstreet_Bus")
                .build();

        StopPlace stopPlace = new StopPlace();
        stopPlace.stopPlaceType = StopPlace.StopPlaceType.ONSTREET_BUS;

        boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
        assertThat(authorized, is(false));
    }

    @Test
    public void multipleEnumsShouldBeAllowed() {

        List<StopPlace.StopPlaceType> types = Arrays.asList(
                StopPlace.StopPlaceType.AIRPORT,
                StopPlace.StopPlaceType.ONSTREET_TRAM,
                StopPlace.StopPlaceType.ONSTREET_BUS
        );


        RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .build();

        roleAssignment.getEntityClassifications().put("StopPlaceType", types.stream().map(Enum::toString).collect(toList()));


        StopPlace stopPlace = new StopPlace();

        types.forEach(type -> {
            stopPlace.stopPlaceType = type;
            boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
            assertThat("Should have access to edit stop with type "+ type, authorized, is(true));
        });
    }

    @Test
    public void mixNegationForEnums() {
            RoleAssignment roleAssignment = RoleAssignment.builder()
                .withRole("editStops")
                .withAdministrativeZone("01")
                .withOrganisation("OST")
                .withEntityClassification(ENTITY_TYPE, "StopPlace")
                .withEntityClassification("StopPlaceType", StopPlace.StopPlaceType.AIRPORT.toString())
                .withEntityClassification("StopPlaceType", "!"+StopPlace.StopPlaceType.ONSTREET_BUS.toString())
                .withEntityClassification("StopPlaceType", StopPlace.StopPlaceType.ONSTREET_TRAM.toString())
                .build();


        StopPlace stopPlace = new StopPlace();

        stopPlace.stopPlaceType = StopPlace.StopPlaceType.AIRPORT;
        assertThat(reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r), is(true));


        stopPlace.stopPlaceType = StopPlace.StopPlaceType.ONSTREET_BUS;
        assertThat("no access for bus", reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r), is(false));


        stopPlace.stopPlaceType = StopPlace.StopPlaceType.ONSTREET_TRAM;
        assertThat(reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r), is(true));
    }



    @Test
    public void authorizedForCertainEnumValues() {

        for(StopPlace.StopPlaceType enumValue : StopPlace.StopPlaceType.values()){
            RoleAssignment roleAssignment = RoleAssignment.builder()
                    .withRole("editEnums")
                    .withAdministrativeZone("01")
                    .withOrganisation("OST")
                    .withEntityClassification(ENTITY_TYPE, "StopPlace")
                    .withEntityClassification("StopPlaceType", "!"+enumValue)
                    .build();

            StopPlace stopPlace = new StopPlace();
            stopPlace.stopPlaceType = enumValue;

            boolean authorized = reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r);
            assertThat("should not be authorized as negation is on !", authorized, is(false));

            Arrays.stream(StopPlace.StopPlaceType.values()).forEach(otherEnum -> {
                if(otherEnum != enumValue) {
                    stopPlace.stopPlaceType = otherEnum;
                    assertThat("One value is not allowed " + enumValue + ", but " + otherEnum + " is",
                            reflectionAuthorizationService.authorized(roleAssignment, stopPlace, roleAssignment.r), is(true));
                }
            });
        }
    }

    private static class StopPlace {
        enum StopPlaceType {
            ONSTREET_BUS("onstreetBus"),
            ONSTREET_TRAM("onstreetTram"),
            AIRPORT("airport");
            private final String value;

            StopPlaceType(String v) {
                value = v;
            }
        }

        StopPlaceType stopPlaceType;
        String submode;
    }

}