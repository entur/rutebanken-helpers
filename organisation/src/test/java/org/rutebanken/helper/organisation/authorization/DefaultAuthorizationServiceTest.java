package org.rutebanken.helper.organisation.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rutebanken.helper.organisation.AuthorizationConstants;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.security.core.Authentication;

class DefaultAuthorizationServiceTest {

  private static final long PROVIDER_ID_RUT = 2L;
  private static final String CODESPACE_RB = "RB";
  private static final String CODESPACE_RUT = "RUT";
  private static final String CODESPACE_XXX = "XXX";

  static Stream<Arguments> testCasesVerifyRouteDataAdministratorPrivileges() {
    return Stream.of(
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN,
        CODESPACE_RB,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ORGANISATION_EDIT,
        CODESPACE_RB,
        false
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_RUT,
        false
      )
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesVerifyRouteDataAdministratorPrivileges")
  void testVerifyRouteDataAdministratorPrivileges(
    String role,
    String organisation,
    boolean isAuthorized
  ) {
    List<RoleAssignment> roleAssignments = roleAssignments(role, organisation);
    AuthorizationService<Long> authorizationService = authorizationService(
      roleAssignments
    );
    assertEquals(isAuthorized, authorizationService.isRouteDataAdmin());
  }

  static Stream<Arguments> testCasesVerifyOrganisationAdministratorPrivileges() {
    return Stream.of(
      Arguments.of(
        AuthorizationConstants.ROLE_ORGANISATION_EDIT,
        CODESPACE_RB,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN,
        CODESPACE_RB,
        false
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_RUT,
        false
      )
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesVerifyOrganisationAdministratorPrivileges")
  void testVerifyOrganisationAdministratorPrivileges(
    String role,
    String organisation,
    boolean isAuthorized
  ) {
    List<RoleAssignment> roleAssignments = roleAssignments(role, organisation);
    AuthorizationService<Long> authorizationService = authorizationService(
      roleAssignments
    );
    assertEquals(isAuthorized, authorizationService.isOrganisationAdmin());
  }

  static Stream<Arguments> testCasesVerifyRouteDataEditorPrivileges() {
    return Stream.of(
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN,
        CODESPACE_RB,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_RUT,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_XXX,
        false
      )
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesVerifyRouteDataEditorPrivileges")
  void testVerifyRouteDataEditorPrivileges(
    String role,
    String organisation,
    boolean isAuthorized
  ) {
    List<RoleAssignment> roleAssignments = roleAssignments(role, organisation);
    AuthorizationService<Long> authorizationService = authorizationService(
      roleAssignments
    );
    assertEquals(
      isAuthorized,
      authorizationService.canEditRouteData(PROVIDER_ID_RUT)
    );
  }

  static Stream<Arguments> testCasesVerifyRouteDataViewerPrivileges() {
    return Stream.of(
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN,
        CODESPACE_RB,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_VIEW_ALL,
        CODESPACE_RB,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_RUT,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_XXX,
        false
      )
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesVerifyRouteDataViewerPrivileges")
  void testVerifyRouteDataViewerPrivileges(
    String role,
    String organisation,
    boolean isAuthorized
  ) {
    List<RoleAssignment> roleAssignments = roleAssignments(role, organisation);
    AuthorizationService<Long> authorizationService = authorizationService(
      roleAssignments
    );
    assertEquals(
      isAuthorized,
      authorizationService.canViewRouteData(PROVIDER_ID_RUT)
    );
  }

  static Stream<Arguments> testCasesVerifyBlockViewerPrivileges() {
    return Stream.of(
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_ADMIN,
        CODESPACE_RB,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_RUT,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_NETEX_BLOCKS_DATA_VIEW,
        CODESPACE_RUT,
        true
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_ROUTE_DATA_EDIT,
        CODESPACE_XXX,
        false
      ),
      Arguments.of(
        AuthorizationConstants.ROLE_NETEX_BLOCKS_DATA_VIEW,
        CODESPACE_XXX,
        false
      )
    );
  }

  @ParameterizedTest
  @MethodSource("testCasesVerifyBlockViewerPrivileges")
  void testVerifyBlockViewerPrivileges(
    String role,
    String organisation,
    boolean isAuthorized
  ) {
    List<RoleAssignment> roleAssignments = roleAssignments(role, organisation);
    AuthorizationService<Long> authorizationService = authorizationService(
      roleAssignments
    );
    assertEquals(
      isAuthorized,
      authorizationService.canViewBlockData(PROVIDER_ID_RUT)
    );
  }

  private static DefaultAuthorizationService<Long> authorizationService(
    List<RoleAssignment> roleAssignments
  ) {
    return new DefaultAuthorizationService<>(
      DefaultAuthorizationServiceTest::getProviderCodespaceByProviderId,
      rolAssignmentExtractor(roleAssignments)
    );
  }

  private static List<RoleAssignment> roleAssignments(
    String role,
    String organisation
  ) {
    return List.of(
      RoleAssignment
        .builder()
        .withOrganisation(organisation)
        .withRole(role)
        .build()
    );
  }

  private static RoleAssignmentExtractor rolAssignmentExtractor(
    List<RoleAssignment> roleAssignments
  ) {
    return new RoleAssignmentExtractor() {
      @Override
      public List<RoleAssignment> getRoleAssignmentsForUser() {
        return roleAssignments;
      }

      @Override
      public List<RoleAssignment> getRoleAssignmentsForUser(
        Authentication authentication
      ) {
        return getRoleAssignmentsForUser();
      }
    };
  }

  private static String getProviderCodespaceByProviderId(Long providerId) {
    if (providerId == PROVIDER_ID_RUT) {
      return CODESPACE_RUT;
    }
    return null;
  }
}
