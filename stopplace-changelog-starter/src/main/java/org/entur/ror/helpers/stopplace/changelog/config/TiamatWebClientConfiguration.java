package org.entur.ror.helpers.stopplace.changelog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration for WebClient used in stop place repository operations.
 *
 * <p>Provides a default WebClient bean with sensible defaults that can be
 * overridden by providing a custom WebClient bean in the application context.</p>
 *
 * @since 5.41.0
 * @author Entur
 */
@Configuration
public class TiamatWebClientConfiguration {

  /**
   * Creates a default WebClient bean with sensible defaults.
   *
   * <p>Configuration:
   * <ul>
   *   <li>Connection timeout: 30 seconds</li>
   *   <li>Response timeout: 60 seconds</li>
   *   <li>Max in-memory buffer: 10MB (for large NeTEx responses)</li>
   * </ul>
   *
   * @param builder the WebClient.Builder to use
   * @return a configured WebClient instance
   */
  @Bean("tiamatWebClient")
  @ConditionalOnMissingBean(name = "tiamatWebClient")
  public WebClient webClient() {
    HttpClient httpClient = HttpClient
      .create()
      .responseTimeout(Duration.ofSeconds(60));

    // Allow larger responses for NeTEx data (30MB)
    ExchangeStrategies exchangeStrategies = ExchangeStrategies
      .builder()
      .codecs(configurer ->
        configurer.defaultCodecs().maxInMemorySize(30 * 1024 * 1024) // 30MB
      )
      .build();

    return WebClient
      .builder()
      .clientConnector(new ReactorClientHttpConnector(httpClient))
      .exchangeStrategies(exchangeStrategies)
      .build();
  }
}
