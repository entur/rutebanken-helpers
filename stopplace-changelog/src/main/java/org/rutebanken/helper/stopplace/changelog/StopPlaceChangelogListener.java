package org.rutebanken.helper.stopplace.changelog;

import java.io.InputStream;

/**
 * Listener interface for receiving stop place change events.
 *
 * <p>This interface defines callback methods that are invoked when stop place
 * lifecycle events occur. Implementations should handle these events according
 * to their application's requirements. All methods have default implementations
 * that do nothing, allowing implementers to only override the events they care about.</p>
 *
 * <p>The stop place data provided in the callbacks is in NeTEx format as an XML
 * stream. For deletion events, no stop place data is provided since the stop place
 * is no longer available.</p>
 *
 * <h3>Event Types:</h3>
 * <ul>
 *   <li><strong>Created:</strong> A new stop place has been created</li>
 *   <li><strong>Updated:</strong> An existing stop place has been modified</li>
 *   <li><strong>Deactivated:</strong> A stop place has been soft-deleted (REMOVE event)</li>
 *   <li><strong>Deleted:</strong> A stop place has been permanently removed (DELETE event)</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Component
 * public class MyStopPlaceHandler implements StopPlaceChangelogListener {
 *
 *     private static final Logger log = LoggerFactory.getLogger(MyStopPlaceHandler.class);
 *
 *     @Override
 *     public void onStopPlaceCreated(String id, InputStream stopPlace) {
 *         log.info("Processing new stop place: {}", id);
 *         // Parse and process the NeTEx data
 *         processStopPlaceData(id, stopPlace);
 *     }
 *
 *     @Override
 *     public void onStopPlaceDeleted(String id) {
 *         log.info("Removing stop place from local cache: {}", id);
 *         removeFromCache(id);
 *     }
 *
 *     private void processStopPlaceData(String id, InputStream netexData) {
 *         // Implementation for processing NeTEx XML data
 *     }
 * }
 * }</pre>
 *
 * <h3>Threading and Error Handling:</h3>
 * <p>Listener methods may be called from background threads. Implementations should be
 * thread-safe if they access shared state. Any exceptions thrown from listener methods
 * will be logged but will not stop the event processing pipeline.</p>
 *
 * @see StopPlaceChangelog
 * @see org.rutebanken.irkalla.avro.StopPlaceChangelogEvent
 * @since 5.41.0
 * @author Entur
 */
public interface StopPlaceChangelogListener {
  /**
   * Called when a new stop place has been created.
   *
   * <p>This method is invoked when a CREATE event is received, indicating that
   * a new stop place has been added to the system. The stop place data contains
   * the complete NeTEx representation of the newly created stop place.</p>
   *
   * @param id the NeTEx ID of the created stop place, never {@code null}
   * @param stopPlace the complete stop place data in NeTEx XML format, never {@code null}
   *                  The stream should be consumed completely and closed by the implementation
   * @see #onStopPlaceUpdated(String, InputStream)
   */
  default void onStopPlaceCreated(String id, InputStream stopPlace) {}

  /**
   * Called when an existing stop place has been updated.
   *
   * <p>This method is invoked when an UPDATE event is received, indicating that
   * an existing stop place has been modified. The stop place data contains
   * the complete updated NeTEx representation of the stop place.</p>
   *
   * @param id the NeTEx ID of the updated stop place, never {@code null}
   * @param stopPlace the complete updated stop place data in NeTEx XML format, never {@code null}
   *                  The stream should be consumed completely and closed by the implementation
   * @see #onStopPlaceCreated(String, InputStream)
   */
  default void onStopPlaceUpdated(String id, InputStream stopPlace) {}

  /**
   * Called when a stop place has been deactivated (soft deleted).
   *
   * <p>This method is invoked when a REMOVE event is received, indicating that
   * a stop place has been deactivated or soft-deleted. The stop place still exists
   * in the system but is no longer active. The stop place data contains the
   * final state before deactivation.</p>
   *
   * @param id the NeTEx ID of the deactivated stop place, never {@code null}
   * @param stopPlace the stop place data in its final state before deactivation,
   *                  in NeTEx XML format, never {@code null}
   *                  The stream should be consumed completely and closed by the implementation
   * @see #onStopPlaceDeleted(String)
   */
  default void onStopPlaceDeactivated(String id, InputStream stopPlace) {}

  /**
   * Called when a stop place has been permanently deleted.
   *
   * <p>This method is invoked when a DELETE event is received, indicating that
   * a stop place has been permanently removed from the system. No stop place
   * data is provided since the stop place no longer exists.</p>
   *
   * @param id the NeTEx ID of the deleted stop place, never {@code null}
   * @see #onStopPlaceDeactivated(String, InputStream)
   */
  default void onStopPlaceDeleted(String id) {}
}
