package org.entur.ror.permission;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

/**
 * UserInfoExtractor that extracts user information from the Baba user repository.
 */
public class RemoteBabaUserInfoExtractor implements UserInfoExtractor {

  private static final String CLAIM_ROR_PREFERRED_USERNAME =
    "https://ror.entur.io/preferred_username";

  private static final Predicate<Throwable> is5xx = throwable ->
    throwable instanceof WebClientResponseException webClientResponseException &&
    webClientResponseException.getStatusCode().is5xxServerError();

  private static final long MAX_RETRY_ATTEMPTS = 3;

  private final WebClient webClient;
  private final String uri;

  public RemoteBabaUserInfoExtractor(WebClient webClient, String uri) {
    this.webClient = webClient;
    this.uri = uri;
  }

  @Override
  public String getPreferredName() {
    BabaContactDetails babaContactDetails = getBabaUser().contactDetails;
    return babaContactDetails.firstName + " " + babaContactDetails.lastName;
  }

  @Override
  public String getPreferredUsername() {
    return getBabaUser().username;
  }

  private BabaUser getBabaUser() {
    Authentication authentication = SecurityContextHolder
      .getContext()
      .getAuthentication();
    if (
      !(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
    ) {
      throw new AccessDeniedException("Not authenticated with token");
    }

    String preferredUserName = (String) jwtAuthenticationToken
      .getTokenAttributes()
      .get(CLAIM_ROR_PREFERRED_USERNAME);

    List<BabaUser> users = webClient
      .get()
      .uri(
        uri,
        uriBuilder ->
          uriBuilder.path("/{userName}/user").build(preferredUserName)
      )
      .retrieve()
      .bodyToFlux(BabaUser.class)
      .retryWhen(
        Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
      )
      .collectList()
      .block();

    if (users == null || users.isEmpty()) {
      throw new IllegalArgumentException(
        "User not found: " + preferredUserName
      );
    }
    if (users.size() > 1) {
      throw new IllegalStateException(
        "Multiple users found: " + preferredUserName
      );
    }
    return users.get(0);
  }
}
