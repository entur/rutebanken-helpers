package org.entur.oauth2.multiissuer;

import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.entur.oauth2.RoRJwtDecoderBuilder;
import org.entur.oauth2.RorAuthenticationConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.util.StringUtils;

/**
 * Resolve the @{@link AuthenticationManager} that should authenticate the current JWT token.
 * This is achieved by extracting the issuer from the token and matching it against either the Entur Partner, Entur Internal
 * or the RoR Auth0 issuer URI.
 * The three @{@link AuthenticationManager}s are instantiated during the first request and then cached.
 * If any of the three issuers is not configured, then corresponding tokens will be rejected.
 */
public class MultiIssuerAuthenticationManagerResolver
  implements AuthenticationManagerResolver<HttpServletRequest> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final String enturInternalAuth0Audience;
  private final String enturInternalAuth0Issuer;
  private final String enturPartnerAuth0Audience;
  private final String enturPartnerAuth0Issuer;
  private final String rorAuth0Audience;
  private final String rorAuth0Issuer;
  private final String rorAuth0ClaimNamespace;

  private final BearerTokenResolver resolver = new DefaultBearerTokenResolver();
  private final Map<String, AuthenticationManager> authenticationManagers =
    new ConcurrentHashMap<>();

  protected MultiIssuerAuthenticationManagerResolver(
    String enturInternalAuth0Audience,
    String enturInternalAuth0Issuer,
    String enturPartnerAuth0Audience,
    String enturPartnerAuth0Issuer,
    String rorAuth0Audience,
    String rorAuth0Issuer,
    String rorAuth0ClaimNamespace
  ) {
    this.enturInternalAuth0Audience = enturInternalAuth0Audience;
    this.enturInternalAuth0Issuer = enturInternalAuth0Issuer;
    this.enturPartnerAuth0Audience = enturPartnerAuth0Audience;
    this.enturPartnerAuth0Issuer = enturPartnerAuth0Issuer;
    this.rorAuth0Audience = rorAuth0Audience;
    this.rorAuth0Issuer = rorAuth0Issuer;
    this.rorAuth0ClaimNamespace = rorAuth0ClaimNamespace;
  }

  /**
   * Build a @{@link JwtDecoder} for Entur Internal Auth0 tenant.
   *
   * @return a @{@link JwtDecoder} for Auth0.
   */
  protected JwtDecoder enturInternalAuth0JwtDecoder() {
    return new RoRJwtDecoderBuilder()
      .withIssuer(enturInternalAuth0Issuer)
      .withAudience(enturInternalAuth0Audience)
      .withAuth0ClaimNamespace(rorAuth0ClaimNamespace)
      .build();
  }

  /**
   * Build a @{@link JwtDecoder} for Entur Internal Auth0 tenant.
   *
   * @return a @{@link JwtDecoder} for Auth0.
   */
  protected JwtDecoder enturPartnerAuth0JwtDecoder() {
    return new RoRJwtDecoderBuilder()
      .withIssuer(enturPartnerAuth0Issuer)
      .withAudience(enturPartnerAuth0Audience)
      .withAuth0ClaimNamespace(rorAuth0ClaimNamespace)
      .build();
  }

  /**
   * Build a @{@link JwtDecoder} for Ror Auth0 tenant.
   *
   * @return a @{@link JwtDecoder} for Auth0.
   */
  protected JwtDecoder rorAuth0JwtDecoder() {
    return new RoRJwtDecoderBuilder()
      .withIssuer(rorAuth0Issuer)
      .withAudience(rorAuth0Audience)
      .withAuth0ClaimNamespace(rorAuth0ClaimNamespace)
      .build();
  }

  private JwtDecoder jwtDecoder(String issuer) {
    if (
      StringUtils.hasText(enturInternalAuth0Issuer) &&
      enturInternalAuth0Issuer.equals(issuer)
    ) {
      return enturInternalAuth0JwtDecoder();
    } else if (
      StringUtils.hasText(enturPartnerAuth0Issuer) &&
      enturPartnerAuth0Issuer.equals(issuer)
    ) {
      return enturPartnerAuth0JwtDecoder();
    } else if (
      StringUtils.hasText(rorAuth0Issuer) && rorAuth0Issuer.equals(issuer)
    ) {
      return rorAuth0JwtDecoder();
    } else {
      throw new IllegalArgumentException(
        "Received JWT token with unknown OAuth2 issuer: " + issuer
      );
    }
  }

  private String toIssuer(HttpServletRequest request) {
    try {
      String token = this.resolver.resolve(request);
      String issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
      logger.debug("Received JWT token from OAuth2 issuer {}", issuer);
      return issuer;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  AuthenticationManager fromIssuer(String issuer) {
    return Optional
      .ofNullable(issuer)
      .map(this::jwtDecoder)
      .map(this::jwtAuthenticationProvider)
      .orElseThrow(() ->
        new IllegalArgumentException(
          "Received JWT token with null OAuth2 issuer"
        )
      )::authenticate;
  }

  @Override
  public AuthenticationManager resolve(HttpServletRequest request) {
    return this.authenticationManagers.computeIfAbsent(
        toIssuer(request),
        this::fromIssuer
      );
  }

  private JwtAuthenticationProvider jwtAuthenticationProvider(
    JwtDecoder jwtDecoder
  ) {
    JwtAuthenticationProvider jwtAuthenticationProvider =
      new JwtAuthenticationProvider(jwtDecoder);
    jwtAuthenticationProvider.setJwtAuthenticationConverter(
      new RorAuthenticationConverter()
    );
    return jwtAuthenticationProvider;
  }
}
