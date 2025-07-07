package org.rutebanken.helper.organisation.user;

import javax.annotation.Nullable;

/**
 * Retrieve user information of the current user.
 */
public interface UserInfoExtractor {
  /**
   * Return the preferred name of the current user, or null if no user is authenticated.
   */
  @Nullable
  String getPreferredName();

  /**
   * Return the preferred username of the current user, or null if no user is authenticated.
   */
  @Nullable
  String getPreferredUsername();
}
