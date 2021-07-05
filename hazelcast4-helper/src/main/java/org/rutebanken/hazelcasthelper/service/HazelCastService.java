/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package org.rutebanken.hazelcasthelper.service;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HazelCastService {
    private static final Logger log = LoggerFactory.getLogger(HazelCastService.class);

    private static final int HC_DEFAULT_PORT = 5701;

    protected HazelcastInstance hazelcast;

    private final KubernetesService kubernetesService;

    private int backupCount = 2;
    private int backupCountAsync;
    private boolean shutDownHookEnabled;

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
        if ( kubernetesService != null && kubernetesService.isKubernetesEnabled()) {
            log.info("Configuring hazelcast");
            try {
                String name = kubernetesService.findDeploymentName();
                hazelcast = runHazelcast(name);
            } catch ( Exception e ) {
                throw new RutebankenHazelcastException("Could not run initialization of hazelcast.",e);
            }
        } else {
            log.warn("Using local hazelcast as we do not have kubernetes");
            hazelcast = initForLocalHazelCast();
        }
        // the shutdown hook should be disabled in a Spring environment or J2E application server to prevent Hazelcast from being stopped
        // too early, leading to the exception "HazelcastInstanceNotActiveException" at shutdown time.
        // Instead the shutdown should be managed by Spring at application context-closing time or by the J2E server at PreDestroy time.
        // see HazelCastService.shutdown()

        if (shutDownHookEnabled) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> hazelcast.shutdown()));
        }
    }

    @PreDestroy
    public final void shutdown() {
        log.info("Hazelcast instance shutdown initiated");
        hazelcast.shutdown();
        log.info("Hazelcast instance shutdown finished");
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

    /**
     * Override this method if you want to provide additional configuration to the map config with name "default".
     * The map config will be updated with some configuration for backup, after the execution of this very method.
     *
     * See the example below for overriding:
     *
     * <pre>
     *  public void updateDefaultMapConfig(MapConfig mapConfig) {
     *     mapConfig
     *       .setEvictionPolicy(EvictionPolicy.LRU)
     *       .setTimeToLiveSeconds(604800)
     *       .setMaxSizeConfig(
     *         new MaxSizeConfig(70, MaxSizeConfig.MaxSizePolicy.USED_HEAP_PERCENTAGE));
     *     logger.info("Map config: {}", mapConfig);
     *  }
     * </pre>
     *
     * @param defaultMapConfig The map config with name "default" to make updates on.
     */
    public void updateDefaultMapConfig(MapConfig defaultMapConfig) {
        // Should be overridden when needed
    }


    /**
     * If you want to provide additional map configurations to hazelcast, you can override this method.
     * This list will be added to the Hazelcast configuration. See {@link com.hazelcast.config.Config#addMapConfig(MapConfig)}
     *
     * Refer to the Hazelcast documentation for configuring maps programmatically.
     * <a href="http://docs.hazelcast.org/docs/3.8/manual/html-single/index.html#configuring-programmatically">Configuring Programmatically</a>
     *
     * See code example in Tiamat: <a href="https://github.com/rutebanken/tiamat/blob/master/src/main/java/org/rutebanken/tiamat/hazelcast/ExtendedHazelcastService.java">ExtendedHazelcastService</a>
     *
     * See the below example below for how to configure one or more maps by overriding this method.
     * <pre>
     * {@code
     *  @Override
     *  public List<MapConfig> getAdditionalMapConfigurations() {
     *     return Arrays.asList(
     *       new MapConfig()
     *         .setName("myVeryImportantMap")
     *         .setBackupCount(2)
     *         .setTimeToLiveSeconds(300));
     *  }
     * }
     * </pre>
     *
     * @return a list with desired map configurations
     */
    public List<MapConfig> getAdditionalMapConfigurations() {
        return new ArrayList<>();
    }



    /**
     * If you want to provide a custom serializer, you can override this method.
     * This list will be added to the Hazelcast configuration. See {@link com.hazelcast.config.SerializationConfig#addSerializerConfig(SerializerConfig)}
     *
     * See code example in Anshar: <a href="https://github.com/rutebanken/anshar/blob/master/src/main/java/org/rutebanken/anshar/messages/collections/ExtendedHazelcastService.java">ExtendedHazelcastService</a>
     *
     * See the below example below for how to configure one or more maps by overriding this method.
     * <pre>
     * {@code
     *  @Override
     *  public List<SerializerConfig> getSerializerConfigs() {
     *     return Arrays.asList(
     *       new SerializerConfig()
     *         .setTypeClass(SpecialClass.class)
     *         .setImplementation(new AwesomeSerializer()));
     *
     *  }
     * }
     * </pre>
     *
     * @return a list with serializers
     */
    public List<SerializerConfig> getSerializerConfigs() {
        return new ArrayList<>();
    }

    private HazelcastInstance runHazelcast(String groupName) {

        // configure Hazelcast instance
        final Config cfg = new Config()
                .setInstanceName(UUID.randomUUID().toString())
                .setClusterName(groupName)
                .setProperty("hazelcast.phone.home.enabled", "false");
        // the shutdown hook should be disabled in a Spring environment to prevent Hazelcast from being stopped
        // too early, leading to the exception "HazelcastInstanceNotActiveException" at shutdown time.
        // Instead the shutdown should be managed by Spring at application context-closing time
        // see HazelCastService.shutdown()
        // TODO the shutdown hook should eventually be disabled by default
        if (!shutDownHookEnabled) {
            cfg.setProperty("hazelcast.shutdownhook.enabled", "false");
        }

        final KubernetesConfig kubernetesConfig = new KubernetesConfig();
        kubernetesConfig
            .setEnabled(kubernetesService.isKubernetesEnabled())
            .setProperty("namespace", kubernetesService.namespace)
            .setProperty("service-name", kubernetesService.findDeploymentName());

        // network join configuration
        final JoinConfig joinCfg = new JoinConfig()
                .setMulticastConfig(new MulticastConfig().setEnabled(false))
                .setKubernetesConfig(kubernetesConfig);

        final NetworkConfig netCfg = new NetworkConfig()
                .setPortAutoIncrement(false)
                .setPort(HC_DEFAULT_PORT)
                .setJoin(joinCfg)
                .setSSLConfig(new SSLConfig().setEnabled(false));

        MapConfig mapConfig = cfg.getMapConfig("default");
        updateDefaultMapConfig(mapConfig);

        log.info("Old config: b_count {} async_b_count {} read_backup_data {}", mapConfig.getBackupCount(), mapConfig.getAsyncBackupCount(), mapConfig.isReadBackupData());
        // http://docs.hazelcast.org/docs/3.7/manual/html-single/index.html#backing-up-maps
        mapConfig.setBackupCount(backupCount)
                .setAsyncBackupCount(backupCountAsync)
                .setReadBackupData(true);

        log.info("Updated config: b_count {} async_b_count {} read_backup_data {}", mapConfig.getBackupCount(), mapConfig.getAsyncBackupCount(), mapConfig.isReadBackupData());

        addMgmtIfConfigured(cfg);

        cfg.setNetworkConfig(netCfg);

        getAdditionalMapConfigurations().forEach(cfg::addMapConfig);

        getSerializerConfigs().forEach( cfg.getSerializationConfig()::addSerializerConfig);

        return Hazelcast.newHazelcastInstance(cfg);
    }

    /**
     * Adding management configuration
     */
    private void addMgmtIfConfigured(Config cfg) {
            ManagementCenterConfig mcc = new ManagementCenterConfig();
            cfg.setManagementCenterConfig(mcc);
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

        MapConfig mapConfig = cfg.getMapConfig("default");
        updateDefaultMapConfig(mapConfig);
        getAdditionalMapConfigurations().forEach(cfg::addMapConfig);

        getSerializerConfigs().forEach( cfg.getSerializationConfig()::addSerializerConfig);

        addMgmtIfConfigured(cfg);

        return Hazelcast.newHazelcastInstance(cfg);
    }

    protected void setBackupCount(int backupCount) {
        this.backupCount = backupCount;
    }

    protected void setBackupCountAsync(int backupCountAsync) {
        this.backupCountAsync = backupCountAsync;
    }

    protected void setShutDownHookEnabled(boolean shutDownHookEnabled) {
        this.shutDownHookEnabled = shutDownHookEnabled;
    }

}
