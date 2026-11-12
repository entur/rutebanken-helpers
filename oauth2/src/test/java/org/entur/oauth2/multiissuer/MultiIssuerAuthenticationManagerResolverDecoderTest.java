package org.entur.oauth2.multiissuer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Unit tests for the decoder factory methods {@link
 * MultiIssuerAuthenticationManagerResolver#enturInternalAuth0JwtDecoder()} and {@link
 * MultiIssuerAuthenticationManagerResolver#enturPartnerAuth0JwtDecoder()}.
 *
 * <p>These methods build a {@link JwtDecoder} via OIDC discovery, so they cannot be exercised
 * without an issuer endpoint. A local, in-process {@link HttpServer} plays the role of an Auth0
 * tenant — serving an OIDC discovery document and a JWKS endpoint — so the decoders are built and
 * exercised with real, signed tokens and no external network access. This mirrors the in-process
 * HTTP-stub approach used by {@link MultiIssuerJwksTimeoutTest}.
 */
class MultiIssuerAuthenticationManagerResolverDecoderTest {

  private static final String KEY_ID = "test-key";
  private static final String AUDIENCE = "test-audience";
  private static final String OTHER_AUDIENCE = "test-other-audience";
  private static final String SUBJECT = "test-subject";

  private HttpServer server;
  private String issuer;
  private RSAKey rsaKey;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    rsaKey =
      new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .keyID(KEY_ID)
        .keyUse(KeyUse.SIGNATURE)
        .algorithm(JWSAlgorithm.RS256)
        .build();

    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    issuer = "http://127.0.0.1:" + server.getAddress().getPort();
    server.createContext("/", this::handle);
    server.start();
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  private void handle(HttpExchange exchange) throws java.io.IOException {
    String path = exchange.getRequestURI().getPath();
    String response;
    if ("/.well-known/openid-configuration".equals(path)) {
      response =
        "{" +
        "\"issuer\":\"" +
        issuer +
        "\"," +
        "\"jwks_uri\":\"" +
        issuer +
        "/.well-known/jwks.json\"," +
        "\"authorization_endpoint\":\"" +
        issuer +
        "/authorize\"," +
        "\"token_endpoint\":\"" +
        issuer +
        "/oauth/token\"," +
        "\"response_types_supported\":[\"code\",\"token\",\"id_token\"]," +
        "\"subject_types_supported\":[\"public\"]," +
        "\"id_token_signing_alg_values_supported\":[\"RS256\"]" +
        "}";
    } else if ("/.well-known/jwks.json".equals(path)) {
      response = new JWKSet(rsaKey).toPublicJWKSet().toString();
    } else {
      exchange.sendResponseHeaders(404, -1);
      exchange.close();
      return;
    }
    byte[] body = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(body);
    }
  }

  private String signedToken(List<String> audiences) throws Exception {
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
      .issuer(issuer)
      .audience(audiences)
      .subject(SUBJECT)
      .issueTime(Date.from(Instant.now()))
      .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
      .build();
    SignedJWT signedJWT = new SignedJWT(
      new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
      claims
    );
    signedJWT.sign(new RSASSASigner(rsaKey));
    return signedJWT.serialize();
  }

  @Test
  void enturPartnerDecoderWithSingleAudienceDecodesValidToken()
    throws Exception {
    JwtDecoder decoder = new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturPartnerAuth0Issuer(issuer)
      .withEnturPartnerAuth0Audience(AUDIENCE)
      .build()
      .enturPartnerAuth0JwtDecoder();

    Jwt jwt = decoder.decode(signedToken(List.of(AUDIENCE)));

    assertEquals(SUBJECT, jwt.getSubject());
    assertEquals(issuer, jwt.getIssuer().toString());
    assertTrue(jwt.getAudience().contains(AUDIENCE));
  }

  @Test
  void enturPartnerDecoderWithAudienceListDecodesValidToken() throws Exception {
    JwtDecoder decoder = new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturPartnerAuth0Issuer(issuer)
      .withEnturPartnerAuth0Audiences(List.of(AUDIENCE, OTHER_AUDIENCE))
      .build()
      .enturPartnerAuth0JwtDecoder();

    Jwt jwt = decoder.decode(signedToken(List.of(AUDIENCE)));

    assertTrue(jwt.getAudience().contains(AUDIENCE));
  }

  @Test
  void enturPartnerDecoderRejectsTokenWithWrongAudience() throws Exception {
    JwtDecoder decoder = new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturPartnerAuth0Issuer(issuer)
      .withEnturPartnerAuth0Audience(AUDIENCE)
      .build()
      .enturPartnerAuth0JwtDecoder();

    String tokenWithWrongAudience = signedToken(List.of("test-wrong-audience"));

    assertThrows(
      JwtException.class,
      () -> decoder.decode(tokenWithWrongAudience)
    );
  }

  @Test
  void enturInternalDecoderWithSingleAudienceDecodesValidToken()
    throws Exception {
    JwtDecoder decoder = new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturInternalAuth0Issuer(issuer)
      .withEnturInternalAuth0Audience(AUDIENCE)
      .build()
      .enturInternalAuth0JwtDecoder();

    Jwt jwt = decoder.decode(signedToken(List.of(AUDIENCE)));

    assertEquals(SUBJECT, jwt.getSubject());
    assertEquals(issuer, jwt.getIssuer().toString());
    assertTrue(jwt.getAudience().contains(AUDIENCE));
  }

  @Test
  void enturInternalDecoderWithAudienceListDecodesValidToken()
    throws Exception {
    JwtDecoder decoder = new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturInternalAuth0Issuer(issuer)
      .withEnturInternalAuth0Audiences(List.of(AUDIENCE, OTHER_AUDIENCE))
      .build()
      .enturInternalAuth0JwtDecoder();

    Jwt jwt = decoder.decode(signedToken(List.of(AUDIENCE)));

    assertTrue(jwt.getAudience().contains(AUDIENCE));
  }

  @Test
  void enturInternalDecoderRejectsTokenWithWrongAudience() throws Exception {
    JwtDecoder decoder = new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturInternalAuth0Issuer(issuer)
      .withEnturInternalAuth0Audience(AUDIENCE)
      .build()
      .enturInternalAuth0JwtDecoder();

    String tokenWithWrongAudience = signedToken(List.of("test-wrong-audience"));

    assertThrows(
      JwtException.class,
      () -> decoder.decode(tokenWithWrongAudience)
    );
  }
}
