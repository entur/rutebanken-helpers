package org.entur.oauth2.multiissuer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MultiIssuerAuthenticationManagerResolverBuilder}. These exercise the
 * builder's own responsibilities in isolation: the fluent-chaining contract, the mutual exclusion
 * between the single-audience and audience-list setters, and the wiring of the JWKS timeouts
 * (defaults and overrides) into the resolver it produces. None of these tests require network
 * access — the decoder's audience validation throws before any OIDC discovery is attempted.
 */
class MultiIssuerAuthenticationManagerResolverBuilderTest {

  @Test
  void fluentSettersReturnSameBuilderForChaining() {
    MultiIssuerAuthenticationManagerResolverBuilder builder =
      new MultiIssuerAuthenticationManagerResolverBuilder();

    assertSame(builder, builder.withEnturInternalAuth0Audience("audience"));
    assertSame(
      builder,
      builder.withEnturInternalAuth0Audiences(List.of("audience"))
    );
    assertSame(builder, builder.withEnturInternalAuth0Issuer("issuer"));
    assertSame(builder, builder.withEnturPartnerAuth0Audience("audience"));
    assertSame(
      builder,
      builder.withEnturPartnerAuth0Audiences(List.of("audience"))
    );
    assertSame(builder, builder.withEnturPartnerAuth0Issuer("issuer"));
    assertSame(builder, builder.withJwksConnectTimeout(Duration.ofSeconds(1)));
    assertSame(builder, builder.withJwksReadTimeout(Duration.ofSeconds(1)));
  }

  @Test
  void buildAppliesDefaultTimeoutsWhenNotOverridden() throws Exception {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder().build();

    assertNotNull(resolver);
    assertEquals(
      MultiIssuerAuthenticationManagerResolver.DEFAULT_JWKS_CONNECT_TIMEOUT,
      timeoutField(resolver, "jwksConnectTimeout")
    );
    assertEquals(
      MultiIssuerAuthenticationManagerResolver.DEFAULT_JWKS_READ_TIMEOUT,
      timeoutField(resolver, "jwksReadTimeout")
    );
  }

  @Test
  void buildPassesThroughOverriddenTimeouts() throws Exception {
    Duration connectTimeout = Duration.ofSeconds(7);
    Duration readTimeout = Duration.ofSeconds(11);

    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withJwksConnectTimeout(connectTimeout)
        .withJwksReadTimeout(readTimeout)
        .build();

    assertEquals(connectTimeout, timeoutField(resolver, "jwksConnectTimeout"));
    assertEquals(readTimeout, timeoutField(resolver, "jwksReadTimeout"));
  }

  @Test
  void settingSinglePartnerAudienceClearsPreviouslyConfiguredAudienceList() {
    // Clearing the single audience (setting it to null) must also clear the audience list;
    // otherwise the list would still satisfy the decoder and no exception would be thrown. The
    // IllegalStateException therefore proves the single-audience setter cleared the list.
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturPartnerAuth0Audiences(List.of("audience1", "audience2"))
        .withEnturPartnerAuth0Audience(null)
        .build();

    assertThrows(
      IllegalStateException.class,
      resolver::enturPartnerAuth0JwtDecoder
    );
  }

  @Test
  void settingPartnerAudienceListClearsPreviouslyConfiguredSingleAudience() {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturPartnerAuth0Audience("single-audience")
        .withEnturPartnerAuth0Audiences(null)
        .build();

    assertThrows(
      IllegalStateException.class,
      resolver::enturPartnerAuth0JwtDecoder
    );
  }

  @Test
  void settingSingleInternalAudienceClearsPreviouslyConfiguredAudienceList() {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturInternalAuth0Audiences(List.of("audience1", "audience2"))
        .withEnturInternalAuth0Audience(null)
        .build();

    assertThrows(
      IllegalStateException.class,
      resolver::enturInternalAuth0JwtDecoder
    );
  }

  @Test
  void settingInternalAudienceListClearsPreviouslyConfiguredSingleAudience() {
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturInternalAuth0Audience("single-audience")
        .withEnturInternalAuth0Audiences(null)
        .build();

    assertThrows(
      IllegalStateException.class,
      resolver::enturInternalAuth0JwtDecoder
    );
  }

  /**
   * Reads a {@link Duration} field from the resolver. The resolver exposes no getters, so the only
   * way to assert that the builder wired the timeouts through is to read them back reflectively.
   */
  private static Duration timeoutField(
    MultiIssuerAuthenticationManagerResolver resolver,
    String fieldName
  ) throws Exception {
    Field field =
      MultiIssuerAuthenticationManagerResolver.class.getDeclaredField(
          fieldName
        );
    field.setAccessible(true);
    return (Duration) field.get(resolver);
  }
}
