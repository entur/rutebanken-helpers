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

import io.fabric8.kubernetes.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KubernetesService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    KubernetesService.class
  );

  private final String kubernetesUrl;

  protected final String serviceName;

  protected final String namespace;

  private final boolean kubernetesEnabled;

  protected KubernetesClient kube;

  public KubernetesService(
    String kubernetesUrl,
    String serviceName,
    String namespace,
    boolean kubernetesEnabled
  ) {
    this.kubernetesUrl = kubernetesUrl;
    this.serviceName = serviceName;
    this.namespace = namespace;
    this.kubernetesEnabled = kubernetesEnabled;
  }

  public KubernetesService(
    String kubernetesUrl,
    String namespace,
    boolean kubernetesEnabled
  ) {
    this(kubernetesUrl, null, namespace, kubernetesEnabled);
  }

  public KubernetesService(String namespace, boolean kubernetesEnabled) {
    this(null, null, namespace, kubernetesEnabled);
  }

  @PostConstruct
  public final void init() {
    if (!kubernetesEnabled) {
      LOGGER.warn(
        "Disabling kubernetes connection as rutebanken.kubernetes.enabled={}",
        kubernetesEnabled
      );
      return;
    }
    if (kubernetesUrl != null && !kubernetesUrl.isEmpty()) {
      LOGGER.info("Connecting to {}", kubernetesUrl);
      Config config = new ConfigBuilder()
        .withMasterUrl("http://localhost:8000/")
        .build();
      kube = new KubernetesClientBuilder().withConfig(config).build();
    } else {
      LOGGER.info(
        "Using default settings, as this should auto-configure correctly in the kubernetes cluster"
      );
      kube = new KubernetesClientBuilder().build();
    }
  }

  @PreDestroy
  public final void end() {
    if (kube != null) {
      kube.close();
    }
  }

  public boolean isKubernetesEnabled() {
    return kubernetesEnabled;
  }

  /**
   * Returns name for deployment if it is set explicitly, otherwise tries to resolve the name based
   * on property "HOSTNAME".
   * TODO It is known that this will fail if the hostname contains dashes. Improve later
   */
  public String findDeploymentName() {
    if (serviceName != null && !serviceName.isEmpty()) {
      // serviceName has been set explicitly
      return serviceName;
    }

    // Resolve name
    String hostname = System.getenv("HOSTNAME");
    if (hostname == null) {
      hostname = "localhost";
    }
    int dash = hostname.indexOf('-');
    return dash == -1 ? hostname : hostname.substring(0, dash);
  }
}
