# Permission Store Proxy

A Java library that provides OAuth2-based authentication and authorization integration with the Entur organization repository (Baba).

## Overview

This module acts as a proxy layer between Spring Security OAuth2 authentication and the Entur Baba user/organization management system. It extracts authenticated user information from JWT tokens and retrieves detailed role assignments and user information from the Baba API.

## Features

- **JWT Token Processing**: Extract authenticated user information from OAuth2 JWT tokens
- **Role Assignment Retrieval**: Fetch user role assignments from Baba API
- **User Information Extraction**: Retrieve user profile and contact details
- **Multi-Issuer Support**: Support for Internal, Partner, and legacy RoR authentication issuers
- **Performance Optimization**: Built-in caching (10s TTL) to reduce API load during burst requests
- **Resilient HTTP Calls**: Automatic retry with exponential backoff for transient failures
- **Machine-to-Machine Support**: Handle both end-user and M2M client authentication

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>permission-store-proxy</artifactId>
    <version>${rutebanken-helpers.version}</version>
</dependency>
```

## Usage

### Role Assignment Extraction

```java
import org.entur.ror.permission.RemoteBabaRoleAssignmentExtractor;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.web.reactive.function.client.WebClient;

// Create an authorized WebClient
WebClient webClient = WebClient.builder()
    .baseUrl("https://baba-api-url")
    .defaultHeader("Authorization", "Bearer " + token)
    .build();

// Create the role assignment extractor
RoleAssignmentExtractor extractor = new RemoteBabaRoleAssignmentExtractor(
    webClient,
    "https://baba-api-url"
);

// Extract role assignments from authentication
List<RoleAssignment> roles = extractor.getRoleAssignmentsForUser(authentication);
```

### User Information Extraction

```java
import org.entur.ror.permission.RemoteBabaUserInfoExtractor;
import org.rutebanken.helper.organisation.user.UserInfoExtractor;

// Create the user info extractor
UserInfoExtractor userInfoExtractor = new RemoteBabaUserInfoExtractor(
    webClient,
    "https://baba-api-url"
);

// Get user display information
String preferredName = userInfoExtractor.getPreferredName();
String username = userInfoExtractor.getPreferredUsername();
```

### Working with AuthenticatedUser

```java
import org.entur.ror.permission.AuthenticatedUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

// Extract authenticated user from JWT token
AuthenticatedUser user = AuthenticatedUser.of(jwtAuthenticationToken);

// Check user type
if (user.isClient()) {
    // Handle M2M client
}

if (user.isInternal()) {
    // Handle Entur internal user
}

if (user.isPartner()) {
    // Handle external partner user
}

// Access user details
String subject = user.subject();
long orgId = user.organisationId();
```

## Configuration

### Required JWT Claims

Your JWT tokens must include:

- **`sub`** (subject): User or client identifier
- **`iss`** (issuer): Authentication issuer URL
- **`https://entur.io/organisationID`**: Organisation ID (required for Internal/Partner users)

### Supported Issuers

#### Internal (Entur internal machine-to-machine clients)
- Production: `https://internal.entur.org/`
- Staging: `https://internal.staging.entur.org/`
- Development: `https://internal.dev.entur.org/`

#### Partner (External Partners)
- Production: `https://partner.entur.org/`
- Staging: `https://partner.staging.entur.org/`
- Development: `https://partner.dev.entur.org/`

#### RoR (Legacy - To Be Deprecated)
- Production: `https://auth2.entur.org/`
- Staging: `https://ror-entur-staging.eu.auth0.com/`
- Development: `https://ror-entur-dev.eu.auth0.com/`

### Baba API Endpoints

The module calls the following Baba API endpoints:

- **POST** `/roleAssignments` - Retrieve user role assignments
- **POST** `/authenticatedUser` - Retrieve user information

## Architecture

### Components

- **`AuthenticatedUser`**: Immutable representation of an authenticated OAuth2 user or M2M client
- **`BabaRoleAssignmentExtractor`**: Abstract base class for role assignment extraction
- **`RemoteBabaRoleAssignmentExtractor`**: Retrieves role assignments via Baba REST API
- **`RemoteBabaUserInfoExtractor`**: Retrieves user information via Baba REST API
- **`BabaUser`**: Represents user account details from Baba
- **`BabaContactDetails`**: User contact information

### Caching Strategy

Both extractors implement short-lived caching using Caffeine:

- **Cache Duration**: 10 seconds
- **Eviction Policy**: Time-based (write-time)
- **Purpose**: Reduce load during burst requests (page refreshes, @PostFilter operations)

### Error Handling

- **5xx Errors**: Automatic retry with exponential backoff (3 attempts, 1s initial delay)
- **4xx Errors**: Immediate failure with detailed logging
- **Invalid Tokens**: Throws `AccessDeniedException`
- **Missing Required Claims**: Throws `IllegalArgumentException`

## Dependencies

This module depends on:

- **organisation**: Role assignment and user info interfaces
- **oauth2**: OAuth2 helper utilities
- **Spring WebFlux**: Reactive WebClient for HTTP calls
- **Caffeine**: High-performance caching library
- **Spring Security OAuth2**: JWT token processing

## Development

### Building

```bash
mvn clean install
```

### Running Tests

```bash
mvn test
```

## Migration Notes

This module is undergoing migration to a new permission store architecture. Current migration items:

- Removal of legacy `permissions` JWT claim
- Removal of legacy `preferred_username` JWT claim
- Deprecation of RoR issuer support
- Removal of debug logging for 4xx errors

## License

Licensed under the EUPL, Version 1.2 or later. See [LICENSE](https://joinup.ec.europa.eu/software/page/eupl) for details.

## Support

For issues and questions, please refer to the [rutebanken-helpers](https://github.com/entur/rutebanken-helpers) repository.

## Related Modules

- **[organisation](../organisation)**: Core organization and role assignment interfaces
- **[oauth2](../oauth2)**: OAuth2 helper utilities
