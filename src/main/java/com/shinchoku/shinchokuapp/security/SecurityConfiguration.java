package com.shinchoku.shinchokuapp.security;

import com.shinchoku.shinchokuapp.api.Errors;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import tools.jackson.databind.ObjectMapper;

/** Configures which endpoints are public and how browser sessions and OIDC login are protected. */
@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain security(
      HttpSecurity httpSecurity,
      AuthenticationFilter authenticationFilter,
      ObjectMapper objectMapper,
      ObjectProvider<ClientRegistrationRepository> clientRegistrationProvider,
      OidcLogin oidcLoginHandler,
      @Value("${app.cookie-secure}") boolean secureCookies)
      throws Exception {
    // JavaScript may read the CSRF cookie to echo it in a header. The separate session cookie stays
    // HttpOnly.
    CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfTokenRepository.setCookieCustomizer(
        cookieBuilder -> cookieBuilder.secure(secureCookies).sameSite("Lax").path("/"));
    httpSecurity.csrf(
        csrfConfiguration ->
            csrfConfiguration
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .requireCsrfProtectionMatcher(
                    request ->
                        !Set.of("GET", "HEAD", "OPTIONS", "TRACE").contains(request.getMethod())
                            && request.getHeader("Authorization") == null));
    httpSecurity.authorizeHttpRequests(
        authorizationRules ->
            authorizationRules
                .requestMatchers(
                    "/actuator/health",
                    "/api/v1/auth/csrf",
                    "/api/dev/login",
                    "/oauth2/**",
                    "/login/**",
                    "/error")
                .permitAll()
                .anyRequest()
                .authenticated());
    // API authentication uses our persisted sessions/API keys, not an HTML login form or Basic
    // Auth.
    httpSecurity
        .formLogin(formLoginConfiguration -> formLoginConfiguration.disable())
        .httpBasic(basicAuthConfiguration -> basicAuthConfiguration.disable());
    httpSecurity.securityContext(
        contextConfiguration -> contextConfiguration.requireExplicitSave(true));
    httpSecurity.exceptionHandling(
        exceptionHandlers ->
            exceptionHandlers
                .authenticationEntryPoint(
                    (request, response, authenticationException) -> {
                      response.setStatus(401);
                      response.setContentType("application/json");
                      objectMapper.writeValue(
                          response.getOutputStream(),
                          Errors.envelope("UNAUTHENTICATED", "Authentication required", request));
                    })
                .accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                      response.setStatus(403);
                      response.setContentType("application/json");
                      objectMapper.writeValue(
                          response.getOutputStream(),
                          Errors.envelope(
                              "FORBIDDEN",
                              "Request is not permitted or the CSRF token is missing",
                              request));
                    }));
    httpSecurity.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
    if (clientRegistrationProvider.getIfAvailable() != null) {
      DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
          new DefaultOAuth2AuthorizationRequestResolver(
              clientRegistrationProvider.getObject(), "/oauth2/authorization");
      // PKCE binds the provider's authorization code to the browser flow that initiated login.
      authorizationRequestResolver.setAuthorizationRequestCustomizer(
          OAuth2AuthorizationRequestCustomizers.withPkce());
      httpSecurity.oauth2Login(
          oidcConfiguration ->
              oidcConfiguration
                  .authorizationEndpoint(
                      endpointConfiguration ->
                          endpointConfiguration.authorizationRequestResolver(
                              authorizationRequestResolver))
                  .successHandler(oidcLoginHandler)
                  .failureHandler(
                      (request, response, loginException) -> {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        objectMapper.writeValue(
                            response.getOutputStream(),
                            Errors.envelope(
                                "LOGIN_FAILED", "Identity provider login failed", request));
                      }));
    }
    return httpSecurity.build();
  }

  /**
   * Run the authentication filter once, inside Spring Security rather than twice in two filter
   * chains.
   */
  @Bean
  FilterRegistrationBean<AuthenticationFilter> disableContainerRegistration(
      AuthenticationFilter authenticationFilter) {
    FilterRegistrationBean<AuthenticationFilter> registration =
        new FilterRegistrationBean<>(authenticationFilter);
    registration.setEnabled(false);
    return registration;
  }
}
