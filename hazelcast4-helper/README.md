# hazelcast4-helper

Provides base Hazelcast configuration for distributed caching in Kubernetes environments. Handles cluster discovery, lifecycle management, and extension hooks for custom map configurations.

## Maven

```xml
<dependency>
    <groupId>org.entur.ror.helpers</groupId>
    <artifactId>hazelcast4-helper</artifactId>
    <version>VERSION</version>
</dependency>
```

## Key Classes

### `HazelCastService`

Base Spring service for managing a Hazelcast instance. Extend this class to customize the cluster configuration.

**Defaults:**
- Port: `5701`
- Backup count: `2`
- Read backup data: enabled
- Shutdown hook: disabled (Spring manages lifecycle)
- Kubernetes mode: auto-detected; falls back to single-node local mode

**Extension hooks (override in subclass):**

| Method | Description |
|--------|-------------|
| `updateDefaultMapConfig(MapConfig)` | Customize the default map config (eviction, TTL, max size, etc.) |
| `getAdditionalMapConfigurations()` | Return additional named map configs |
| `getSerializerConfigs()` | Provide custom Hazelcast serializers |

### `KubernetesService`

Detects whether the app is running in Kubernetes and provides the current namespace. Inject this into `HazelCastService`.

## Usage

### Minimal setup (use defaults)

```java
@Configuration
public class CacheConfig extends HazelCastService {
    public CacheConfig(KubernetesService kubernetesService) {
        super(kubernetesService);
    }
}
```

### Custom map configuration

```java
@Configuration
public class CacheConfig extends HazelCastService {

    public CacheConfig(KubernetesService kubernetesService) {
        super(kubernetesService);
    }

    @Override
    public void updateDefaultMapConfig(MapConfig config) {
        config.setEvictionConfig(
            new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LRU)
                .setMaxSizePolicy(MaxSizePolicy.USED_HEAP_PERCENTAGE)
                .setSize(70)
        ).setTimeToLiveSeconds(604800); // 7 days
    }

    @Override
    public List<MapConfig> getAdditionalMapConfigurations() {
        return List.of(
            new MapConfig("short-lived-cache")
                .setTimeToLiveSeconds(300)
                .setBackupCount(1)
        );
    }
}
```

## Kubernetes Discovery

When running in Kubernetes, Hazelcast uses DNS-based member discovery within the same namespace. No additional service mesh configuration is required.

When running locally, Hazelcast starts in single-node mode (multicast disabled) so no clustering is attempted.

## Known Issues

### `Unknown protocol: HTT`

Caused by missing Hazelcast port configuration in the Kubernetes service definition.

**Fix:** Ensure port `5701` (or the configured Hazelcast port) is declared in the Kubernetes service:

```yaml
spec:
  ports:
    - name: hazelcast
      port: 5701
      protocol: TCP
      targetPort: 5701
```

---

[Back to root](../README.md)