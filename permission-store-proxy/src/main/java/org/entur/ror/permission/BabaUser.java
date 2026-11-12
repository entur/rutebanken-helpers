package org.entur.ror.permission;

import org.jspecify.annotations.Nullable;

/**
 * Details about an authenticated user, either a user account in the Baba database or a machine-to-machine client.
 */

public class BabaUser {

  public String username;
  public boolean isClient;

  @Nullable
  public BabaContactDetails contactDetails;
}
