package org.entur.oauth2.user;

/**
 * Retrieve user information from the JWT token.
 * User details are extracted from Entur-custom claims (non-standard claims).
 * @deprecated Use {@link EnturJwtUserInfoExtractor} to use Entur-custom claims or {@link DefaultJwtUserInfoExtractor}
 * to use standard OIDC claims.
 */
@Deprecated
public class JwtUserInfoExtractor extends EnturJwtUserInfoExtractor {}
