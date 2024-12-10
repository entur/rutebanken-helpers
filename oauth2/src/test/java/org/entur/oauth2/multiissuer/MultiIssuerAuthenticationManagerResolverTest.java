package org.entur.oauth2.multiissuer;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
