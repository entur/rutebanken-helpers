package org.entur.oauth2.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class DefaultJwtUserInfoExtractorTest {

  private static final String TEST_SUBJECT = "testSubject";
  private static final String TEST_ISSUER = "testIssuer";
  private static final String TEST_USERNAME = "testUsername";

  @BeforeEach
  public void setUp() {
    SecurityContextHolder.clearContext();
  }

  @Test
  public void noAuthenticatedUser() {
    DefaultJwtUserInfoExtractor extractor = new DefaultJwtUserInfoExtractor();
    assertNull(extractor.getPreferredUsername());
    assertNull(extractor.getPreferredName());
  }

  @Test
  public void anonymousAuthenticatedUser() {
    SecurityContextHolder
      .getContext()
      .setAuthentication(
        new AnonymousAuthenticationToken(
          "key",
          new Object(),
          List.of(new SimpleGrantedAuthority("role"))
        )
      );
    DefaultJwtUserInfoExtractor extractor = new DefaultJwtUserInfoExtractor();
    assertNull(extractor.getPreferredUsername());
    assertNull(extractor.getPreferredName());
  }

  @Test
  public void jwtAuthenticatedUser() {
    Map<String, Object> claims = Map.of(
      JwtClaimNames.SUB,
      TEST_SUBJECT,
      JwtClaimNames.ISS,
      TEST_ISSUER,
      StandardClaimNames.PREFERRED_USERNAME,
      TEST_USERNAME
    );

    Jwt jwt = Jwt
      .withTokenValue("token")
      .header("alg", "none")
      .claims(existingClaims -> existingClaims.putAll(claims))
      .build();

    SecurityContextHolder
      .getContext()
      .setAuthentication(new JwtAuthenticationToken(jwt));
    DefaultJwtUserInfoExtractor extractor = new DefaultJwtUserInfoExtractor();
    assertEquals(TEST_USERNAME, extractor.getPreferredUsername());
    assertEquals(TEST_USERNAME, extractor.getPreferredName());
  }
}
