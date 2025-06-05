package org.entur.oauth2.user;

import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Retrieve user information from the JWT token.
 */
public class JwtUserInfoExtractor implements UserInfoExtractor {

  private static final String CLAIM_ROR_PREFERRED_NAME =
    "https://ror.entur.io/preferred_name";
  private static final String CLAIM_ROR_PREFERRED_USERNAME =
    "https://ror.entur.io/preferred_username";

  @Override
  public String getPreferredName() {
    return getClaim(CLAIM_ROR_PREFERRED_NAME);
  }

  @Override
  public String getPreferredUsername() {
    return getClaim(CLAIM_ROR_PREFERRED_USERNAME);
  }

  private String getClaim(String claim) {
    Authentication auth = SecurityContextHolder
      .getContext()
      .getAuthentication();
    JwtAuthenticationToken jwtAuthenticationToken =
      (JwtAuthenticationToken) auth;
    Jwt jwt = (Jwt) jwtAuthenticationToken.getPrincipal();
    return jwt.getClaimAsString(claim);
  }
}
