package com.shinchoku.shinchokuapp.security;

import static com.shinchoku.shinchokuapp.persistence.JpaPredicates.*;

import com.shinchoku.shinchokuapp.api.ApiException;
import com.shinchoku.shinchokuapp.api.Errors;
import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import com.shinchoku.shinchokuapp.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@Component
public class OidcLogin implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;

  private final OAuthConnectionRepository oAuthConnectionRepository;

  private final UserSessionRepository userSessionRepository;

  private final TokenService tokenService;

  private final TransactionTemplate transactionTemplate;

  private final ObjectMapper objectMapper;

  private final String frontendUrl;

  public OidcLogin(
      UserRepository userRepository,
      OAuthConnectionRepository oAuthConnectionRepository,
      UserSessionRepository userSessionRepository,
      TokenService tokenService,
      TransactionTemplate transactionTemplate,
      ObjectMapper objectMapper,
      @Value("${app.frontend-url}") String frontendUrl) {
    this.userRepository = userRepository;
    this.oAuthConnectionRepository = oAuthConnectionRepository;
    this.userSessionRepository = userSessionRepository;
    this.tokenService = tokenService;
    this.transactionTemplate = transactionTemplate;
    this.objectMapper = objectMapper;
    this.frontendUrl = frontendUrl;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws java.io.IOException {
    try {
      if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)
          || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
        throw new ApiException(401, "VERIFIED_EMAIL_REQUIRED", "A verified OIDC email is required");
      }
      var oauth = (OAuth2AuthenticationToken) authentication;
      UUID userId =
          transactionTemplate.execute(
              status -> {
                String provider = oauth.getAuthorizedClientRegistrationId(),
                    subject = oidcUser.getSubject();
                Object linkingUser = request.getSession().getAttribute("linkUser");
                if (linkingUser != null) {
                  String rawToken = null;
                  if (request.getCookies() != null) {
                    for (var cookie : request.getCookies()) {
                      if (cookie.getName().equals(TokenService.COOKIE)) {
                        rawToken = cookie.getValue();
                      }
                    }
                  }
                  UserSession original =
                      rawToken == null
                          ? null
                          : userSessionRepository
                              .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                                  TokenService.hashToken(rawToken), Instant.now())
                              .orElse(null);
                  if (original == null
                      || !original.getUserId().equals(UUID.fromString(linkingUser.toString()))) {
                    throw new ApiException(
                        401, "LINK_SESSION_EXPIRED", "Sign in again before linking this identity");
                  }
                }
                OAuthConnection existing =
                    oAuthConnectionRepository
                        .findByProviderAndProviderSubject(provider, subject)
                        .orElse(null);
                if (existing != null) {
                  if (linkingUser != null
                      && !existing.getUserId().equals(UUID.fromString(linkingUser.toString()))) {
                    throw ApiException.conflict("This provider belongs to another account");
                  }
                  return existing.getUserId();
                }
                UUID ownerId;
                if (linkingUser != null) {
                  ownerId = UUID.fromString(linkingUser.toString());
                } else {
                  String email = oidcUser.getEmail().toLowerCase(Locale.ROOT);
                  if (userRepository.findByEmail(email).isPresent()) {
                    throw ApiException.conflict(
                        "Sign in with the existing provider before linking this identity");
                  }
                  User user = new User();
                  user.setEmail(email);
                  user.setDisplayName(
                      oidcUser.getFullName() == null ? email : oidcUser.getFullName());
                  ownerId = userRepository.saveAndFlush(user).getId();
                }
                OAuthConnection connection = new OAuthConnection();
                connection.setUserId(ownerId);
                connection.setProvider(provider);
                connection.setProviderSubject(subject);
                oAuthConnectionRepository.saveAndFlush(connection);
                return ownerId;
              });
      Object assurance = oidcUser.getClaims().get("amr");
      boolean mfaVerified =
          assurance instanceof Collection<?> methods
              && (methods.contains("mfa") || (methods.contains("pwd") && methods.contains("otp")));
      tokenService.issueSessionCookie(userId, mfaVerified, request, response);
      if (request.getSession(false) != null) {
        request.getSession(false).invalidate();
      }
      response.sendRedirect(frontendUrl);
    } catch (ApiException exception) {
      response.setStatus(exception.status);
      response.setContentType("application/json");
      objectMapper.writeValue(
          response.getOutputStream(),
          Errors.envelope(exception.code, exception.getMessage(), request));
    }
  }
}
