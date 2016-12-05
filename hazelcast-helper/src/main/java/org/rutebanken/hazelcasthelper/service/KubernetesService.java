package org.rutebanken.hazelcasthelper.service;

import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KubernetesService {
    private static final Logger log = LoggerFactory.getLogger(KubernetesService .class);

    private final String kubernetesUrl;

    protected final String namespace;

    private final boolean kuberentesEnabled;

    protected KubernetesClient kube;

    public KubernetesService(String kubernetesUrl, String namespace, boolean kuberentesEnabled) {
        this.kubernetesUrl = kubernetesUrl;
        this.namespace = namespace;
        this.kuberentesEnabled = kuberentesEnabled;
    }

    public KubernetesService(String namespace, boolean kuberentesEnabled) {
        this( null, namespace, kuberentesEnabled );
    }

    @PostConstruct
    public final void init() {
        if ( !kuberentesEnabled ) {
            log.warn("Disabling kubernetes connection as rutebanken.kubernetes.enabled="+kuberentesEnabled);
            return;
        }
        if ( kubernetesUrl != null && !"".equals( kubernetesUrl )) {
            log.info("Connecting to "+kubernetesUrl );
            kube = new DefaultKubernetesClient("http://localhost:8000/");
        } else {
            log.info("Using default settings, as this should auto-configure correctly in the kubernetes cluster");
            kube = new DefaultKubernetesClient();
        }
    }

    @PreDestroy
    public final void end() {
        if ( kube != null ) {
            kube.close();
        }
    }

    public boolean isKuberentesEnabled() {
        return kuberentesEnabled;
    }

    public List<String> findEndpoints() {
        String serviceName = findDeploymentName();
        log.info("Shall find endpoints for "+serviceName);
        return findEndpoints( serviceName );
    }

    /**
     * When running on kubernetes, the deployment name is part of the hostname.
     * TODO It is known that this will fail if the hostname contains dashes. Improve later
     */
    public String findDeploymentName() {
        String hostname = System.getenv("HOSTNAME");
        if ( hostname == null ) {
            hostname = "localhost";
        }
        int dash = hostname.indexOf("-");
        return dash == -1
                ? hostname
                : hostname.substring(0, dash );
    }

    /**
     * @return Endpoints found for the given service name, both the ready and not ready endpoints
     */
    public List<String> findEndpoints(String serviceName) {
        if ( kube == null ) {
            return new ArrayList<>();
        }
        Endpoints eps = kube.endpoints().inNamespace(namespace).withName(serviceName).get();
        List<String> ready    = addressesFrom( eps, EndpointSubset::getAddresses);
        List<String> notready = addressesFrom( eps, EndpointSubset::getNotReadyAddresses);
        log.info("Got " + ready.size() + " endpoints and " + notready.size() + " NOT ready endpoints");

        List<String> result = new ArrayList<>(ready);
        result.addAll( notready );
        log.info("Ended up with the the following endpoints for endpoint " + serviceName + " : " + result);
        return result;
    }

    private List<String> addressesFrom(Endpoints endpoints, Function<EndpointSubset, List<EndpointAddress>> addressFunction ) {
        if ( endpoints == null || endpoints.getSubsets() == null) {
            return new ArrayList<>();
        }

        return endpoints.getSubsets()
                .stream()
                .map(addressFunction)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(EndpointAddress::getIp)
                .collect(Collectors.toList());
    }
}