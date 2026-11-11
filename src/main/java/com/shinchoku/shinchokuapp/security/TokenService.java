package com.shinchoku.shinchokuapp.security;

import com.shinchoku.shinchokuapp.entity.UserSession;
import com.shinchoku.shinchokuapp.repository.UserSessionRepository;
import jakarta.servlet.http.*;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The raw credential travels only in the cookie; JPA persists a one-way hash in UserSession. */
@Service
@Transactional
public class TokenService {

  public static final String COOKIE = "SHINCHOKU_SESSION";

  private final UserSessionRepository userSessionRepository;

  private final boolean secureCookies;

  private final long sessionLifetimeHours;

  public TokenService(
      UserSessionRepository userSessionRepository,
      @Value("${app.cookie-secure}") boolean secureCookies,
      @Value("${app.session-hours}") long sessionLifetimeHours) {
    this.userSessionRepository = userSessionRepository;
    this.secureCookies = secureCookies;
    this.sessionLifetimeHours = sessionLifetimeHours;
  }

  public static String generateToken() {
    byte[] randomBytes = new byte[32];
    new SecureRandom().nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  public static String hashToken(String rawToken) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  public void issueSessionCookie(
      UUID userId, boolean mfaVerified, HttpServletRequest request, HttpServletResponse response) {
    String rawToken = generateToken();
    UserSession session = new UserSession();
    session.setUserId(userId);
    session.setTokenHash(hashToken(rawToken));
    session.setUserAgent(request.getHeader("User-Agent"));
    session.setExpiresAt(Instant.now().plus(Duration.ofHours(sessionLifetimeHours)));
    session.setMfaVerified(mfaVerified);
    userSessionRepository.saveAndFlush(session);
    response.addHeader(
        "Set-Cookie",
        ResponseCookie.from(COOKIE, rawToken)
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofHours(sessionLifetimeHours))
            .build()
            .toString());
  }

  public void clearSessionCookie(HttpServletResponse response) {
    response.addHeader(
        "Set-Cookie",
        ResponseCookie.from(COOKIE, "")
            .httpOnly(true)
            .secure(secureCookies)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build()
            .toString());
  }
}
