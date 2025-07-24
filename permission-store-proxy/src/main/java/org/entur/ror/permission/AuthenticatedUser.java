package org.entur.ror.permission;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

/**
 * An authenticated OAuth2 user, identified by an OAuth2 "sub" claim in a JWT token.
 * This can be either an end user or a m2m client.
 */
public final class AuthenticatedUser {

  public static final long UNKNOWN_ORGANISATION = -1L;

  static final String ENTUR_CLAIM_ORGANISATION_ID =
    "https://entur.io/organisationID";
  /**
   * TODO Permission store migration: Obsolete, to be removed after migration.
   */
  static final String OAUTH2_CLAIM_PERMISSIONS = "permissions";
  /**
   * TODO Permission store migration: Obsolete, to be removed after migration.
   */
  static final String ROR_CLAIM_PREFERRED_USERNAME =
    "https://ror.entur.io/preferred_username";

  private final String subject;
  private final long organisationId;
  /**
   * TODO Permission store migration: Obsolete, to be removed after migration.
   */
  private final List<String> permissions;
  private final String issuer;

  /**
   * TODO Permission store migration: Obsolete, to be removed after migration.
   */
  private final String username;

  public static AuthenticatedUser of(JwtAuthenticationToken authentication) {
    AuthenticatedUserBuilder authenticatedUserBuilder =
      new AuthenticatedUserBuilder();
    authenticatedUserBuilder.withSubject(
      authentication.getToken().getSubject()
    );
    authenticatedUserBuilder.withIssuer(
      authentication.getToken().getIssuer().toString()
    );

    Optional
      .ofNullable(
        authentication.getToken().getClaim(ENTUR_CLAIM_ORGANISATION_ID)
      )
      .ifPresent(id -> authenticatedUserBuilder.withOrganisationId((Long) id));

    Optional
      .ofNullable(
        authentication.getToken().getClaimAsStringList(OAUTH2_CLAIM_PERMISSIONS)
      )
      .ifPresent(authenticatedUserBuilder::withPermissions);

    Optional
      .ofNullable(
        authentication.getToken().getClaimAsString(ROR_CLAIM_PREFERRED_USERNAME)
      )
      .ifPresent(authenticatedUserBuilder::withUsername);

    return authenticatedUserBuilder.build();
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
    this.subject = Objects.requireNonNull(subject, "Missing subject");
    this.issuer = Objects.requireNonNull(issuer, "Missing issuer");
    this.organisationId = organisationId;
    this.permissions = permissions;
    this.username = username;
    if (
      (isPartner() || isInternal()) && organisationId == UNKNOWN_ORGANISATION
    ) {
      throw new IllegalArgumentException(
        "Missing organisation ID for Entur Partner/Internal user " + subject
      );
    }
    if (isRor() && !StringUtils.hasText(username)) {
      throw new IllegalArgumentException(
        "Missing username for RoR user " + subject
      );
    }
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

  /**
   * TODO Permission store migration: Obsolete, to be removed after migration.
   */
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

  @Override
  public String toString() {
    return (
      "AuthenticatedUser{" +
      "subject='" +
      subject +
      '\'' +
      ", organisationId=" +
      organisationId +
      ", permissions=" +
      permissions +
      ", issuer='" +
      issuer +
      '\'' +
      ", username='" +
      username +
      '\'' +
      '}'
    );
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    AuthenticatedUser that = (AuthenticatedUser) o;
    return (
      organisationId == that.organisationId &&
      Objects.equals(subject, that.subject) &&
      Objects.equals(permissions, that.permissions) &&
      Objects.equals(issuer, that.issuer) &&
      Objects.equals(username, that.username)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, organisationId, permissions, issuer, username);
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
    private String issuer;
    private long organisationId = UNKNOWN_ORGANISATION;
    private List<String> permissions = List.of();
    private String username = "";

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
