package org.rutebanken.helper.stopplace.changelog.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class StopPlaceRepositoryTest {

  @Mock
  private WebClient webClient;

  @Mock
  private WebClient.Builder webClientBuilder;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersSpec;

  @Mock
  private WebClient.ResponseSpec responseSpec;

  private StopPlaceRepository repository;

  private final String tiamatUrl = "http://localhost:8080/tiamat";
  private final String stopPlaceId = "NSR:StopPlace:123";

  @BeforeEach
  void setUp() {
    when(webClient.mutate()).thenReturn(webClientBuilder);
    when(webClientBuilder.baseUrl(tiamatUrl)).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);

    repository =
      new StopPlaceRepository(
        webClient,
        tiamatUrl,
        "RELEVANT",
        "RELEVANT",
        "RELEVANT",
        "RELEVANT",
        "RELEVANT",
        true
      );
  }

  @Test
  void testGetStopPlaceUpdateSuccess() {
    // Setup
    String xmlResponse = "<?xml version=\"1.0\"?><StopPlace>test</StopPlace>";
    byte[] responseBytes = xmlResponse.getBytes(StandardCharsets.UTF_8);

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class)))
      .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(byte[].class))
      .thenReturn(Mono.just(responseBytes));

    // Execute
    InputStream result = repository.getStopPlaceUpdate(stopPlaceId);

    // Verify
    assertNotNull(result);

    // Verify URI builder was called with correct parameters
    ArgumentCaptor<Function<WebClient.UriSpec, URI>> uriCaptor =
      ArgumentCaptor.forClass(Function.class);
    verify(requestHeadersUriSpec).uri(uriCaptor.capture());

    // Read the result stream
    try {
      String actualContent = new String(
        result.readAllBytes(),
        StandardCharsets.UTF_8
      );
      assertEquals(xmlResponse, actualContent);
    } catch (Exception e) {
      fail("Failed to read response stream: " + e.getMessage());
    }
  }

  @Test
  void testGetStopPlaceUpdateEmptyResponse() {
    // Setup
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class)))
      .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(byte[].class))
      .thenReturn(Mono.just(new byte[0]));

    // Execute & Verify
    StopPlaceFetchException exception = assertThrows(
      StopPlaceFetchException.class,
      () -> repository.getStopPlaceUpdate(stopPlaceId)
    );

    assertTrue(exception.getMessage().contains(stopPlaceId));
    assertNotNull(exception.getCause());
  }

  @Test
  void testGetStopPlaceUpdateNullResponse() {
    // Setup
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class)))
      .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(byte[].class)).thenReturn(Mono.empty());

    // Execute & Verify
    StopPlaceFetchException exception = assertThrows(
      StopPlaceFetchException.class,
      () -> repository.getStopPlaceUpdate(stopPlaceId)
    );

    assertTrue(exception.getMessage().contains(stopPlaceId));
    assertNotNull(exception.getCause());
  }

  @Test
  void testGetStopPlaceUpdateHttpError() {
    // Setup
    WebClientResponseException httpError = WebClientResponseException.create(
      404,
      "Not Found",
      null,
      "Stop place not found".getBytes(),
      null
    );

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class)))
      .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(byte[].class))
      .thenReturn(Mono.error(httpError));

    // Execute & Verify
    StopPlaceFetchException exception = assertThrows(
      StopPlaceFetchException.class,
      () -> repository.getStopPlaceUpdate(stopPlaceId)
    );

    assertTrue(exception.getMessage().contains(stopPlaceId));
    assertEquals(httpError, exception.getCause());
  }

  @Test
  void testGetStopPlaceUpdateNetworkError() {
    // Setup
    RuntimeException networkError = new RuntimeException("Connection timeout");

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class)))
      .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(byte[].class))
      .thenReturn(Mono.error(networkError));

    // Execute & Verify
    StopPlaceFetchException exception = assertThrows(
      StopPlaceFetchException.class,
      () -> repository.getStopPlaceUpdate(stopPlaceId)
    );

    assertTrue(exception.getMessage().contains(stopPlaceId));
    assertEquals(networkError, exception.getCause());
  }

  @Test
  void testGetStopPlaceUpdateWithAllVersionsFalse() {
    // Test with allVersions = false
    StopPlaceRepository repoWithoutAllVersions = new StopPlaceRepository(
      webClient,
      tiamatUrl,
      "ALL",
      "NONE",
      "RELEVANT",
      "ALL",
      "NONE",
      false // allVersions = false
    );

    String xmlResponse =
      "<?xml version=\"1.0\"?><StopPlace>latest version only</StopPlace>";
    byte[] responseBytes = xmlResponse.getBytes(StandardCharsets.UTF_8);

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class)))
      .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(byte[].class))
      .thenReturn(Mono.just(responseBytes));

    InputStream result = repoWithoutAllVersions.getStopPlaceUpdate(stopPlaceId);
    assertNotNull(result);
  }

  @Test
  void testGetStopPlaceUpdateLargeResponse() {
    // Test with a large response (simulating 5MB)
    byte[] largeResponse = new byte[5 * 1024 * 1024];
    for (int i = 0; i < largeResponse.length; i++) {
      largeResponse[i] = (byte) (i % 256);
    }

    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(Function.class)))
      .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(byte[].class))
      .thenReturn(Mono.just(largeResponse));

    InputStream result = repository.getStopPlaceUpdate(stopPlaceId);
    assertNotNull(result);

    try {
      byte[] actualBytes = result.readAllBytes();
      assertArrayEquals(largeResponse, actualBytes);
    } catch (Exception e) {
      fail("Failed to read large response: " + e.getMessage());
    }
  }
}
