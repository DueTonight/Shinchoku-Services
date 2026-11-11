package com.shinchoku.shinchokuapp.security;

import com.shinchoku.shinchokuapp.api.ApiException;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The authenticated identity for the current request. A sessionId means browser/session
 * authentication; an apiKeyWorkspaceId means a workspace-bound API key. Key scopes restrict the
 * owner's permissions rather than grant new roles.
 */
public record Caller(
    UUID userId, UUID sessionId, UUID apiKeyWorkspaceId, Set<String> scopes, boolean mfaVerified) {

  /**
   * Session-only endpoints use this distinction to reject API keys, even valid keys owned by the
   * same user.
   */
  public boolean isSessionAuthenticated() {
    return sessionId != null;
  }

  /**
   * Reads the identity installed by AuthenticationFilter. Services cannot impersonate a user by
   * accepting an author ID from request JSON.
   */
  public static Caller current() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof Caller authenticatedCaller)) {
      throw new ApiException(401, "UNAUTHENTICATED", "Authentication required");
    }
    return authenticatedCaller;
  }

  /**
   * Requires a browser/session identity for actions such as managing sessions or creating API keys.
   */
  public void requireSession() {
    if (!isSessionAuthenticated()) {
      throw ApiException.forbidden();
    }
  }

  /**
   * Applies the key's maximum allowed operation. Workspace/team role checks are still performed
   * separately by Access.
   */
  public void requireScope(String requiredScope) {
    if (!isSessionAuthenticated()
        && !scopes.contains("admin")
        && !scopes.contains(requiredScope)
        && !(requiredScope.equals("read") && scopes.contains("write"))) {
      throw ApiException.forbidden();
    }
  }
}
