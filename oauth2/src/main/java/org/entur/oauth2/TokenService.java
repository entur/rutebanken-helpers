package org.entur.oauth2;

/**
 * Retrieve an OAuth2 bearer token from an Authorization Server.
 * The token can then be used to add an Authorization header in an HTTP request.
 * This is intended mainly for Camel applications.
 * Alternatively for Spring Boot applications, the {@link AuthorizedWebClientBuilder} automates completely the process of retrieving
 * the token and adding it in the HTTP Authorization header.
 */
public interface TokenService {

    String getToken();
}
