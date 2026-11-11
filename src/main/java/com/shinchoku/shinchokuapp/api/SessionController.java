package com.shinchoku.shinchokuapp.api;

import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.User;
import com.shinchoku.shinchokuapp.repository.UserRepository;
import com.shinchoku.shinchokuapp.security.TokenService;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class SessionController {

  private final Contract contract;

  public SessionController(Contract contract) {
    this.contract = contract;
  }

  public record CsrfResponse(String token, String headerName) {}

  @GetMapping("/api/v1/auth/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getToken(), token.getHeaderName());
  }

  @GetMapping("/api/v1/openapi")
  public Map<String, Object> openapi() {
    return contract.document();
  }
}

/** Development-only bootstrap account. It is never registered in the production profile. */
@Profile("local")
@RestController
class LocalSessionController {

  private final UserRepository userRepository;

  private final TokenService tokenService;

  private final boolean enabled;

  LocalSessionController(
      UserRepository userRepository,
      TokenService tokenService,
      @Value("${app.local-login-enabled:false}") boolean enabled) {
    this.userRepository = userRepository;
    this.tokenService = tokenService;
    this.enabled = enabled;
  }

  @PostMapping("/api/dev/login")
  @Transactional
  public ProfileResponse login(HttpServletRequest request, HttpServletResponse response) {
    if (!enabled
        || !Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1").contains(request.getRemoteAddr())) {
      throw ApiException.missing();
    }
    User user =
        userRepository
            .findByEmail("developer@shinchoku.local")
            .orElseGet(
                () -> {
                  User developer = new User();
                  developer.setEmail("developer@shinchoku.local");
                  developer.setDisplayName("Local Developer");
                  return userRepository.saveAndFlush(developer);
                });
    tokenService.issueSessionCookie(user.getId(), false, request, response);
    return ResponseMapper.profile(user);
  }
}
