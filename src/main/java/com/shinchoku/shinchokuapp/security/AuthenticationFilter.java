package com.shinchoku.shinchokuapp.security;

import com.shinchoku.shinchokuapp.api.*;
import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.repository.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Authenticates with typed session/key entities and never exposes those entities as JSON. */
@Component
public class AuthenticationFilter extends OncePerRequestFilter {

  private final ApiKeyRepository apiKeyRepository;

  private final UserSessionRepository userSessionRepository;

  private final WorkspaceMemberRepository workspaceMemberRepository;

  private final ObjectMapper objectMapper;

  private final String allowedFrontendOrigin;

  public AuthenticationFilter(
      ApiKeyRepository apiKeyRepository,
      UserSessionRepository userSessionRepository,
      WorkspaceMemberRepository workspaceMemberRepository,
      ObjectMapper objectMapper,
      @Value("${app.frontend-url}") String allowedFrontendOrigin) {
    this.apiKeyRepository = apiKeyRepository;
    this.userSessionRepository = userSessionRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.objectMapper = objectMapper;
    this.allowedFrontendOrigin = allowedFrontendOrigin;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    request.setAttribute("requestId", requestId);
    response.setHeader("X-Request-Id", requestId);
    try {
      String authorization = request.getHeader("Authorization"), sessionToken = null;
      if (request.getCookies() != null) {
        for (Cookie cookie : request.getCookies()) {
          if (cookie.getName().equals(TokenService.COOKIE)) {
            sessionToken = cookie.getValue();
          }
        }
      }
      Caller caller = null;
      if (authorization != null) {
        if (!authorization.startsWith("Bearer ")) {
          throw invalidToken();
        }
        ApiKey key =
            apiKeyRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                    TokenService.hashToken(authorization.substring(7)), Instant.now())
                .orElseThrow(AuthenticationFilter::invalidToken);
        if (workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserIdAndRemovedAtIsNull(
                key.getWorkspaceId(), key.getOwnerId())
            .isEmpty()) {
          throw invalidToken();
        }
        caller =
            new Caller(
                key.getOwnerId(),
                null,
                key.getWorkspaceId(),
                new HashSet<>(Arrays.asList(key.getScopes())),
                false);
        key.setLastUsedAt(Instant.now());
        apiKeyRepository.saveAndFlush(key);
      } else if (sessionToken != null) {
        UserSession session =
            userSessionRepository
                .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                    TokenService.hashToken(sessionToken), Instant.now())
                .orElse(null);
        if (session != null) {
          caller =
              new Caller(
                  session.getUserId(), session.getId(), null, Set.of(), session.getMfaVerified());
        }
      }
      if (caller != null) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(caller, null, List.of()));
      }
      if (!Set.of("GET", "HEAD", "OPTIONS", "TRACE").contains(request.getMethod())
          && authorization == null
          && (sessionToken != null || request.getRequestURI().startsWith("/api/dev/"))
          && !allowedFrontendOrigin.equals(request.getHeader("Origin"))) {
        throw new ApiException(403, "INVALID_ORIGIN", "Request origin is not allowed");
      }
      filterChain.doFilter(request, response);
    } catch (ApiException exception) {
      writeError(response, request, exception.status, exception.code, exception.getMessage());
    } catch (DataAccessException exception) {
      writeError(
          response, request, 503, "SERVICE_UNAVAILABLE", "The database is temporarily unavailable");
    }
  }

  private static ApiException invalidToken() {
    return new ApiException(401, "INVALID_TOKEN", "Invalid authentication token");
  }

  private void writeError(
      HttpServletResponse response,
      HttpServletRequest request,
      int status,
      String code,
      String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    objectMapper.writeValue(response.getOutputStream(), Errors.envelope(code, message, request));
  }
}
