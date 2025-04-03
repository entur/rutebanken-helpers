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

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public interface RoleAssignmentExtractor {
  /**
   * Extract role assignments for user from security context.
   */
  default List<RoleAssignment> getRoleAssignmentsForUser() {
    Authentication auth = SecurityContextHolder
      .getContext()
      .getAuthentication();
    return getRoleAssignmentsForUser(auth);
  }

  List<RoleAssignment> getRoleAssignmentsForUser(Authentication authentication);
}
