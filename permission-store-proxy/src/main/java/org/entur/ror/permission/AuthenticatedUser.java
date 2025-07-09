package org.entur.ror.permission;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * An authenticated OAuth2 user, identified by an OAuth2 "sub" claim in a JWT token.
 * This can be either an end user or a m2m client.
 */
public final class AuthenticatedUser {

  private static final String ENTUR_CLAIM_ORGANISATION_ID =
    "https://entur.io/organisationID";
  private static final String OAUTH2_CLAIM_PERMISSIONS = "permissions";

  private static final String ROR_CLAIM_PREFERRED_USERNAME =
    "https://ror.entur.io/preferred_username";

  private final String subject;
  private final long organisationId;
  private final List<String> permissions;
  private final String issuer;
  private final String username;

  public static AuthenticatedUser of(JwtAuthenticationToken authentication) {
    String subject = authentication.getToken().getSubject();
    String issuer = authentication.getToken().getIssuer().toString();

    long organisationId = (long) Optional
      .ofNullable(
        authentication.getToken().getClaim(ENTUR_CLAIM_ORGANISATION_ID)
      )
      .orElse(-1L);

    List<String> permissions = Optional
      .ofNullable(
        authentication.getToken().getClaimAsStringList(OAUTH2_CLAIM_PERMISSIONS)
      )
      .orElse(List.of());

    String username = Optional
      .ofNullable(
        authentication.getToken().getClaimAsString(ROR_CLAIM_PREFERRED_USERNAME)
      )
      .orElse("");

    return new AuthenticatedUserBuilder()
      .withSubject(subject)
      .withOrganisationId(organisationId)
      .withPermissions(permissions)
      .withIssuer(issuer)
      .withUsername(username)
      .build();
  }

  public static AuthenticatedUser ofDTO(AuthenticatedUserDTO dto) {
    return new AuthenticatedUserBuilder()
      .withSubject(dto.subject)
      .withOrganisationId(dto.organisationId)
      .withPermissions(dto.permissions)
      .withIssuer(dto.issuer)
      .withUsername(dto.username)
      .build();
  }

  AuthenticatedUser(
    String subject,
    long organisationId,
    List<String> permissions,
    String issuer,
    String username
  ) {
    this.subject = Objects.requireNonNull(subject);
    this.organisationId = organisationId;
    this.permissions = permissions;
    this.issuer = issuer;
    this.username = username;
  }

  public String subject() {
    return subject;
  }

  public long organisationId() {
    return organisationId;
  }

  public List<String> permissions() {
    return permissions;
  }

  public String username() {
    return username;
  }

  public boolean isClient() {
    return subject.endsWith("@clients");
  }

  public boolean isInternal() {
    return List
      .of(
        "https://internal.dev.entur.org/",
        "https://internal.staging.entur.org/",
        "https://internal.entur.org/"
      )
      .contains(issuer);
  }

  public boolean isPartner() {
    return List
      .of(
        "https://partner.dev.entur.org/",
        "https://partner.staging.entur.org/",
        "https://partner.entur.org/"
      )
      .contains(issuer);
  }

  public boolean isRor() {
    return List
      .of(
        "https://ror-entur-dev.eu.auth0.com/",
        "https://ror-entur-staging.eu.auth0.com/",
        "https://auth2.entur.org/"
      )
      .contains(issuer);
  }

  public AuthenticatedUserDTO toDTO() {
    return new AuthenticatedUserDTO(
      subject,
      organisationId,
      permissions,
      issuer,
      username
    );
  }

  /**
   * A lightweight representation of the authenticated user, to be used for JSON serialization/deserialization.
   */
  public record AuthenticatedUserDTO(
    String subject,
    long organisationId,
    List<String> permissions,
    String issuer,
    String username
  ) {}

  public static class AuthenticatedUserBuilder {

    private String subject;
    private long organisationId;
    private List<String> permissions = List.of();
    private String issuer;
    private String username;

    public AuthenticatedUserBuilder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    public AuthenticatedUserBuilder withOrganisationId(long organisationId) {
      this.organisationId = organisationId;
      return this;
    }

    public AuthenticatedUserBuilder withPermissions(List<String> permissions) {
      this.permissions = permissions;
      return this;
    }

    public AuthenticatedUserBuilder withIssuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    public AuthenticatedUserBuilder withUsername(String username) {
      this.username = username;
      return this;
    }

    public AuthenticatedUser build() {
      return new AuthenticatedUser(
        subject,
        organisationId,
        permissions,
        issuer,
        username
      );
    }
  }
}
