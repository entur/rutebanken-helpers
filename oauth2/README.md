# OAuth2 Helper Library

OAuth2 helper library for Spring Boot applications, providing simplified OAuth2/JWT authentication and authorization functionality.

## Overview

This module provides utilities and builders for integrating OAuth2 authentication and authorization in Spring Boot applications. It supports both resource server (JWT validation) and client (authorized API calls) configurations, with special support for Auth0 and multi-issuer scenarios.

## Features

- **JWT Token Validation**: Simplified JWT decoder configuration with audience validation
- **Authorized WebClient**: Build WebClient instances with automatic OAuth2 token management
- **Multi-Issuer Support**: Handle JWT tokens from multiple OAuth2 providers
- **Custom Claims Support**: Extract user information and roles from standard and custom JWT claims
- **Auth0 Integration**: Built-in support for Auth0-specific claim structures
- **Role and Permission Mapping**: Convert JWT claims to Spring Security authorities

## Dependencies

Add this dependency to your Maven project:

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>oauth2</artifactId>
    <version>${rutebanken-helpers.version}</version>
</dependency>
```

## Main Components

### Resource Server (JWT Validation)

#### MultiIssuerAuthenticationManagerResolverBuilder
Support multiple JWT issuers in a single application:

```java
MultiIssuerAuthenticationManagerResolver resolver = 
    new MultiIssuerAuthenticationManagerResolverBuilder()
        .withEnturInternalAuth0Issuer("https://internal.auth0.com/")
        .withEnturInternalAuth0Audience("internal-api")
        .withEnturPartnerAuth0Issuer("https://partner.auth0.com/")
        .withEnturPartnerAuth0Audience("partner-api")
        .build();
```

#### AudienceValidator
Validate that JWT tokens contain the expected audience claim:

```java
OAuth2TokenValidator<Jwt> audienceValidator = 
    new AudienceValidator("your-api-audience");
```

### OAuth2 Client (Authorized API Calls)

#### AuthorizedWebClientBuilder
Build a WebClient that automatically adds OAuth2 bearer tokens to outgoing requests:

```java
WebClient webClient = new AuthorizedWebClientBuilder(WebClient.builder())
    .withProperties(oauth2ClientProperties)
    .withAudience("target-api-audience")
    .withClientRegistrationId("my-client")
    .build();
```

#### TokenService
Retrieve OAuth2 bearer tokens programmatically (useful for non-Spring integrations like Apache Camel):

```java
public interface TokenService {
    String getToken();
}
```

Implementation:
```java
TokenService tokenService = new OAuth2TokenService(authorizedClientManager, clientRegistration);
String token = tokenService.getToken();
```

### User Information and Authorities

#### DefaultJwtUserInfoExtractor
Extract user information from standard OIDC JWT claims:

```java
DefaultJwtUserInfoExtractor extractor = new DefaultJwtUserInfoExtractor();
String preferredName = extractor.getPreferredName();
```

## Configuration

### Application Properties Example

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://your-auth-server.com/
      client:
        registration:
          my-client:
            client-id: your-client-id
            client-secret: your-client-secret
            authorization-grant-type: client_credentials
            scope: api:read,api:write
        provider:
          my-provider:
            token-uri: https://your-auth-server.com/oauth/token
```

## Custom OAuth2 Claims

The library uses standard OAuth2 claim names:

- `roles`: User roles
- `role_assignments`: Detailed role assignments with organization context
- `permissions`: User permissions

## Usage Examples

### Configure Resource Server Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        MultiIssuerAuthenticationManagerResolver resolver = 
            new MultiIssuerAuthenticationManagerResolverBuilder()
                .withEnturInternalAuth0Issuer("https://auth.example.com/")
                .withEnturInternalAuth0Audience("my-api")
                .build();
            
        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/public/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationManagerResolver(resolver)
            );
            
        return http.build();
    }
}
```

### Make Authorized API Calls

```java
@Service
public class ExternalApiClient {
    
    private final WebClient webClient;
    
    public ExternalApiClient(
            WebClient.Builder webClientBuilder,
            OAuth2ClientProperties oauth2ClientProperties) {
        this.webClient = new AuthorizedWebClientBuilder(webClientBuilder)
            .withProperties(oauth2ClientProperties)
            .withClientRegistrationId("external-api")
            .withAudience("https://api.example.com")
            .build();
    }
    
    public Mono<String> callExternalApi() {
        return webClient.get()
            .uri("https://api.example.com/data")
            .retrieve()
            .bodyToMono(String.class);
    }
}
```

## License

Licensed under the EUPL, Version 1.2 or later.

## Links

- [GitHub Repository](https://github.com/entur/rutebanken-helpers)
- [Parent Project README](../README.md)
