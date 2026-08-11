package org.entur.oauth2.multiissuer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

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

  @Test
  void testResolveRejectsRequestWithoutBearerToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThrows(
      IllegalArgumentException.class,
      () -> multiIssuerAuthenticationManagerResolver.resolve(request)
    );
  }

  @Test
  void testResolveRejectsMalformedToken() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer not-a-jwt");

    assertThrows(
      IllegalArgumentException.class,
      () -> multiIssuerAuthenticationManagerResolver.resolve(request)
    );
  }

  @Test
  void testResolveRejectsTokenFromUnconfiguredIssuer() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(
      "Authorization",
      "Bearer " + unsignedToken("test-unknown-issuer")
    );

    // The default resolver has no issuers configured, so the parsed issuer cannot be routed to a
    // decoder and the whole resolve call must fail.
    assertThrows(
      IllegalArgumentException.class,
      () -> multiIssuerAuthenticationManagerResolver.resolve(request)
    );
  }

  @Test
  void testEnturPartnerDecoderRequiresConfiguredAudience() {
    // The default resolver configures neither a single audience nor a list, so building the Entur
    // Partner decoder must fail before any OIDC discovery is attempted.
    assertThrows(
      IllegalStateException.class,
      multiIssuerAuthenticationManagerResolver::enturPartnerAuth0JwtDecoder
    );
  }

  @Test
  void testEnturInternalDecoderRequiresConfiguredAudience() {
    assertThrows(
      IllegalStateException.class,
      multiIssuerAuthenticationManagerResolver::enturInternalAuth0JwtDecoder
    );
  }

  @Test
  void testJwksRestOperationsUsesConfigurableSimpleRequestFactory() {
    // The JWKS/OIDC-discovery client must be a RestTemplate backed by a
    // SimpleClientHttpRequestFactory, since that is the factory whose connect/read timeouts the
    // resolver configures.
    RestOperations restOperations =
      multiIssuerAuthenticationManagerResolver.jwksRestOperations();

    RestTemplate restTemplate = assertInstanceOf(
      RestTemplate.class,
      restOperations
    );
    assertInstanceOf(
      SimpleClientHttpRequestFactory.class,
      restTemplate.getRequestFactory()
    );
  }

  @Test
  @SuppressWarnings("deprecation")
  void testDeprecatedConstructorProducesUsableResolver() {
    // The deprecated single-audience constructor delegates through the list-based and
    // timeout-aware constructors. Building via it must still yield a resolver that rejects unknown
    // issuers.
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolver(
        "test-internal-audience",
        "test-internal-issuer",
        "test-partner-audience",
        "test-partner-issuer",
        "test-claim-namespace"
      );

    assertThrows(
      IllegalArgumentException.class,
      () -> resolver.fromIssuer("unknown")
    );
  }

  @Test
  void testAudienceListConstructorProducesUsableResolver() {
    // The list-based constructor applies the default JWKS timeouts. Building via it must yield a
    // resolver that rejects unknown issuers.
    MultiIssuerAuthenticationManagerResolver resolver =
      new MultiIssuerAuthenticationManagerResolver(
        null,
        List.of("test-internal-audience"),
        "test-internal-issuer",
        null,
        List.of("test-partner-audience"),
        "test-partner-issuer"
      );

    assertThrows(
      IllegalArgumentException.class,
      () -> resolver.fromIssuer("unknown")
    );
  }

  /**
   * Builds an unsigned JWT carrying only the issuer claim. The resolver reads the issuer from the
   * token to select a decoder and does not verify the signature while doing so, so an unsigned
   * token is enough to drive that routing in a unit test.
   */
  private static String unsignedToken(String issuer) {
    JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer).build();
    return new PlainJWT(claims).serialize();
  }
}
