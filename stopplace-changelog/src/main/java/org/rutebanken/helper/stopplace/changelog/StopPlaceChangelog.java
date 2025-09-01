package org.rutebanken.helper.stopplace.changelog;

/**
 * Central interface for consuming stop place change events.
 *
 * <p>This interface provides the main entry point for applications that need to listen
 * to stop place lifecycle events (create, update, deactivate, delete). Implementations
 * typically consume these events from a message broker like Kafka.</p>
 *
 * <p>The changelog follows a listener pattern where multiple {@link StopPlaceChangelogListener}
 * instances can be registered to receive notifications about stop place changes. Each
 * listener will be notified of all relevant events based on the configured filters.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Service
 * public class MyService {
 *     private final StopPlaceChangelog changelog;
 *     private final MyStopPlaceHandler handler;
 *
 *     @PostConstruct
 *     public void init() {
 *         changelog.registerStopPlaceChangelogListener(handler);
 *     }
 *
 *     @PreDestroy
 *     public void cleanup() {
 *         changelog.unregisterStopPlaceChangelogListener(handler);
 *     }
 * }
 * }</pre>
 *
 * @see StopPlaceChangelogListener
 * @see org.rutebanken.helper.stopplace.changelog.kafka.KafkaStopPlaceChangelog
 * @since 5.41.0
 * @author Entur
 */
public interface StopPlaceChangelog {
  /**
   * Registers a listener to receive stop place change events.
   *
   * <p>Once registered, the listener will receive notifications for all stop place
   * lifecycle events (create, update, deactivate, delete) that pass any configured
   * filters. Multiple listeners can be registered and each will receive the same
   * events independently.</p>
   *
   * <p>The listener will start receiving events immediately after registration,
   * subject to the underlying message broker's delivery guarantees.</p>
   *
   * @param listener the listener to register, must not be {@code null}
   * @throws IllegalArgumentException if listener is {@code null}
   * @see #unregisterStopPlaceChangelogListener(StopPlaceChangelogListener)
   */
  void registerStopPlaceChangelogListener(StopPlaceChangelogListener listener);

  /**
   * Unregisters a previously registered listener.
   *
   * <p>After unregistration, the listener will no longer receive stop place
   * change events. If the listener was not previously registered, this method
   * has no effect.</p>
   *
   * <p>It is important to unregister listeners when they are no longer needed
   * to prevent memory leaks and unnecessary processing.</p>
   *
   * @param listener the listener to unregister, must not be {@code null}
   * @throws IllegalArgumentException if listener is {@code null}
   * @see #registerStopPlaceChangelogListener(StopPlaceChangelogListener)
   */
  void unregisterStopPlaceChangelogListener(
    StopPlaceChangelogListener listener
  );
}
