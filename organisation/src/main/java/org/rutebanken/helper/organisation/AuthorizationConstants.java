/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package org.rutebanken.helper.organisation;

public final class AuthorizationConstants {

  private AuthorizationConstants() {}

  public static final String ROLE_DELETE_STOPS = "deleteStops";
  public static final String ROLE_EDIT_STOPS = "editStops";

  /**
   * Administrator rights for all organisations in the organisation register.
   */
  public static final String ROLE_ORGANISATION_EDIT = "editOrganisation";

  /**
   * Editor rights for route data for a single provider.
   */
  public static final String ROLE_ROUTE_DATA_EDIT = "editRouteData";

  /**
   * Viewer rights for route data and configuration for all providers.
   */

  public static final String ROLE_ROUTE_DATA_VIEW_ALL = "viewAllRouteData";

  /**
   * Administrator rights for route data and configuration for all providers.
   */
  public static final String ROLE_ROUTE_DATA_ADMIN = "adminEditRouteData";

  /**
   * Viewer rights for NeTEx Blocks for a single provider.
   */

  public static final String ROLE_NETEX_BLOCKS_DATA_VIEW =
    "viewNetexBlocksData";

  public static final String ENTITY_CLASSIFIER_ALL_TYPES = "*";
  public static final String ENTITY_TYPE = "EntityType";

  public static final String ENTITY_CLASSIFIER_ALL_ATTRIBUTES = "*";
}
