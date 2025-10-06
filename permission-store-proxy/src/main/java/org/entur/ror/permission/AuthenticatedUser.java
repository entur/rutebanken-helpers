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

  public static final long UNKNOWN_ORGANISATION = -1L;

  static final String ENTUR_CLAIM_ORGANISATION_ID =
    "https://entur.io/organisationID";

  private final String subject;
  private final long organisationId;

  private final String issuer;

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

    return authenticatedUserBuilder.build();
  }

  public static AuthenticatedUser ofDTO(AuthenticatedUserDTO dto) {
    return new AuthenticatedUserBuilder()
      .withSubject(dto.subject)
      .withOrganisationId(dto.organisationId)
      .withIssuer(dto.issuer)
      .build();
  }

  AuthenticatedUser(String subject, long organisationId, String issuer) {
    this.subject = Objects.requireNonNull(subject, "Missing subject");
    this.issuer = Objects.requireNonNull(issuer, "Missing issuer");
    this.organisationId = organisationId;
    if (
      (isPartner() || isInternal()) && organisationId == UNKNOWN_ORGANISATION
    ) {
      throw new IllegalArgumentException(
        "Missing organisation ID for Entur Partner/Internal user " + subject
      );
    }
  }

  public String subject() {
    return subject;
  }

  public long organisationId() {
    return organisationId;
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

  public AuthenticatedUserDTO toDTO() {
    return new AuthenticatedUserDTO(subject, organisationId, issuer);
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
      ", issuer='" +
      issuer +
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
      Objects.equals(issuer, that.issuer)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(subject, organisationId, issuer);
  }

  /**
   * A lightweight representation of the authenticated user, to be used for JSON serialization/deserialization.
   */
  public record AuthenticatedUserDTO(
    String subject,
    long organisationId,
    String issuer
  ) {}

  public static class AuthenticatedUserBuilder {

    private String subject;
    private String issuer;
    private long organisationId = UNKNOWN_ORGANISATION;

    public AuthenticatedUserBuilder withSubject(String subject) {
      this.subject = subject;
      return this;
    }

    public AuthenticatedUserBuilder withOrganisationId(long organisationId) {
      this.organisationId = organisationId;
      return this;
    }

    public AuthenticatedUserBuilder withIssuer(String issuer) {
      this.issuer = issuer;
      return this;
    }

    public AuthenticatedUser build() {
      return new AuthenticatedUser(subject, organisationId, issuer);
    }
  }
}
