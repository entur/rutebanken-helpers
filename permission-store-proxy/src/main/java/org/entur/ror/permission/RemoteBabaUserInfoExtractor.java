package org.entur.ror.permission;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import org.springframework.http.MediaType;
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
  @Nullable
  public String getPreferredName() {
    BabaUser babaUser = getBabaUser();
    if (babaUser == null) {
      return null;
    }
    BabaContactDetails babaContactDetails = babaUser.contactDetails;
    return babaContactDetails.firstName + " " + babaContactDetails.lastName;
  }

  @Override
  @Nullable
  public String getPreferredUsername() {
    BabaUser babaUser = getBabaUser();
    if (babaUser == null) {
      return null;
    }
    return getBabaUser().username;
  }

  private BabaUser getBabaUser() {
    Authentication authentication = SecurityContextHolder
      .getContext()
      .getAuthentication();
    if (
      !(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
    ) {
      return null;
    }

    AuthenticatedUser authenticatedUser = AuthenticatedUser.of(
      jwtAuthenticationToken
    );

    List<BabaUser> users = webClient
      .post()
      .uri(uri, uriBuilder -> uriBuilder.path("/authenticatedUser").build())
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(authenticatedUser.toDTO())
      .retrieve()
      .bodyToFlux(BabaUser.class)
      .retryWhen(
        Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
      )
      .collectList()
      .block();

    if (users == null || users.isEmpty()) {
      throw new IllegalArgumentException(
        "User not found: " + authenticatedUser.subject()
      );
    }
    if (users.size() > 1) {
      throw new IllegalStateException(
        "Multiple users found: " + authenticatedUser.subject()
      );
    }
    return users.get(0);
  }
}
