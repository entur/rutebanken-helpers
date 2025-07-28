package org.entur.ror.permission;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.rutebanken.helper.organisation.user.UserInfoExtractor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

/**
 * UserInfoExtractor that extracts user information from the Baba user repository.
 * Calls to the API happen often in burst, for example because an application page is refreshed and multiple API
 * calls are sent at once.
 * To reduce the load, this implementation makes use of a short-lived cache (10s)
 */
public class RemoteBabaUserInfoExtractor implements UserInfoExtractor {

  private static final Predicate<Throwable> is5xx = throwable ->
    throwable instanceof WebClientResponseException webClientResponseException &&
    webClientResponseException.getStatusCode().is5xxServerError();

  private static final long MAX_RETRY_ATTEMPTS = 3;
  private static final Duration CACHE_TTL = Duration.ofSeconds(10);

  private final WebClient webClient;
  private final String uri;
  private final Cache<AuthenticatedUser, BabaUser> babaUserCache;

  public RemoteBabaUserInfoExtractor(WebClient webClient, String uri) {
    this.webClient = webClient;
    this.uri = uri;
    babaUserCache = Caffeine.newBuilder().expireAfterWrite(CACHE_TTL).build();
  }

  @Override
  @Nullable
  public String getPreferredName() {
    BabaUser babaUser = getBabaUser();
    if (babaUser == null) {
      return null;
    }
    if (babaUser.isClient) {
      return babaUser.username + " (API Client)";
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
    if (babaUser.isClient) {
      return babaUser.username + " (API Client)";
    }
    return babaUser.username;
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

    return babaUserCache.get(authenticatedUser, this::getBabaUser);
  }

  private BabaUser getBabaUser(AuthenticatedUser authenticatedUser) {
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
