package org.entur.oauth2;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Build a WebClient for authorized API calls.
 * The WebClient inserts a JWT bearer token in the Authorization HTTP header.
 * The JWT token is obtained from the configured Authorization Server.
 */
public class AuthorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder {

    private OAuth2ClientProperties properties;
    private String audience;

    public AuthorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder withOAuth2ClientProperties(OAuth2ClientProperties properties) {
        this.properties = properties;
        return this;
    }

    public AuthorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder withAudience(String audience) {
        this.audience = audience;
        return this;
    }

    public AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager build() {


        ReactiveClientRegistrationRepository clientRegistrations = clientRegistrationRepository();
        ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrations);

        // the failure handler ensures that the authorized client is removed if an authorization error occurs. A new token will then be requested.
        // this makes it possible to replace an expired token with a new one.
        ReactiveOAuth2AuthorizationFailureHandler authorizationFailureHandler =
                new RemoveAuthorizedClientReactiveOAuth2AuthorizationFailureHandler(
                        (clientRegistrationId, principal, attributes) ->
                                reactiveOAuth2AuthorizedClientService.removeAuthorizedClient(clientRegistrationId, principal.getName()));

        WebClientReactiveClientCredentialsTokenResponseClient webClientReactiveClientCredentialsTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();
        webClientReactiveClientCredentialsTokenResponseClient.setWebClient(webClientForTokenRequest());

        ClientCredentialsReactiveOAuth2AuthorizedClientProvider reactiveOAuth2AuthorizedClientProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();
        reactiveOAuth2AuthorizedClientProvider.setAccessTokenResponseClient(webClientReactiveClientCredentialsTokenResponseClient);

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientServiceReactiveOAuth2AuthorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations, reactiveOAuth2AuthorizedClientService);
        authorizedClientServiceReactiveOAuth2AuthorizedClientManager.setAuthorizationFailureHandler(authorizationFailureHandler);
        authorizedClientServiceReactiveOAuth2AuthorizedClientManager.setAuthorizedClientProvider(reactiveOAuth2AuthorizedClientProvider);

        return authorizedClientServiceReactiveOAuth2AuthorizedClientManager;
    }

    /**
     * Return the repository of OAuth2 clients.
     * In a reactive Spring Boot application this bean would be auto-configured.
     * For a servlet-based (not reactive) application, the bean must be created manually.
     *
     * @return the repository of OAuth2 clients.
     */
    private ReactiveClientRegistrationRepository clientRegistrationRepository() {
        List<ClientRegistration> registrations = new ArrayList<>(
                OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());
        return new InMemoryReactiveClientRegistrationRepository(registrations);
    }

    /**
     * Return a WebClient for requesting a token to the Authorization Server.
     * Auth0 requires that the form data in the body include an "audience" parameter in addition to the standard
     * "grant_type" parameter.
     *
     * @return a WebClient instance that can be used for requesting a token to the Authorization Server.
     */
    private WebClient webClientForTokenRequest() {

        // The exchange filter adds the 2 required parameters in the request body.
        ExchangeFilterFunction tokenRequestFilter = ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            ClientRequest.Builder builder = ClientRequest.from(clientRequest);
            LinkedMultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "client_credentials");
            formData.add("audience", audience);
            builder.body(BodyInserters.fromFormData(formData));
            return Mono.just(builder.build());
        });

        return WebClient.builder()
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.add(tokenRequestFilter))
                .build();
    }


}


