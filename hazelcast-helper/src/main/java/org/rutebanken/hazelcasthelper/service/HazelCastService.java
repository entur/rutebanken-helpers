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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private KubernetesService kubernetesService;

    @PostConstruct
    public void init() {
        String supress = System.getProperty("suppress", "false");
        if ( supress.equals("false")) {
            log.info("Configuring up hazelcast");
            try {
                String name = kubernetesService.findDeploymentName();
                // Consider: The password part could be a kubernetes secret
                hazelcast = runHazelcast( kubernetesService.findEndpoints(), name, name+"_pw" );
                startupOk = true;
            } catch ( Exception e ) {
                log.error("Could not run init. HZ will be null and dummy implementation used",e);
            }
        } else {
            log.info("Hazelcast is NOT active as supress="+supress);
        }
    }


    @PreDestroy
    public void shutdown() {
        if ( hazelcast != null ) {
            hazelcast.shutdown();
        }
    }

    public Boolean getStartupOk() {
        return startupOk && numberOfClusterMemembers() > 0;
    }

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

    public int numberOfClusterMemembers() {
        return hazelcast.getCluster().getMembers().size();
    }

    private HazelcastInstance runHazelcast(final List<String> nodes, String groupName, String password) {
        final int HC_PORT = 5701;
        if ( nodes.isEmpty() ) {
            log.warn("No nodes given - will start lonely HZ");
        }
        // configure Hazelcast instance
        final Config cfg = new Config()
                .setInstanceName(UUID.randomUUID().toString())
                .setGroupConfig(new GroupConfig(groupName, password));

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

}
