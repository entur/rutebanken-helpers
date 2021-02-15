package org.entur.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Extract the roles from the JWT token and convert them into Spring Security Authorities.
 */
public class JwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        for (String authority : getAuthorities(jwt)) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + authority));
        }
        return grantedAuthorities;
    }

    private Collection<String> getAuthorities(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess instanceof Map) {
            Object authorities = ((Map<Object, Object>) realmAccess).get("roles");
            if (authorities instanceof String) {
                if (StringUtils.hasText((String) authorities)) {
                    return Arrays.asList(((String) authorities).split(" "));
                } else {
                    return Collections.emptyList();
                }
            } else if (authorities instanceof Collection) {
                return (Collection<String>) authorities;
            }
        }
        return Collections.emptyList();
    }
}
