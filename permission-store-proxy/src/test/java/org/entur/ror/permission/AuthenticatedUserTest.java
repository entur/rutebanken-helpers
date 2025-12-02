package org.entur.ror.permission;

import static org.entur.ror.permission.AuthenticatedUser.ENTUR_CLAIM_ORGANISATION_ID;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthenticatedUserTest {

  private static final String PARTNER_SUBJECT = "partnerSubject";
  private static final String PARTNER_ISSUER = "https://partner.entur.org/";
  private static final long PARTNER_ORG = 1L;

  @Test
  void testValidEnturPartnerUser() {
    Map<String, Object> claims = Map.of(
      JwtClaimNames.SUB,
      PARTNER_SUBJECT,
      JwtClaimNames.ISS,
      PARTNER_ISSUER,
      ENTUR_CLAIM_ORGANISATION_ID,
      PARTNER_ORG
    );

    Jwt jwt = Jwt
      .withTokenValue("token")
      .header("alg", "none")
      .claims(existingClaims -> existingClaims.putAll(claims))
      .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

    AuthenticatedUser user = AuthenticatedUser.of(authentication);
    assertEquals(PARTNER_SUBJECT, user.subject());
    assertEquals(PARTNER_ORG, user.organisationId());
    assertTrue(user.isPartner());
  }

  @Test
  void testEnturPartnerUserMissingOrganisation() {
    Map<String, Object> claims = Map.of(
      JwtClaimNames.SUB,
      PARTNER_SUBJECT,
      JwtClaimNames.ISS,
      PARTNER_ISSUER
    );

    Jwt jwt = Jwt
      .withTokenValue("token")
      .header("alg", "none")
      .claims(existingClaims -> existingClaims.putAll(claims))
      .build();

    JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

    assertThrows(
      IllegalArgumentException.class,
      () -> AuthenticatedUser.of(authentication)
    );
  }
}
