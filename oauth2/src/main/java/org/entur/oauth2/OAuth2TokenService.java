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

package org.entur.oauth2;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

/**
 * Token service that uses Spring Security OAuth2 for retrieving the access token.
 * Requires that OAuth2 client configuration is set in the properties spring.security.oauth2.client.registration.* .
 */
public class OAuth2TokenService implements TokenService {

    private String clientId;
    private String clientRegistrationId;
    private AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientServiceReactiveOAuth2AuthorizedClientManager;

    @Override
    public String getToken() {
        OAuth2AuthorizeRequest oAuth2AuthorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId).principal(clientId).build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientServiceReactiveOAuth2AuthorizedClientManager.authorize(oAuth2AuthorizeRequest).block();
        return authorizedClient.getAccessToken().getTokenValue();
    }

    public static class Builder {

        private OAuth2TokenService oAuth2TokenService;
        private OAuth2ClientProperties properties;
        private String audience;

        public Builder() {
            oAuth2TokenService = new OAuth2TokenService();
        }

        public OAuth2TokenService build() {
            AuthorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder authorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder();
            authorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder.withOAuth2ClientProperties(properties);
            authorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder.withAudience(audience);
            oAuth2TokenService.clientId = properties.getRegistration().get(this.oAuth2TokenService.clientRegistrationId).getClientId();
            oAuth2TokenService.authorizedClientServiceReactiveOAuth2AuthorizedClientManager = authorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder.build();
            return oAuth2TokenService;
        }

        public OAuth2TokenService.Builder withClientRegistrationId(String clientRegistrationId) {
            this.oAuth2TokenService.clientRegistrationId = clientRegistrationId;
            return this;
        }

        public OAuth2TokenService.Builder withOAuth2ClientProperties(OAuth2ClientProperties properties) {
            this.properties = properties;
            return this;
        }

        public OAuth2TokenService.Builder withAudience(String audience) {
            this.audience = audience;
            return this;
        }
    }

}
