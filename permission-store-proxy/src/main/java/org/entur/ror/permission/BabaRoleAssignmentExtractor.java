package org.entur.ror.permission;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import org.rutebanken.helper.organisation.RoleAssignment;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

/**
 * Role Assignment extractor that retrieves RoleAssignments from the API exposed by the organisation repository Baba.
 * The user is matched by the preferred named claim in the JWT token.
 */
public class BabaRoleAssignmentExtractor implements RoleAssignmentExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    BabaRoleAssignmentExtractor.class
  );

  private static final long MAX_RETRY_ATTEMPTS = 3;
  private static final String OAUTH2_CLAIM_PREFERRED_USERNAME =
    "https://ror.entur.io/preferred_username";

  private final WebClient webClient;
  private final String uri;

  private static final Predicate<Throwable> is5xx = throwable ->
    throwable instanceof WebClientResponseException webClientResponseException &&
    webClientResponseException.getStatusCode().is5xxServerError();

  /**
   *
   * @param webclient an authorized web client that has access to the user API.
   * @param uri the URI to the REST service.
   */
  public BabaRoleAssignmentExtractor(WebClient webclient, String uri) {
    this.webClient = webclient;
    this.uri = uri;
  }

  @Override
  public List<RoleAssignment> getRoleAssignmentsForUser(
    Authentication authentication
  ) {
    long t1 = System.currentTimeMillis();

    if (
      !(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
    ) {
      throw new AccessDeniedException("Not authenticated with token");
    }

    String preferredUserName = (String) jwtAuthenticationToken
      .getTokenAttributes()
      .get(OAUTH2_CLAIM_PREFERRED_USERNAME);

    List<RoleAssignment> roleAssignments = webClient
      .get()
      .uri(
        uri,
        uriBuilder ->
          uriBuilder
            .path("/{userName}/roleAssignments")
            .build(preferredUserName)
      )
      .retrieve()
      .bodyToFlux(RoleAssignment.class)
      .retryWhen(
        Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
      )
      .collectList()
      .block();

    long t2 = System.currentTimeMillis();
    LOGGER.trace("Retrieved role assignments in {} ms", t2 - t1);

    return roleAssignments;
  }
}
