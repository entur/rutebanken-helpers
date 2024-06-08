package org.entur.oauth2.user;

import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Retrieve user information from the JWT token.
 */
public class JwtUserInfoExtractor implements UserInfoExtractor {

    @Override
    public String getPreferredName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) auth;
        Jwt jwt = (Jwt) jwtAuthenticationToken.getPrincipal();
        return jwt.getClaimAsString("https://ror.entur.io/preferred_name");

    }

}
