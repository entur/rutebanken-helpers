# permission-store-proxy

## Overview

The `permission-store-proxy` module is a Java library that provides OAuth2-based authentication and authorization integration with the Entur organization repository (Baba). It extracts role assignments and user information from authenticated JWT tokens and retrieves detailed permissions from the Baba service.

## Purpose

This module serves as a proxy/adapter layer between Spring Security OAuth2 authentication and the Entur Baba user/organization management system. It enables applications to:

1. Extract authenticated user information from JWT tokens
2. Retrieve role assignments for users from the Baba API
3. Cache user information and role assignments for performance
4. Support multiple authentication issuers (Internal, Partner, RoR legacy)

## Key Components

### Authentication Models

- **`AuthenticatedUser`**: Represents an OAuth2 authenticated user or M2M client, extracted from JWT tokens
  - Supports multiple issuers (Internal, Partner, RoR legacy)
  - Includes subject, organisation ID, permissions, issuer, and username
  - Distinguishes between end users and machine-to-machine clients
  - Provides DTO for serialization

- **`BabaUser`**: Details about a user account in Baba database or M2M client
  - Contains username, client flag, and contact details

- **`BabaContactDetails`**: Contact information for users

### Role Assignment Extractors

- **`BabaRoleAssignmentExtractor`**: Abstract base class implementing `RoleAssignmentExtractor`
  - Validates JWT authentication
  - Delegates to concrete implementations for actual role retrieval

- **`RemoteBabaRoleAssignmentExtractor`**: Retrieves role assignments via Baba REST API
  - Uses Spring WebClient for HTTP calls
  - Implements 10-second caching (Caffeine) to reduce API load during burst requests
  - Retry logic with exponential backoff for 5xx errors (3 attempts)
  - POST to `/roleAssignments` endpoint with authenticated user details

### User Information Extractors

- **`RemoteBabaUserInfoExtractor`**: Implements `UserInfoExtractor` interface
  - Retrieves user information from Baba API
  - 10-second cache using Caffeine
  - POST to `/authenticatedUser` endpoint
  - Provides preferred name and username for display

## Architecture

### Dependency Graph

```
permission-store-proxy
├── organisation (sibling module)
├── oauth2 (sibling module)
├── caffeine (caching)
└── Spring WebFlux (reactive WebClient)
```

### Authentication Flow

1. Spring Security validates JWT token
2. `AuthenticatedUser.of()` extracts claims from `JwtAuthenticationToken`
3. Role/user info extractors use `AuthenticatedUser` to query Baba API
4. Results are cached for 10 seconds to optimize repeated requests
5. Applications use role assignments for authorization decisions

## Configuration

### JWT Token Claims

The module expects the following JWT claims:

- **Standard claims**: `sub` (subject), `iss` (issuer)
- **Custom claims**:
  - `https://entur.io/organisationID`: Organisation ID (required for Internal/Partner users)
  - `permissions`: List of permission strings (legacy)
  - `https://ror.entur.io/preferred_username`: Username (required for RoR users)

### Supported Issuers

**Internal (Entur employees)**:
- `https://internal.dev.entur.org/`
- `https://internal.staging.entur.org/`
- `https://internal.entur.org/`

**Partner (External partners)**:
- `https://partner.dev.entur.org/`
- `https://partner.staging.entur.org/`
- `https://partner.entur.org/`

**RoR (Legacy, to be deprecated)**:
- `https://ror-entur-dev.eu.auth0.com/`
- `https://ror-entur-staging.eu.auth0.com/`
- `https://auth2.entur.org/`

## Usage

### Setting up Role Assignment Extractor

```java
WebClient authorizedWebClient = WebClient.builder()
    .baseUrl("https://baba-api-url")
    .defaultHeader("Authorization", "Bearer " + token)
    .build();

RoleAssignmentExtractor extractor = new RemoteBabaRoleAssignmentExtractor(
    authorizedWebClient,
    "https://baba-api-url"
);

// Use with Spring Security
List<RoleAssignment> roles = extractor.getRoleAssignmentsForUser(authentication);
```

### Setting up User Info Extractor

```java
UserInfoExtractor userInfoExtractor = new RemoteBabaUserInfoExtractor(
    authorizedWebClient,
    "https://baba-api-url"
);

String preferredName = userInfoExtractor.getPreferredName();
String username = userInfoExtractor.getPreferredUsername();
```

## Caching Strategy

Both extractors use Caffeine cache with:
- **TTL**: 10 seconds
- **Eviction**: Write-time based
- **Rationale**: Reduces load during burst requests (e.g., page refresh, @PostFilter operations)

## Error Handling

### Retry Logic
- HTTP 5xx errors trigger exponential backoff retry (3 attempts, 1s initial delay)
- HTTP 4xx errors are logged with payload details and thrown immediately

### Validation
- Missing subject or issuer throws `NullPointerException`
- Missing organisation ID for Internal/Partner users throws `IllegalArgumentException`
- Missing username for RoR users throws `IllegalArgumentException`
- Non-JWT authentication throws `AccessDeniedException`

## Migration Notes

The codebase includes several `TODO Permission Store migration` comments indicating ongoing migration work:
- Legacy `permissions` claim will be removed
- Legacy `preferred_username` claim will be removed
- RoR issuer support will be removed
- Debug logging for 4xx errors will be removed

## Testing

The module includes unit tests:
- `AuthenticatedUserTest`: Tests authenticated user creation and validation

## License

Licensed under EUPL v1.2 - See pom.xml for details.

## Related Modules

- **organisation**: Defines `RoleAssignment` and `RoleAssignmentExtractor` interfaces
- **oauth2**: OAuth2 helper utilities
