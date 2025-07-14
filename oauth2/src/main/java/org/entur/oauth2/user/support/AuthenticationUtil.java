package org.entur.oauth2.user.support;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AuthenticationUtil {

  private AuthenticationUtil() {}

  /**
   * Extract the given JWT claim from the current security context holder.
   * Return null if the user is not authenticated with a JWT token or if the token does not contain the given claim.
   */
  public static String getClaim(String claim) {
    Authentication auth = SecurityContextHolder
      .getContext()
      .getAuthentication();

    if (auth instanceof JwtAuthenticationToken jwtAuthenticationToken) {
      Jwt jwt = (Jwt) jwtAuthenticationToken.getPrincipal();
      return jwt.getClaimAsString(claim);
    } else {
      return null;
    }
  }
}
