# Plan: Add Multi-Audience Support to MultiIssuerAuthenticationManagerResolver

## Objective
Update `MultiIssuerAuthenticationManagerResolver` and `MultiIssuerAuthenticationManagerResolverBuilder` to support multiple audiences for the Entur Partner and Entur Internal tenants while maintaining backward compatibility.

## Current State Analysis

### Existing Implementation
1. **MultiIssuerAuthenticationManagerResolverBuilder**: 
   - Uses single `String` fields for audiences: `enturInternalAuth0Audience`, `enturPartnerAuth0Audience`, `rorAuth0Audience`
   - Provides `withXxxAudience(String)` methods
   
2. **MultiIssuerAuthenticationManagerResolver**:
   - Constructor accepts single `String` parameters for audiences
   - Protected methods `enturInternalAuth0JwtDecoder()`, `enturPartnerAuth0JwtDecoder()`, `rorAuth0JwtDecoder()` create decoders
   - Uses `RoRJwtDecoderBuilder.withAudience(String)` which internally creates `AudienceValidator(audience)`

3. **RoRJwtDecoderBuilder**:
   - Has `withAudience(String audience)` method
   - Creates `AudienceValidator(audience)` in `build()` method

4. **AudienceValidator**:
   - Already supports both single audience and multiple audiences (List<String>)
   - Has two constructors: `AudienceValidator(String)` and `AudienceValidator(List<String>)`

### Marduk's Current Solution
- Extends `MultiIssuerAuthenticationManagerResolver`
- Overrides `enturPartnerAuth0JwtDecoder()` method
- Manually creates `NimbusJwtDecoder` and `AudienceValidator` with `List.of(enturPartnerAuth0Audience, rorAuth0Audience)`
- This workaround is necessary because base class doesn't support multiple audiences

## Proposed Changes

### 1. RoRJwtDecoderBuilder (Optional but Recommended)

**File**: `src/main/java/org/entur/oauth2/RoRJwtDecoderBuilder.java`

**Changes**:
```java
public class RoRJwtDecoderBuilder {
  private String issuer;
  private String audience;        // Keep for backward compatibility
  private List<String> audiences; // New field for multiple audiences
  private String auth0ClaimNamespace;

  // Existing method - backward compatible
  public RoRJwtDecoderBuilder withAudience(String audience) {
    this.audience = audience;
    this.audiences = null; // Clear the list if single audience is set
    return this;
  }

  // New method for multiple audiences
  public RoRJwtDecoderBuilder withAudiences(List<String> audiences) {
    this.audiences = audiences;
    this.audience = null; // Clear single audience if list is set
    return this;
  }

  public JwtDecoder build() {
    // Determine which audience configuration to use
    OAuth2TokenValidator<Jwt> audienceValidator;
    if (audiences != null && !audiences.isEmpty()) {
      audienceValidator = new AudienceValidator(audiences);
    } else if (audience != null) {
      audienceValidator = new AudienceValidator(audience);
    } else {
      throw new IllegalStateException("Either audience or audiences must be set");
    }
    // ... rest of build logic
  }
}
```

**Backward Compatibility**: Existing code using `withAudience(String)` continues to work without changes.

---

### 2. MultiIssuerAuthenticationManagerResolverBuilder

**File**: `src/main/java/org/entur/oauth2/multiissuer/MultiIssuerAuthenticationManagerResolverBuilder.java`

**Changes**:
```java
public class MultiIssuerAuthenticationManagerResolverBuilder {

  // Existing fields - keep for backward compatibility
  private String enturInternalAuth0Audience;
  private String enturPartnerAuth0Audience;
  private String rorAuth0Audience;
  
  // New fields for multiple audiences
  private List<String> enturInternalAuth0Audiences;
  private List<String> enturPartnerAuth0Audiences;
  
  private String enturInternalAuth0Issuer;
  private String enturPartnerAuth0Issuer;
  private String rorAuth0Issuer;
  private String rorAuth0ClaimNamespace;

  // Existing single-audience methods - backward compatible
  public MultiIssuerAuthenticationManagerResolverBuilder withEnturInternalAuth0Audience(
    String enturInternalAuth0Audience
  ) {
    this.enturInternalAuth0Audience = enturInternalAuth0Audience;
    this.enturInternalAuth0Audiences = null;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturPartnerAuth0Audience(
    String enturPartnerAuth0Audience
  ) {
    this.enturPartnerAuth0Audience = enturPartnerAuth0Audience;
    this.enturPartnerAuth0Audiences = null;
    return this;
  }

  // New multiple-audience methods
  public MultiIssuerAuthenticationManagerResolverBuilder withEnturInternalAuth0Audiences(
    List<String> enturInternalAuth0Audiences
  ) {
    this.enturInternalAuth0Audiences = enturInternalAuth0Audiences;
    this.enturInternalAuth0Audience = null;
    return this;
  }

  public MultiIssuerAuthenticationManagerResolverBuilder withEnturPartnerAuth0Audiences(
    List<String> enturPartnerAuth0Audiences
  ) {
    this.enturPartnerAuth0Audiences = enturPartnerAuth0Audiences;
    this.enturPartnerAuth0Audience = null;
    return this;
  }

  // Existing methods remain unchanged
  public MultiIssuerAuthenticationManagerResolverBuilder withRorAuth0Audience(
    String rorAuth0Audience
  ) {
    this.rorAuth0Audience = rorAuth0Audience;
    return this;
  }

  // ... other existing methods ...

  public MultiIssuerAuthenticationManagerResolver build() {
    return new MultiIssuerAuthenticationManagerResolver(
      enturInternalAuth0Audience,
      enturInternalAuth0Audiences,
      enturInternalAuth0Issuer,
      enturPartnerAuth0Audience,
      enturPartnerAuth0Audiences,
      enturPartnerAuth0Issuer,
      rorAuth0Audience,
      rorAuth0Issuer,
      rorAuth0ClaimNamespace
    );
  }
}
```

**Note**: RoR Auth0 remains single-audience only as per current Marduk implementation.

---

### 3. MultiIssuerAuthenticationManagerResolver

**File**: `src/main/java/org/entur/oauth2/multiissuer/MultiIssuerAuthenticationManagerResolver.java`

**Changes**:
```java
public class MultiIssuerAuthenticationManagerResolver
  implements AuthenticationManagerResolver<HttpServletRequest> {

  // Existing single-audience fields - keep for backward compatibility
  private final String enturInternalAuth0Audience;
  private final String enturPartnerAuth0Audience;
  private final String rorAuth0Audience;
  
  // New multiple-audience fields
  private final List<String> enturInternalAuth0Audiences;
  private final List<String> enturPartnerAuth0Audiences;
  
  private final String enturInternalAuth0Issuer;
  private final String enturPartnerAuth0Issuer;
  private final String rorAuth0Issuer;
  private final String rorAuth0ClaimNamespace;

  // Existing constructor - keep for backward compatibility
  @Deprecated
  protected MultiIssuerAuthenticationManagerResolver(
    String enturInternalAuth0Audience,
    String enturInternalAuth0Issuer,
    String enturPartnerAuth0Audience,
    String enturPartnerAuth0Issuer,
    String rorAuth0Audience,
    String rorAuth0Issuer,
    String rorAuth0ClaimNamespace
  ) {
    this(
      enturInternalAuth0Audience,
      null, // enturInternalAuth0Audiences
      enturInternalAuth0Issuer,
      enturPartnerAuth0Audience,
      null, // enturPartnerAuth0Audiences
      enturPartnerAuth0Issuer,
      rorAuth0Audience,
      rorAuth0Issuer,
      rorAuth0ClaimNamespace
    );
  }

  // New primary constructor
  protected MultiIssuerAuthenticationManagerResolver(
    String enturInternalAuth0Audience,
    List<String> enturInternalAuth0Audiences,
    String enturInternalAuth0Issuer,
    String enturPartnerAuth0Audience,
    List<String> enturPartnerAuth0Audiences,
    String enturPartnerAuth0Issuer,
    String rorAuth0Audience,
    String rorAuth0Issuer,
    String rorAuth0ClaimNamespace
  ) {
    this.enturInternalAuth0Audience = enturInternalAuth0Audience;
    this.enturInternalAuth0Audiences = enturInternalAuth0Audiences;
    this.enturInternalAuth0Issuer = enturInternalAuth0Issuer;
    this.enturPartnerAuth0Audience = enturPartnerAuth0Audience;
    this.enturPartnerAuth0Audiences = enturPartnerAuth0Audiences;
    this.enturPartnerAuth0Issuer = enturPartnerAuth0Issuer;
    this.rorAuth0Audience = rorAuth0Audience;
    this.rorAuth0Issuer = rorAuth0Issuer;
    this.rorAuth0ClaimNamespace = rorAuth0ClaimNamespace;
  }

  // Update decoder methods to use list when available
  @Override
  protected JwtDecoder enturInternalAuth0JwtDecoder() {
    RoRJwtDecoderBuilder builder = new RoRJwtDecoderBuilder()
      .withIssuer(enturInternalAuth0Issuer)
      .withAuth0ClaimNamespace(rorAuth0ClaimNamespace);
      
    if (enturInternalAuth0Audiences != null && !enturInternalAuth0Audiences.isEmpty()) {
      builder.withAudiences(enturInternalAuth0Audiences);
    } else if (enturInternalAuth0Audience != null) {
      builder.withAudience(enturInternalAuth0Audience);
    }
    
    return builder.build();
  }

  @Override
  protected JwtDecoder enturPartnerAuth0JwtDecoder() {
    RoRJwtDecoderBuilder builder = new RoRJwtDecoderBuilder()
      .withIssuer(enturPartnerAuth0Issuer)
      .withAuth0ClaimNamespace(rorAuth0ClaimNamespace);
      
    if (enturPartnerAuth0Audiences != null && !enturPartnerAuth0Audiences.isEmpty()) {
      builder.withAudiences(enturPartnerAuth0Audiences);
    } else if (enturPartnerAuth0Audience != null) {
      builder.withAudience(enturPartnerAuth0Audience);
    }
    
    return builder.build();
  }

  // rorAuth0JwtDecoder remains unchanged (single audience only)
  
  // ... rest of the class remains unchanged ...
}
```

---

## Migration Path for Marduk

After these changes are implemented in `rutebanken-helpers`:

### Option 1: Use Builder Pattern (Recommended)
```java
@Component
public class MardukMultiIssuerAuthenticationManagerResolver {
  
  @Bean
  public MultiIssuerAuthenticationManagerResolver multiIssuerResolver(
      @Value("${marduk.oauth2.resourceserver.auth0.partner.jwt.audience}") String enturPartnerAuth0Audience,
      @Value("${marduk.oauth2.resourceserver.auth0.partner.jwt.issuer-uri}") String enturPartnerAuth0Issuer,
      @Value("${marduk.oauth2.resourceserver.auth0.ror.jwt.audience}") String rorAuth0Audience,
      @Value("${marduk.oauth2.resourceserver.auth0.ror.jwt.issuer-uri}") String rorAuth0Issuer,
      @Value("${marduk.oauth2.resourceserver.auth0.ror.claim.namespace}") String rorAuth0ClaimNamespace) {
    
    return new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturPartnerAuth0Audiences(List.of(enturPartnerAuth0Audience, rorAuth0Audience))
      .withEnturPartnerAuth0Issuer(enturPartnerAuth0Issuer)
      .withRorAuth0Audience(rorAuth0Audience)
      .withRorAuth0Issuer(rorAuth0Issuer)
      .withRorAuth0ClaimNamespace(rorAuth0ClaimNamespace)
      .build();
  }
}
```

### Option 2: Keep Extending (Still Works)
The existing Marduk implementation will continue to work because:
1. The old constructor is kept (but deprecated)
2. The `enturPartnerAuth0JwtDecoder()` method can still be overridden

---

## Testing Strategy

### Unit Tests to Add/Update

**File**: `src/test/java/org/entur/oauth2/multiissuer/MultiIssuerAuthenticationManagerResolverTest.java`

```java
@Test
void testEnturPartnerWithMultipleAudiences() {
  MultiIssuerAuthenticationManagerResolver resolver = 
    new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturPartnerAuth0Audiences(List.of("audience1", "audience2"))
      .withEnturPartnerAuth0Issuer("https://partner.auth0.com/")
      .withRorAuth0ClaimNamespace("https://entur.io/")
      .build();
  
  // Test that resolver can create decoder with multiple audiences
  assertNotNull(resolver.enturPartnerAuth0JwtDecoder());
}

@Test
void testBackwardCompatibilityWithSingleAudience() {
  MultiIssuerAuthenticationManagerResolver resolver = 
    new MultiIssuerAuthenticationManagerResolverBuilder()
      .withEnturPartnerAuth0Audience("single-audience")
      .withEnturPartnerAuth0Issuer("https://partner.auth0.com/")
      .withRorAuth0ClaimNamespace("https://entur.io/")
      .build();
  
  assertNotNull(resolver.enturPartnerAuth0JwtDecoder());
}

@Test
void testEnturInternalWithMultipleAudiences() {
  // Similar test for Entur Internal
}
```

**File**: `src/test/java/org/entur/oauth2/RoRJwtDecoderBuilderTest.java` (new)

```java
@Test
void testBuildWithSingleAudience() {
  JwtDecoder decoder = new RoRJwtDecoderBuilder()
    .withIssuer("https://issuer.com/")
    .withAudience("single-audience")
    .build();
  
  assertNotNull(decoder);
}

@Test
void testBuildWithMultipleAudiences() {
  JwtDecoder decoder = new RoRJwtDecoderBuilder()
    .withIssuer("https://issuer.com/")
    .withAudiences(List.of("audience1", "audience2"))
    .build();
  
  assertNotNull(decoder);
}

@Test
void testThrowsExceptionWhenNoAudienceSet() {
  assertThrows(IllegalStateException.class, () ->
    new RoRJwtDecoderBuilder()
      .withIssuer("https://issuer.com/")
      .build()
  );
}
```

---

## Backward Compatibility Guarantees

1. ✅ **Existing code using single audiences continues to work**: The original `withXxxAudience(String)` methods are preserved
2. ✅ **Existing constructor is preserved**: Deprecated but functional for classes extending `MultiIssuerAuthenticationManagerResolver`
3. ✅ **No breaking changes to method signatures**: All new methods are additions, not modifications
4. ✅ **Default behavior unchanged**: When using single audience methods, behavior is identical to current implementation
5. ✅ **Marduk's extension continues to work**: The protected methods can still be overridden

---

## Implementation Order

1. **Phase 1**: Update `RoRJwtDecoderBuilder`
   - Add `withAudiences(List<String>)` method
   - Update `build()` method to handle both single and multiple audiences
   - Add unit tests

2. **Phase 2**: Update `MultiIssuerAuthenticationManagerResolver`
   - Add new fields for multiple audiences
   - Create new primary constructor with List parameters
   - Deprecate old constructor, delegate to new one
   - Update `enturInternalAuth0JwtDecoder()` and `enturPartnerAuth0JwtDecoder()` methods
   - Add unit tests

3. **Phase 3**: Update `MultiIssuerAuthenticationManagerResolverBuilder`
   - Add new fields for multiple audiences
   - Add `withXxxAudiences(List<String>)` methods
   - Update `build()` method to pass Lists to resolver
   - Add unit tests

4. **Phase 4**: Update documentation
   - Update CLAUDE.md with new multi-audience capability
   - Add JavaDoc examples showing both single and multiple audience usage
   - Document migration path from Marduk's current approach

---

## Benefits

1. **Eliminates need for subclassing**: Marduk and other projects can use builder pattern instead
2. **Cleaner code**: No need to override protected methods
3. **Full backward compatibility**: Existing code continues to work
4. **Consistent API**: Multiple audiences supported across all tenants (except RoR which remains single)
5. **Reusable**: Any project needing multiple audiences can use this feature

---

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking changes for existing clients | Keep all existing constructors and methods, only deprecate old constructor |
| Confusion between single/multiple audience methods | Clear JavaDoc, builder prevents setting both simultaneously |
| Complex testing matrix | Comprehensive unit tests covering all combinations |
| Marduk migration issues | Provide clear migration guide, keep extending as valid option |

---

## Questions to Resolve

1. **Should RoR Auth0 also support multiple audiences?** 
   - Current Marduk implementation only uses multiple audiences for Entur Partner
   - Decision: Keep RoR single-audience for now, can add later if needed

2. **Should we validate that at least one audience is provided?**
   - Decision: Yes, throw `IllegalStateException` in builder if neither is set

3. **What happens if both single and multiple audiences are set?**
   - Decision: Last one set wins (clear previous value)

4. **Should the old constructor be removed in a future major version?**
   - Decision: Yes, but document deprecation with removal timeline

---

## Documentation Updates Required

1. **CLAUDE.md**: Add section on multi-audience configuration
2. **JavaDoc**: Update class-level documentation for all modified classes
3. **Migration Guide**: Create guide for projects currently extending the resolver (like Marduk)
4. **Example configurations**: Add code examples showing both single and multiple audience usage

---

## Summary

This plan provides a comprehensive, backward-compatible solution to support multiple audiences for Entur Partner and Entur Internal tenants. The approach uses builder pattern enhancements and constructor overloading to maintain compatibility while adding new functionality. Marduk can migrate from its custom subclass to using the builder pattern, or continue with its existing approach.
