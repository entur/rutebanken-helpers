# Organisation Helper Library

A Java library for managing role-based authorization and access control in Entur RoR applications.

## Overview

The Organisation module provides a comprehensive framework for managing user permissions, role assignments, and access control based on organizational boundaries and administrative zones. It integrates with Spring Security and JWT tokens to enforce fine-grained authorization policies.

## Features

- **JWT Role Extraction**: Parse and extract role assignments from JWT tokens
- **Fine-grained Authorization**: Control access based on roles, organizations, and administrative zones
- **Provider-scoped Access**: Manage permissions per data provider
- **Entity-level Permissions**: Control access to specific entity types and classifications
- **Spring Security Integration**: Seamless integration with Spring Security framework
- **Reflection-based Validation**: Automatic validation of data access using object introspection

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>organisation</artifactId>
    <version>5.49.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### 1. Configure Authorization Service

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public AuthorizationService<String> authorizationService(
            RoleAssignmentExtractor roleAssignmentExtractor) {
        return new DefaultAuthorizationService(roleAssignmentExtractor);
    }
}
```

### 2. Use in Your Service

```java
@Service
public class RouteDataService {
    
    @Autowired
    private AuthorizationService<String> authorizationService;
    
    public void updateRouteData(String providerId, RouteData data) {
        // Validate user has permission to edit this provider's data
        authorizationService.validateEditRouteData(providerId);
        
        // Proceed with update
        // ...
    }
    
    public List<RouteData> getRouteData(String providerId) {
        // Check if user can view this provider's data
        if (!authorizationService.canViewRouteData(providerId)) {
            throw new AccessDeniedException("Cannot view route data for provider: " + providerId);
        }
        
        // Return data
        // ...
    }
}
```

### 3. Using Data-Scoped Authorization

```java
@Service
public class StopPlaceService {
    
    @Autowired
    private DataScopedAuthorizationService authorizationService;
    
    @Autowired
    private EntityResolver entityResolver;
    
    public void updateStopPlace(StopPlace stopPlace) {
        // Automatically validates organization, admin zones, and entity types
        authorizationService.assertAuthorized(
            AuthorizationConstants.ROLE_EDIT_STOPS, 
            stopPlace
        );
        
        // Proceed with update
        // ...
    }
}
```

## Role Assignment Structure

Role assignments are embedded in JWT tokens with a compact structure:

```json
{
  "r": "editRouteData",
  "o": "RUT",
  "z": "NO-0301",
  "e": {
    "StopPlace": ["tramStop", "busStop"],
    "PlaceOfInterest": ["*"]
  }
}
```

### Fields:
- **r** (required): Role code (e.g., `editRouteData`, `editStops`)
- **o** (required): Organisation code
- **z** (optional): Administrative zone code
- **e** (optional): Entity classifications mapping entity types to their classifiers

## Available Roles

### Administrative Roles
- `adminEditRouteData` - Full administrator rights for all route data
- `editOrganisation` - Administrator rights for organization management

### Route Data Roles
- `editRouteData` - Edit route data for assigned providers
- `viewAllRouteData` - View route data for all providers

### Stop Place Roles
- `editStops` - Edit stop places
- `deleteStops` - Delete stop places

### Organization Roles
- `viewAllOrganisationData` - View all organization data
- `readRoleAssignments` - View role assignments for all users

### NeTEx Roles
- `viewPrivateNetexData` - View private NeTEx data (blocks, dead runs, etc.)

## Authorization Service Methods

### Role Checks
```java
// Check administrative roles
boolean isAdmin = authorizationService.isRouteDataAdmin();
boolean isOrgAdmin = authorizationService.isOrganisationAdmin();

// View permissions
boolean canViewAll = authorizationService.canViewAllOrganisationData();
boolean canViewRoles = authorizationService.canViewRoleAssignments();
```

### Provider-Specific Checks
```java
String providerId = "RUT";

// Route data permissions
boolean canView = authorizationService.canViewRouteData(providerId);
boolean canEdit = authorizationService.canEditRouteData(providerId);

// Block data permissions
boolean canViewBlocks = authorizationService.canViewBlockData(providerId);
```

### Validation Methods (throws AccessDeniedException)
```java
authorizationService.validateRouteDataAdmin();
authorizationService.validateOrganisationAdmin();
authorizationService.validateEditRouteData(providerId);
authorizationService.validateViewBlockData(providerId);
```

## Custom Entity Resolver

To use data-scoped authorization with custom entities, implement the `EntityResolver` interface:

```java
@Component
public class MyEntityResolver implements EntityResolver {
    
    @Override
    public String resolveOrganisationRef(Object entity) {
        if (entity instanceof MyEntity) {
            return ((MyEntity) entity).getOrganisationId();
        }
        return null;
    }
    
    @Override
    public Set<String> resolveAdministrativeZoneRefs(Object entity) {
        if (entity instanceof MyEntity) {
            return ((MyEntity) entity).getAdministrativeZones();
        }
        return Collections.emptySet();
    }
    
    @Override
    public String resolveEntityTypeName(Object entity) {
        if (entity instanceof StopPlace) {
            return "StopPlace";
        }
        return entity.getClass().getSimpleName();
    }
    
    @Override
    public Set<String> resolveEntityClassifierNames(Object entity) {
        if (entity instanceof StopPlace) {
            return Set.of(((StopPlace) entity).getStopPlaceType().value());
        }
        return Collections.emptySet();
    }
}
```

## Building Role Assignments

```java
RoleAssignment assignment = RoleAssignment.builder()
    .withRole("editRouteData")
    .withOrganisation("RUT")
    .withAdministrativeZone("NO-0301")
    .withEntityClassification("StopPlace", "tramStop")
    .withEntityClassification("StopPlace", "busStop")
    .build();
```

## Testing

For testing purposes, use `FullAccessAuthorizationService` to grant all permissions:

```java
@TestConfiguration
public class TestSecurityConfig {
    
    @Bean
    @Primary
    public AuthorizationService<String> testAuthorizationService() {
        return new FullAccessAuthorizationService<>();
    }
}
```

## Dependencies

- Spring Security Core
- Jackson (for JSON serialization)
- Google Guava
- SLF4J

## License

Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence").

See: https://joinup.ec.europa.eu/software/page/eupl

## Contributing

This library is part of the [Rutebanken Helpers](https://github.com/entur/rutebanken-helpers) project.

## Support

For issues and questions, please use the GitHub issue tracker.
