package org.entur.oauth2.multiissuer;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MultiIssuerAuthenticationManagerResolverTest {

  private MultiIssuerAuthenticationManagerResolver multiIssuerAuthenticationManagerResolver;

  @BeforeEach
  void setup() {
    multiIssuerAuthenticationManagerResolver =
      new MultiIssuerAuthenticationManagerResolverBuilder().build();
  }

  @Test
  void testUnknownIssuer() {
    assertThrows(
      IllegalArgumentException.class,
      () -> multiIssuerAuthenticationManagerResolver.fromIssuer("unknown")
    );
  }

  @Test
  void testNullIssuer() {
    assertThrows(
      IllegalArgumentException.class,
      () -> multiIssuerAuthenticationManagerResolver.fromIssuer(null)
    );
  }

  @Test
  void testBuilderWithMultipleAudiences() {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturPartnerAuth0Audiences(List.of("audience1", "audience2"))
        .withEnturPartnerAuth0Issuer("https://partner.auth0.com/")
        .build();

    // Just verify that the builder accepts multiple audiences and creates a resolver
    assertThrows(
      IllegalArgumentException.class,
      () -> resolver.fromIssuer("unknown")
    );
  }

  @Test
  void testBuilderWithSingleAudience() {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturPartnerAuth0Audience("single-audience")
        .withEnturPartnerAuth0Issuer("https://partner.auth0.com/")
        .build();

    // Just verify backward compatibility
    assertThrows(
      IllegalArgumentException.class,
      () -> resolver.fromIssuer("unknown")
    );
  }

  @Test
  void testEnturInternalWithMultipleAudiences() {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturInternalAuth0Audiences(List.of("audience1", "audience2"))
        .withEnturInternalAuth0Issuer("https://internal.auth0.com/")
        .build();

    // Just verify that the builder accepts multiple audiences and creates a resolver
    assertThrows(
      IllegalArgumentException.class,
      () -> resolver.fromIssuer("unknown")
    );
  }

  @Test
  void testEnturInternalWithSingleAudience() {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturInternalAuth0Audience("single-audience")
        .withEnturInternalAuth0Issuer("https://internal.auth0.com/")
        .build();

    // Just verify backward compatibility
    assertThrows(
      IllegalArgumentException.class,
      () -> resolver.fromIssuer("unknown")
    );
  }
}
