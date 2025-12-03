# OAuth2 Module - Developer Guide

## Overview

The `oauth2` module is a helper library for Spring Boot applications that provides OAuth2/JWT authentication and authorization functionality. It is designed specifically to work with Auth0 and supports multi-issuer JWT validation for Entur's authentication infrastructure.

## Package Structure

```
org.entur.oauth2
├── Core Authentication & Authorization
│   └── AudienceValidator.java
│
├── Multi-Issuer Support
│   ├── multiissuer/MultiIssuerAuthenticationManagerResolver.java
│   └── multiissuer/MultiIssuerAuthenticationManagerResolverBuilder.java
│
├── Authorized HTTP Clients
│   ├── AuthorizedWebClientBuilder.java
│   ├── TokenService.java
│   ├── OAuth2TokenService.java
│   └── AuthorizedClientServiceReactiveOAuth2AuthorizedClientManagerBuilder.java
│
└── User Information Extraction
    ├── user/DefaultJwtUserInfoExtractor.java
    └── user/support/AuthenticationUtil.java
```

## Key Components

### 1. JWT Validation and Decoding

**AudienceValidator**
- Validates JWT audience claims
- Supports single or multiple audiences
- Returns OAuth2TokenValidatorResult

### 2. Multi-Issuer Authentication

**MultiIssuerAuthenticationManagerResolver**
- Supports OAuth2 issuers:
  - Entur Internal Auth0 tenant
  - Entur Partner Auth0 tenant
- Lazily instantiates and caches AuthenticationManagers
- Extracts issuer from JWT token and routes to appropriate handler
- Rejects tokens from unconfigured issuers

**Usage Pattern:**
The resolver extracts the issuer from incoming JWT tokens and dynamically selects the appropriate authentication manager. Each manager is configured with specific audience, issuer, and claim namespace settings.

### 3. Authorities and Roles

The library uses Spring Security's standard JWT role extraction from the "roles" claim.
Roles are automatically prefixed with `ROLE_` for Spring Security authorization.

### 4. Authorized HTTP Clients

**AuthorizedWebClientBuilder**
- Builds WebClient instances with automatic OAuth2 token injection
- Handles token refresh on expiration
- Configures client credentials flow for Auth0
- Adds required "audience" parameter for Auth0 compatibility

**Usage:**
```java
WebClient client = new AuthorizedWebClientBuilder(webClientBuilder)
    .withOAuth2ClientProperties(oAuth2Properties)
    .withAudience("https://api.example.com")
    .withClientRegistrationId("my-client")
    .build();
```

**TokenService / OAuth2TokenService**
- Alternative for Camel applications
- Retrieves OAuth2 bearer tokens directly
- Tokens can be manually added to HTTP Authorization headers

### 5. User Information Extraction

**DefaultJwtUserInfoExtractor**
- Extracts user information from standard OIDC claims
- Follows OpenID Connect specifications

## Dependencies

- Spring Boot Starter WebFlux
- Spring Boot Starter Security
- Spring Security OAuth2 Resource Server
- Spring Security OAuth2 JOSE
- Spring Security OAuth2 Client
- Organisation module (internal dependency)

## Configuration

The module expects OAuth2 client configuration via Spring Boot properties:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          my-client:
            client-id: ${CLIENT_ID}
            client-secret: ${CLIENT_SECRET}
            authorization-grant-type: client_credentials
        provider:
          my-provider:
            token-uri: https://auth.example.com/oauth/token
```

Additional Entur-specific configuration:
- Entur Internal Auth0: audience, issuer
- Entur Partner Auth0: audience, issuer

## Integration Patterns

### Resource Server (API)
1. Use `MultiIssuerAuthenticationManagerResolver` for APIs accepting tokens from multiple Auth0 tenants
2. Configure with appropriate issuers and audiences
3. Roles are automatically extracted and converted to Spring Security authorities

### Client (Service-to-Service)
1. Use `AuthorizedWebClientBuilder` for reactive applications
2. Use `TokenService` for imperative/Camel applications
3. Tokens are automatically obtained and refreshed

## Testing

Test classes:
- `DefaultJwtUserInfoExtractorTest.java`
- `MultiIssuerAuthenticationManagerResolverTest.java`

## Common Use Cases

1. **Multi-tenant API authentication**: Use `MultiIssuerAuthenticationManagerResolver` to accept JWTs from different Auth0 tenants
2. **Service-to-service communication**: Use `AuthorizedWebClientBuilder` to automatically handle OAuth2 token lifecycle
3. **Role-based authorization**: Roles are extracted from JWT and available via Spring Security's role checking
4. **Custom claim handling**: Auth0 namespace claims are normalized and made accessible

## Notes

- The module is designed for Auth0 but can work with other OAuth2 providers with adjustments
- Audience validation is mandatory
- Token caching and refresh is handled automatically by Spring Security
- The `ror_` prefix handling is specific to Entur Partner tenant configuration
