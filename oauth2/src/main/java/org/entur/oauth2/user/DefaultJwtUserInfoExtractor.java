package org.entur.oauth2.user;

import static org.entur.oauth2.user.support.AuthenticationUtil.getClaim;

import javax.annotation.Nullable;
import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;

/**
 * Default UserInfoExtractor that retrieves the username from the standard OIDC claim "preferred_username".
 * The preferred name is equal to the preferred username.
 */
public class DefaultJwtUserInfoExtractor implements UserInfoExtractor {

  @Nullable
  @Override
  public String getPreferredName() {
    return getPreferredUsername();
  }

  @Nullable
  @Override
  public String getPreferredUsername() {
    return getClaim(StandardClaimNames.PREFERRED_USERNAME);
  }
}
