package com.shinchoku.shinchokuapp.service;

import static com.shinchoku.shinchokuapp.persistence.JpaPredicates.*;

import com.shinchoku.shinchokuapp.api.ApiException;
import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import com.shinchoku.shinchokuapp.persistence.JpaPagination;
import com.shinchoku.shinchokuapp.repository.*;
import com.shinchoku.shinchokuapp.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Personal identity/session operations and workspace key/repository metadata. Credential entities
 * stay private; response DTOs expose only approved fields.
 */
@Service
@Transactional
public class ProfileService {

  private final UserRepository userRepository;

  private final UserSessionRepository userSessionRepository;

  private final OAuthConnectionRepository oAuthConnectionRepository;

  private final ApiKeyRepository apiKeyRepository;

  private final GithubRepositoryJpaRepository githubRepositoryJpaRepository;

  private final Access workspaceAccess;

  private final TokenService tokenService;

  private final JpaPagination pagination;

  private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;

  private final String frontendUrl;

  public ProfileService(
      UserRepository userRepository,
      UserSessionRepository userSessionRepository,
      OAuthConnectionRepository oAuthConnectionRepository,
      ApiKeyRepository apiKeyRepository,
      GithubRepositoryJpaRepository githubRepositoryJpaRepository,
      Access workspaceAccess,
      TokenService tokenService,
      JpaPagination pagination,
      ObjectProvider<ClientRegistrationRepository> clientRegistrations,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.userRepository = userRepository;
    this.userSessionRepository = userSessionRepository;
    this.oAuthConnectionRepository = oAuthConnectionRepository;
    this.apiKeyRepository = apiKeyRepository;
    this.githubRepositoryJpaRepository = githubRepositoryJpaRepository;
    this.workspaceAccess = workspaceAccess;
    this.tokenService = tokenService;
    this.pagination = pagination;
    this.clientRegistrations = clientRegistrations;
    this.frontendUrl = frontendUrl;
  }

  /**
   * Loads the authenticated user through UserRepository; clients cannot choose another user ID for
   * this personal route.
   */
  public ProfileResponse getProfile() {
    return ResponseMapper.profile(
        userRepository.findById(Caller.current().userId()).orElseThrow(ApiException::missing));
  }

  /**
   * Applies typed profile fields to the managed User and validates timezone names before
   * persistence.
   */
  public ProfileResponse updateProfile(ProfilePatchRequest request) {
    Caller.current().requireSession();
    User user =
        userRepository.findById(Caller.current().userId()).orElseThrow(ApiException::missing);
    if (request.hasTimezone()) {
      try {
        ZoneId.of(request.getTimezone());
      } catch (Exception exception) {
        throw ApiException.invalid("Unknown timezone");
      }
      user.setTimezone(request.getTimezone());
    }
    if (request.hasDisplayName()) {
      user.setDisplayName(request.getDisplayName());
    }
    if (request.hasAvatarUrl()) {
      user.setAvatarUrl(request.getAvatarUrl());
    }
    if (request.hasTheme()) {
      user.setTheme(request.getTheme());
    }
    return ResponseMapper.profile(userRepository.saveAndFlush(user));
  }

  /**
   * Returns active-session DTOs with a current-session flag. Token hashes and MFA internals remain
   * on UserSession entities.
   */
  public PageResponse<SessionResponse> listSessions(CursorRequest cursor) {
    Caller caller = Caller.current();
    caller.requireSession();
    return pagination.byTime(
        UserSession.class,
        and(
            equalTo("userId", caller.userId()),
            equalTo("revokedAt", null),
            (root, query, builder) -> builder.greaterThan(root.get("expiresAt"), Instant.now())),
        cursor,
        "createdAt",
        UserSession::getCreatedAt,
        false,
        session -> ResponseMapper.session(session, caller.sessionId()));
  }

  /**
   * Only the current user's session can be revoked here; revoking the active session also expires
   * its browser cookie.
   */
  public void revokeSession(UUID sessionId, HttpServletResponse response) {
    Caller caller = Caller.current();
    caller.requireSession();
    UserSession session =
        userSessionRepository
            .findOne(and(equalTo("id", sessionId), equalTo("userId", caller.userId())))
            .orElseThrow(ApiException::missing);
    session.setRevokedAt(Instant.now());
    if (sessionId.equals(caller.sessionId())) {
      tokenService.clearSessionCookie(response);
    }
  }

  /**
   * Revokes the application session and clears both its cookie and any transient OIDC linking
   * session.
   */
  public void logout(HttpServletRequest request, HttpServletResponse response) {
    Caller caller = Caller.current();
    caller.requireSession();
    userSessionRepository
        .findById(caller.sessionId())
        .ifPresent(session -> session.setRevokedAt(Instant.now()));
    tokenService.clearSessionCookie(response);
    if (request.getSession(false) != null) {
      request.getSession(false).invalidate();
    }
  }

  public PageResponse<ConnectionResponse> listConnections(CursorRequest cursor) {
    Caller.current().requireSession();
    return pagination.byTime(
        OAuthConnection.class,
        equalTo("userId", Caller.current().userId()),
        cursor,
        "createdAt",
        OAuthConnection::getCreatedAt,
        false,
        ResponseMapper::connection);
  }

  /**
   * Locks the user before counting connections so two concurrent removals cannot both remove the
   * last sign-in method.
   */
  public void disconnectProvider(UUID connectionId) {
    Caller.current().requireSession();
    UUID userId = Caller.current().userId();
    userRepository.findLocked(userId).orElseThrow(ApiException::missing);
    OAuthConnection connection =
        oAuthConnectionRepository
            .findOne(and(equalTo("id", connectionId), equalTo("userId", userId)))
            .orElseThrow(ApiException::missing);
    if (oAuthConnectionRepository.countByUserId(userId) <= 1) {
      throw ApiException.conflict("The last sign-in method cannot be removed");
    }
    oAuthConnectionRepository.delete(connection);
    oAuthConnectionRepository.flush();
  }

  /**
   * Allows only the registered return URI and configured provider, then records the authenticated
   * user initiating the link.
   */
  public AuthorizationUrlResponse startConnection(
      String provider, ConnectionStartRequest request, HttpServletRequest servletRequest) {
    Caller.current().requireSession();
    if (!frontendUrl.equals(request.redirectUri())) {
      throw ApiException.invalid("Redirect URI is not registered");
    }
    if (clientRegistrations.getIfAvailable() == null
        || clientRegistrations.getObject().findByRegistrationId(provider) == null) {
      throw new ApiException(
          503, "PROVIDER_NOT_CONFIGURED", "This identity provider has not been configured");
    }
    servletRequest.getSession().setAttribute("linkUser", Caller.current().userId().toString());
    return new AuthorizationUrlResponse(frontendUrl + "/oauth2/authorization/" + provider);
  }

  public PageResponse<ApiKeyResponse> listApiKeys(UUID workspaceId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    Specification<ApiKey> filter = and(workspace(workspaceId), equalTo("revokedAt", null));
    if (!workspaceAccess.isWorkspaceAdministrator(workspaceId)) {
      filter = filter.and(equalTo("ownerId", Caller.current().userId()));
    }
    return pagination.byId(ApiKey.class, filter, cursor, ResponseMapper::apiKey);
  }

  /**
   * Checks scope and expiry, persists a hash and prefix, and reveals the raw token once in
   * ApiKeyIssuedResponse.
   */
  public ApiKeyIssuedResponse createApiKey(UUID workspaceId, ApiKeyCreateRequest request) {
    Caller.current().requireSession();
    workspaceAccess.requireWorkspaceWritePermission(workspaceId);
    if (request.scopes().contains(ApiKeyScope.ADMIN)) {
      workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    }
    if (!request.expiresAt().isAfter(Instant.now())) {
      throw ApiException.invalid("API key expiry must be in the future");
    }
    String token = "shk_" + TokenService.generateToken();
    ApiKey key = new ApiKey();
    key.setWorkspaceId(workspaceId);
    key.setOwnerId(Caller.current().userId());
    key.setName(request.name());
    key.setScopes(request.scopes().stream().map(ApiKeyScope::value).toArray(String[]::new));
    key.setExpiresAt(request.expiresAt());
    key.setTokenHash(TokenService.hashToken(token));
    key.setPrefix(token.substring(0, 12));
    return ResponseMapper.issuedKey(apiKeyRepository.saveAndFlush(key), token);
  }

  /** Permits the key owner or a workspace administrator to revoke a workspace-scoped key. */
  public void revokeApiKey(UUID workspaceId, UUID keyId) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    ApiKey key =
        apiKeyRepository
            .findByWorkspaceIdAndId(workspaceId, keyId)
            .orElseThrow(ApiException::missing);
    if (!key.getOwnerId().equals(Caller.current().userId())) {
      workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    } else {
      Caller.current().requireScope("write");
    }
    key.setRevokedAt(Instant.now());
  }

  /**
   * Only metadata is listed. A guest with a project-only grant does not gain visibility into the
   * entire team's repository metadata.
   */
  public PageResponse<RepositoryResponse> listGithubRepositories(
      UUID workspaceId, UUID teamId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    Specification<GithubRepository> filter =
        and(
            workspace(workspaceId),
            workspaceAccess.visibleTeamResource(workspaceId, "teamId", null));
    if (teamId != null) {
      filter = filter.and(equalTo("teamId", teamId));
    }
    return pagination.byId(GithubRepository.class, filter, cursor, ResponseMapper::repository);
  }
}
