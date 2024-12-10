package org.entur.oauth2.multiissuer;

public class MultiIssuerAuthenticationManagerResolverBuilder {

  private String enturInternalAuth0Audience;
  private String enturInternalAuth0Issuer;
  private String enturPartnerAuth0Audience;
  private String enturPartnerAuth0Issuer;
  private String rorAuth0Audience;
  private String rorAuth0Issuer;
  private String rorAuth0ClaimNamespace;

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturInternalAuth0Audience(
    String enturInternalAuth0Audience
  ) {
    this.enturInternalAuth0Audience = enturInternalAuth0Audience;
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
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturPartnerAuth0Issuer(
    String enturPartnerAuth0Issuer
  ) {
    this.enturPartnerAuth0Issuer = enturPartnerAuth0Issuer;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withRorAuth0Audience(
    String rorAuth0Audience
  ) {
    this.rorAuth0Audience = rorAuth0Audience;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withRorAuth0Issuer(
    String rorAuth0Issuer
  ) {
    this.rorAuth0Issuer = rorAuth0Issuer;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withRorAuth0ClaimNamespace(
    String rorAuth0ClaimNamespace
  ) {
    this.rorAuth0ClaimNamespace = rorAuth0ClaimNamespace;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolver build() {
    return new MultiIssuerAuthenticationManagerResolver(
      enturInternalAuth0Audience,
      enturInternalAuth0Issuer,
      enturPartnerAuth0Audience,
      enturPartnerAuth0Issuer,
      rorAuth0Audience,
      rorAuth0Issuer,
      rorAuth0ClaimNamespace
    );
  }
}
