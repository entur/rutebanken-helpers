package org.entur.oauth2;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Objects;

/**
 * Validate the audience in the JWT token.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final List<String> audiences;

    public AudienceValidator(String audience) {
        this(List.of(audience));
    }

    public AudienceValidator(List<String> audiences) {
        Objects.requireNonNull(audiences);
        this.audiences = audiences;
    }

    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (audiences.stream().anyMatch(jwt.getAudience()::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
