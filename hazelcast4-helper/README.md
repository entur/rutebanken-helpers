## Hazelcast 4 helper
Helps configure Hazelcast for usage in Kubernetes.

### Known issues:
1. `Unknown protocol: HTT`
   Caused by missing configuration of hazelcast-port in kubernetes.
   
   Fix: Ensure that the port hazelcast uses (default: _5701_) is configured in kubernetes service.
   E.g:
   ```
   spec:
     ports:
       - name: hazelcast
         port: 5701
         protocol: TCP
         targetPort: 5701
   ```