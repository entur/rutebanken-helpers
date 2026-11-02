package org.entur.oauth2.multiissuer;

import java.time.Duration;
import java.util.List;

public class MultiIssuerAuthenticationManagerResolverBuilder {

  private String enturInternalAuth0Audience;
  private List<String> enturInternalAuth0Audiences;
  private String enturInternalAuth0Issuer;
  private String enturPartnerAuth0Audience;
  private List<String> enturPartnerAuth0Audiences;
  private String enturPartnerAuth0Issuer;
  private Duration jwksConnectTimeout =
    MultiIssuerAuthenticationManagerResolver.DEFAULT_JWKS_CONNECT_TIMEOUT;
  private Duration jwksReadTimeout =
    MultiIssuerAuthenticationManagerResolver.DEFAULT_JWKS_READ_TIMEOUT;

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturInternalAuth0Audience(
    String enturInternalAuth0Audience
  ) {
    this.enturInternalAuth0Audience = enturInternalAuth0Audience;
    this.enturInternalAuth0Audiences = null;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturInternalAuth0Audiences(
    List<String> enturInternalAuth0Audiences
  ) {
    this.enturInternalAuth0Audiences = enturInternalAuth0Audiences;
    this.enturInternalAuth0Audience = null;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturInternalAuth0Issuer(
    String enturInternalAuth0Issuer
  ) {
    this.enturInternalAuth0Issuer = enturInternalAuth0Issuer;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturPartnerAuth0Audience(
    String enturPartnerAuth0Audience
  ) {
    this.enturPartnerAuth0Audience = enturPartnerAuth0Audience;
    this.enturPartnerAuth0Audiences = null;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturPartnerAuth0Audiences(
    List<String> enturPartnerAuth0Audiences
  ) {
    this.enturPartnerAuth0Audiences = enturPartnerAuth0Audiences;
    this.enturPartnerAuth0Audience = null;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturPartnerAuth0Issuer(
    String enturPartnerAuth0Issuer
  ) {
    this.enturPartnerAuth0Issuer = enturPartnerAuth0Issuer;
    return this;
  }

  /**
   * Override the connect timeout for the JWKS/OIDC-discovery HTTP client. Defaults to
   * {@link MultiIssuerAuthenticationManagerResolver#DEFAULT_JWKS_CONNECT_TIMEOUT}.
   */
  public MultiIssuerAuthenticationManagerResolverBuilder withJwksConnectTimeout(
    Duration jwksConnectTimeout
  ) {
    this.jwksConnectTimeout = jwksConnectTimeout;
    return this;
  }

  /**
   * Override the read timeout for the JWKS/OIDC-discovery HTTP client. Defaults to
   * {@link MultiIssuerAuthenticationManagerResolver#DEFAULT_JWKS_READ_TIMEOUT}.
   */
  public MultiIssuerAuthenticationManagerResolverBuilder withJwksReadTimeout(
    Duration jwksReadTimeout
  ) {
    this.jwksReadTimeout = jwksReadTimeout;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolver build() {
    return new MultiIssuerAuthenticationManagerResolver(
      enturInternalAuth0Audience,
      enturInternalAuth0Audiences,
      enturInternalAuth0Issuer,
      enturPartnerAuth0Audience,
      enturPartnerAuth0Audiences,
      enturPartnerAuth0Issuer,
      jwksConnectTimeout,
      jwksReadTimeout
    );
  }
}
