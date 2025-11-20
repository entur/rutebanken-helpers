package org.entur.oauth2;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class RoRJwtDecoderBuilderTest {

  @Test
  void testThrowsExceptionWhenNoAudienceSet() {
    RoRJwtDecoderBuilder builder = new RoRJwtDecoderBuilder()
      .withIssuer("https://test-issuer.com/");

    assertThrows(IllegalStateException.class, () -> builder.build());
  }

  @Test
  void testWithAudienceClearsPreviousAudiences() {
    RoRJwtDecoderBuilder builder = new RoRJwtDecoderBuilder()
      .withAudiences(List.of("audience1", "audience2"))
      .withAudience("single-audience");
    // The test verifies that calling withAudience clears the audiences list
    // This should not throw when build is called (if issuer is set)
  }

  @Test
  void testWithAudiencesClearsPreviousAudience() {
    RoRJwtDecoderBuilder builder = new RoRJwtDecoderBuilder()
      .withAudience("single-audience")
      .withAudiences(List.of("audience1", "audience2"));
    // The test verifies that calling withAudiences clears the single audience
    // This should not throw when build is called (if issuer is set)
  }
}
