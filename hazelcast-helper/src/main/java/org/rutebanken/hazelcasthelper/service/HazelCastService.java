package org.rutebanken.hazelcasthelper.service;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;

@Service
public class HazelCastService {
    private static final Logger log = LoggerFactory.getLogger(HazelCastService.class);

    protected HazelcastInstance hazelcast;

    private boolean startupOk = false;

    private KubernetesService kubernetesService;

    /**
     * Create a networked hazelcast instance, or
     * @param kubernetesService A networked hazelcast instance is only set up if
     *                          kubernetesService is not null
     */
    public HazelCastService(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @PostConstruct
    public final void init() {
        if ( kubernetesService != null && kubernetesService.isKuberentesEnabled()) {
            log.info("Configuring hazelcast");
            try {
                String name = kubernetesService.findDeploymentName();
                // Consider: The password part could be a kubernetes secret
                hazelcast = runHazelcast( kubernetesService.findEndpoints(), name, name+"_pw" );
                startupOk = true;
            } catch ( Exception e ) {
                throw new RutebankenHazelcastException("Could not run initialization of hazelcast.",e);
            }
        } else {
            log.warn("Using local hazelcast as we do not have kubernetes");
            hazelcast = initForLocalHazelCast();
        }
    }


    @PreDestroy
    public final void shutdown() {
        hazelcast.shutdown();
    }

    /**
     * Method to be used as part of liveness test for a k8s service
     */
    public Boolean getStartupOk() {
        return startupOk && numberOfClusterMembers() > 0;
    }

    /**
     * @return Just debug information - format may change over time
     */
    public String information() {
        if ( hazelcast == null ) {
            return "Hazelcast is null, i.e. not configured.";
        }
        StringBuilder sb = new StringBuilder("cluster members \n");
        hazelcast.getCluster()
                .getMembers()
                .forEach( m -> sb.append(" - localMember: ")
                        .append(m.localMember())
                        .append(" Address: ")
                        .append(m.getAddress())
                        .append(" Attributes: ")
                        .append(m.getAttributes())
                        .append("\n"));

        return sb.toString();
    }

    public int numberOfClusterMembers() {
        return hazelcast == null
                ? 0
                : hazelcast.getCluster().getMembers().size();
    }

    private HazelcastInstance runHazelcast(final List<String> nodes, String groupName, String password) {
        final int HC_PORT = 5701;
        if ( nodes.isEmpty() ) {
            log.warn("No nodes given - will start lonely HZ");
        }
        // configure Hazelcast instance
        final Config cfg = new Config()
                .setInstanceName(UUID.randomUUID().toString())
                .setGroupConfig(new GroupConfig(groupName, password))
                .setProperty("hazelcast.phone.home.enabled","false");

        // tcp
        final TcpIpConfig tcpCfg = new TcpIpConfig();
        nodes.forEach(tcpCfg::addMember);
        tcpCfg.setEnabled(true);

        // network join configuration
        final JoinConfig joinCfg = new JoinConfig()
                .setMulticastConfig(new MulticastConfig().setEnabled(false))
                .setTcpIpConfig(tcpCfg);

        final NetworkConfig netCfg = new NetworkConfig()
                .setPortAutoIncrement(false)
                .setPort(HC_PORT)
                .setJoin(joinCfg)
                .setSSLConfig(new SSLConfig().setEnabled(false));

        cfg.setNetworkConfig(netCfg);
        return Hazelcast.newHazelcastInstance(cfg);
    }

    /**
     * Initialize a VM local instance of hazelcast
     */
    private HazelcastInstance initForLocalHazelCast() {
        log.info("Running hazelcast with LOCAL configuration ONLY - this is for junit tests and when you do not have a kubernetes environment");
        final Config cfg = new Config()
                .setInstanceName(UUID.randomUUID().toString())
                .setProperty("hazelcast.phone.home.enabled", "false");
        final JoinConfig joinCfg = new JoinConfig()
                .setMulticastConfig(new MulticastConfig().setEnabled(false))
                .setTcpIpConfig(new TcpIpConfig().setEnabled(false));
        NetworkConfig networkCfg = new NetworkConfig()
                .setJoin(joinCfg);
        networkCfg.getInterfaces()
                .setEnabled(false);
        cfg.setNetworkConfig( networkCfg );

        return Hazelcast.newHazelcastInstance(cfg);
    }
}
