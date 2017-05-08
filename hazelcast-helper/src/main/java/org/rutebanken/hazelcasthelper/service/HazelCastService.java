package org.rutebanken.hazelcasthelper.service;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class HazelCastService {
    private static final Logger log = LoggerFactory.getLogger(HazelCastService.class);

    protected HazelcastInstance hazelcast;

    private boolean startupOk = false;

    private KubernetesService kubernetesService;

    private final String managementUrl;

    /**
     * Create a networked hazelcast instance, or
     * @param kubernetesService A networked hazelcast instance is only set up if
     *                          kubernetesService is not null
     */
    public HazelCastService(KubernetesService kubernetesService) {
        this(kubernetesService, null);
    }

    public HazelCastService(KubernetesService kubernetesService, String managementUrl) {
        this.kubernetesService = kubernetesService;
        this.managementUrl = managementUrl;
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
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                hazelcast.shutdown();
            }
        });
    }


    @PreDestroy
    public final void shutdown() {
        log.info("Shutdown initiated");
        hazelcast.shutdown();
        log.info("Shutdown finished");
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

        // http://docs.hazelcast.org/docs/3.7/manual/html-single/index.html#backing-up-maps
        MapConfig mapConfig = cfg.getMapConfig("default");
        updateDefaultMapConfig(mapConfig);

        log.info("Old config: b_count "+mapConfig.getBackupCount()+" async_b_count "+mapConfig.getAsyncBackupCount()+" read_backup_data "+mapConfig.isReadBackupData());
        mapConfig.setBackupCount( 2 )
                .setAsyncBackupCount( 0 )
                .setReadBackupData( true );

        log.info("Updated config: b_count "+mapConfig.getBackupCount()+" async_b_count "+mapConfig.getAsyncBackupCount()+" read_backup_data "+mapConfig.isReadBackupData());

        addMgmtIfConfigured(cfg);

        cfg.setNetworkConfig(netCfg);

        getAdditionalMapConfigurations().forEach(cfg::addMapConfig);

        return Hazelcast.newHazelcastInstance(cfg);
    }

    /**
     * Override this method if you want to provide additional configuration to the map config with name "default"
     *
     * @param defaultMapConfig The map config with name "default"
     */
    public void updateDefaultMapConfig(MapConfig defaultMapConfig) {}


    /**
     * If you want to provide additional map configurations to hazelcast.
     *
     * @return a list with desired map configurations
     */
    public List<MapConfig> getAdditionalMapConfigurations() {
        return new ArrayList<>();
    }

    /**
     * Adding management configuration if it has been given
     */
    private void addMgmtIfConfigured(Config cfg) {
        if ( managementUrl != null && !managementUrl.isEmpty() ) {
            ManagementCenterConfig mcc = new ManagementCenterConfig(managementUrl, 3);
            mcc.setEnabled(true);
            cfg.setManagementCenterConfig(mcc);
            log.info("Added management URL: "+managementUrl);
        }
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
        addMgmtIfConfigured(cfg);

        return Hazelcast.newHazelcastInstance(cfg);
    }
}
