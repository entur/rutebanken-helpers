package org.entur.oauth2.multiissuer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

/**
 * Tests for the JWKS {@link RestOperations} timeouts configured by
 * {@link MultiIssuerAuthenticationManagerResolver}.
 *
 * <p>Background: Spring Security 7 changed the default JWKS HTTP client timeout from infinite to
 * 500 ms. A cold TLS handshake from a GKE pod to Auth0 regularly exceeds 500 ms, so key-cache
 * refreshes started throwing {@code SocketTimeoutException: Read timed out}. The resolver now
 * builds its decoders with an explicit {@code RestOperations} carrying generous, configurable
 * timeouts. These tests exercise that {@code RestOperations} directly against a slow local HTTP
 * server.
 */
class MultiIssuerJwksTimeoutTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  private String startServerWithDelay(Duration delay) throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
      "/",
      exchange -> {
        try {
          Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(body);
        }
      }
    );
    server.start();
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
  }

  @Test
  void defaultReadTimeoutAllowsJwksResponseSlowerThanSpringSecurityDefault()
    throws Exception {
    // Spring Security 7's default JWKS read timeout is 500 ms. Simulate a JWKS response that takes
    // ~700 ms (a plausible cold TLS handshake to Auth0) and confirm the default 5 s read timeout
    // configured here does not abort it.
    String url = startServerWithDelay(Duration.ofMillis(700));
    RestOperations restOperations =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .build()
        .jwksRestOperations();

    String body = restOperations.getForObject(url, String.class);

    assertEquals("{}", body);
  }

  @Test
  void configuredReadTimeoutIsEnforced() throws Exception {
    // A caller may tighten the read timeout so a genuinely slow JWKS endpoint fails fast rather
    // than tying up worker threads. Confirm the configured value is actually applied.
    String url = startServerWithDelay(Duration.ofMillis(700));
    RestOperations restOperations =
      new MultiIssuerAuthenticationManagerResolverBuilder()
        .withJwksReadTimeout(Duration.ofMillis(200))
        .build()
        .jwksRestOperations();

    assertThrows(
      ResourceAccessException.class,
      () -> restOperations.getForObject(url, String.class)
    );
  }
}
