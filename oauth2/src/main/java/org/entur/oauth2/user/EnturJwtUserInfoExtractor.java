package org.entur.oauth2.user;

import static org.entur.oauth2.user.support.AuthenticationUtil.getClaim;

import javax.annotation.Nullable;
import org.rutebanken.helper.organisation.user.UserInfoExtractor;

/**
 * Retrieve user information from the JWT token.
 * User details are extracted from Entur-custom claims (non-standard claims).
 * TODO Permission Store migration: Obsolete after the migration to Entur Partner tenant.
 */
public class EnturJwtUserInfoExtractor implements UserInfoExtractor {

  private static final String CLAIM_ROR_PREFERRED_NAME =
    "https://ror.entur.io/preferred_name";
  private static final String CLAIM_ROR_PREFERRED_USERNAME =
    "https://ror.entur.io/preferred_username";

  @Override
  @Nullable
  public String getPreferredName() {
    return getClaim(CLAIM_ROR_PREFERRED_NAME);
  }

  @Override
  @Nullable
  public String getPreferredUsername() {
    return getClaim(CLAIM_ROR_PREFERRED_USERNAME);
  }
}
