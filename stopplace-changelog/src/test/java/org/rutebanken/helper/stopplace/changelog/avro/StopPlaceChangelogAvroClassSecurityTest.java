package org.rutebanken.helper.stopplace.changelog.avro;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.Instant;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.ClassSecurityValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.irkalla.avro.EnumType;
import org.rutebanken.irkalla.avro.StopPlaceChangelogEvent;

/**
 * Verifies that the generated Avro classes can be resolved by name once
 * {@link StopPlaceChangelogAvroClassSecurity} has installed the class security validator.
 *
 * <p>The Avro global validator is process-wide state. This test resets it to
 * {@link ClassSecurityValidator#DEFAULT} around each test method so that it observes the
 * Avro defaults regardless of test ordering.
 */
class StopPlaceChangelogAvroClassSecurityTest {

  @BeforeEach
  @AfterEach
  void resetAvroClassSecurityValidator() {
    ClassSecurityValidator.setGlobal(ClassSecurityValidator.DEFAULT);
  }

  @Test
  void generatedClassesAreForbiddenWithAvroDefaults() {
    assertThrows(
      SecurityException.class,
      () ->
        new SpecificData().getClass(StopPlaceChangelogEvent.getClassSchema())
    );
    assertThrows(
      SecurityException.class,
      () -> new SpecificData().getClass(EnumType.getClassSchema())
    );
  }

  @Test
  void generatedClassesAreResolvedByNameOnceTheValidatorIsInstalled() {
    StopPlaceChangelogAvroClassSecurity.trustGeneratedClasses();

    assertEquals(
      StopPlaceChangelogEvent.class,
      new SpecificData().getClass(StopPlaceChangelogEvent.getClassSchema())
    );
    assertEquals(
      EnumType.class,
      new SpecificData().getClass(EnumType.getClassSchema())
    );
  }

  @Test
  void generatedRecordCanBeRoundTripped() throws Exception {
    StopPlaceChangelogAvroClassSecurity.trustGeneratedClasses();

    Instant changed = Instant.ofEpochMilli(1_700_000_000_000L);
    StopPlaceChangelogEvent event = StopPlaceChangelogEvent
      .newBuilder()
      .setStopPlaceId("NSR:StopPlace:123")
      .setStopPlaceVersion(2L)
      .setStopPlaceChanged(changed)
      .setEventType(EnumType.UPDATE)
      .build();

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
    // the single argument constructor resolves the record class by name, unlike the two
    // argument one used by the Confluent serializer, so this exercises the validator as well
    new SpecificDatumWriter<StopPlaceChangelogEvent>(
      StopPlaceChangelogEvent.getClassSchema()
    )
      .write(event, encoder);
    encoder.flush();

    StopPlaceChangelogEvent decoded =
      new SpecificDatumReader<StopPlaceChangelogEvent>(
        StopPlaceChangelogEvent.getClassSchema()
      )
        .read(
          null,
          DecoderFactory.get().binaryDecoder(out.toByteArray(), null)
        );

    assertEquals("NSR:StopPlace:123", decoded.getStopPlaceId().toString());
    assertEquals(2L, decoded.getStopPlaceVersion());
    assertEquals(changed, decoded.getStopPlaceChanged());
    assertEquals(EnumType.UPDATE, decoded.getEventType());
  }

  @Test
  void registrationIsIdempotent() {
    StopPlaceChangelogAvroClassSecurity.trustGeneratedClasses();
    StopPlaceChangelogAvroClassSecurity.trustGeneratedClasses();

    assertDoesNotThrow(() ->
      ClassSecurityValidator.validate(StopPlaceChangelogEvent.class)
    );
    assertDoesNotThrow(() -> ClassSecurityValidator.validate(String.class));
  }

  /**
   * Guards against installing the validator without composing the Avro defaults back in.
   * The generated getters return {@link CharSequence}, so losing the defaults would break
   * deserialization in a different way.
   */
  @Test
  void avroDefaultTrustedClassesSurviveComposition() {
    StopPlaceChangelogAvroClassSecurity.trustGeneratedClasses();

    assertDoesNotThrow(() -> ClassSecurityValidator.validate(CharSequence.class)
    );
    assertDoesNotThrow(() -> ClassSecurityValidator.validate(String.class));
    assertDoesNotThrow(() -> ClassSecurityValidator.validate(Long.class));
  }

  /**
   * The allowlist must stay narrow: only the generated classes are added to the Avro defaults.
   */
  @Test
  void unrelatedClassesRemainForbidden() {
    StopPlaceChangelogAvroClassSecurity.trustGeneratedClasses();

    assertThrows(
      SecurityException.class,
      () -> ClassSecurityValidator.validate(File.class)
    );
  }
}
