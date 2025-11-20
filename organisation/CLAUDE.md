# Organisation Module

## Overview
The `organisation` module is a helper library for managing role-based authorization and access control in Rutebanken/Entur applications. It provides functionality to extract roles and administrative zones from JWT tokens, validate permissions, and control access to route data and organizational information based on user roles and assignments.

## Purpose
This module enables fine-grained access control by:
- Extracting and managing role assignments from JWT tokens
- Validating user permissions for various operations
- Checking organizational boundaries and administrative zones
- Controlling access to route data, stop places, and NeTEx data based on provider assignments

## Key Components

### 1. Role Management
- **`RoleAssignment`**: Represents a user's role assignment with organization, administrative zone, and entity classifications. Uses short attribute names (`r`, `o`, `z`, `e`) to keep JWT tokens small.
- **`RoleAssignmentExtractor`**: Interface for extracting role assignments from Spring Security authentication context.

### 2. Authorization Services
- **`AuthorizationService<T>`**: Core interface defining authorization checks for various operations:
  - Route data administration and editing
  - Organization administration
  - Block data viewing
  - Role assignment viewing
  
- **`DefaultAuthorizationService`**: Default implementation that checks role assignments against organizational boundaries.
- **`FullAccessAuthorizationService`**: Implementation that grants full access to all operations (for testing/admin scenarios).
- **`ReflectionAuthorizationService`**: Advanced implementation using reflection to validate data access based on object properties.
- **`DataScopedAuthorizationService`**: Extended authorization service that validates access to specific data objects.

### 3. Validation Components
- **`OrganisationChecker`**: Validates that data belongs to organizations the user has access to.
- **`AdministrativeZoneChecker`**: Validates access based on administrative zones.
- **`EntityResolver`**: Interface for resolving entity properties needed for authorization checks.

### 4. Authorization Constants
Defined in `AuthorizationConstants`:
- `ROLE_ROUTE_DATA_ADMIN`: Administrator rights for all route data
- `ROLE_ROUTE_DATA_EDIT`: Editor rights for provider-specific route data
- `ROLE_ROUTE_DATA_VIEW_ALL`: Viewer rights for all route data
- `ROLE_ORGANISATION_EDIT`: Organization administrator rights
- `ROLE_ORGANISATION_DATA_VIEW_ALL`: Viewer rights for all organizations
- `ROLE_NETEX_PRIVATE_DATA_VIEW`: Viewer rights for private NeTEx data (blocks, dead runs, etc.)
- `ROLE_EDIT_STOPS`: Stop place editing rights
- `ROLE_DELETE_STOPS`: Stop place deletion rights
- `ROLE_ROLE_ASSIGNMENTS_VIEW`: Rights to view role assignments

## Dependencies
- **Spring Security**: For authentication and authorization integration
- **Jackson**: For JSON serialization of role assignments in JWT
- **Google Guava**: Utility libraries
- **SLF4J**: Logging framework

## Usage Example

```java
// Inject authorization service
@Autowired
private AuthorizationService<String> authorizationService;

// Check if user can edit route data for a provider
String providerId = "RUT";
if (authorizationService.canEditRouteData(providerId)) {
    // Allow editing
} else {
    // Deny access
}

// Validate with exception throwing
authorizationService.validateEditRouteData(providerId);

// Check administrative roles
if (authorizationService.isRouteDataAdmin()) {
    // Full access to route data
}

// Extract role assignments from current user
@Autowired
private RoleAssignmentExtractor roleAssignmentExtractor;

List<RoleAssignment> assignments = roleAssignmentExtractor.getRoleAssignmentsForUser();
```

## Role Assignment Structure
Role assignments in JWT tokens follow this compact structure:
```json
{
  "r": "editRouteData",        // Role code
  "o": "RUT",                   // Organisation code (required)
  "z": "NO-0301",              // Administrative zone code (optional)
  "e": {                        // Entity classifications (optional)
    "StopPlace": ["tramStop", "busStop"],
    "PlaceOfInterest": ["*"]
  }
}
```

## Architecture Notes
- **JWT Integration**: Role assignments are expected to be included in JWT tokens under the "roles" claim
- **Spring Security**: Uses `SecurityContextHolder` to access current authentication context
- **Reflection-based Validation**: The `ReflectionAuthorizationService` can validate object access by inspecting object properties via reflection
- **Provider-scoped Access**: Most operations are scoped to specific data providers, allowing granular access control

## Exception Handling
- **`NotAuthenticatedException`**: Thrown when no authenticated user is found
- **`OrganisationException`**: General exception for organization-related errors
- **`AccessDeniedException`**: Spring Security exception thrown when authorization fails

## Testing
The module includes comprehensive test coverage in `src/test/java` using JUnit 5 and Hamcrest matchers.

## License
Licensed under EUPL v1.2 (European Union Public License)
