package com.shinchoku.shinchokuapp;

import static com.shinchoku.shinchokuapp.support.TestDatabase.*;
import static org.junit.jupiter.api.Assertions.*;

import com.shinchoku.shinchokuapp.api.Contract;
import com.shinchoku.shinchokuapp.api.ContractOperation;
import com.shinchoku.shinchokuapp.api.InsightsController;
import com.shinchoku.shinchokuapp.api.IssueController;
import com.shinchoku.shinchokuapp.api.PlanningController;
import com.shinchoku.shinchokuapp.api.ProfileController;
import com.shinchoku.shinchokuapp.api.TriageController;
import com.shinchoku.shinchokuapp.api.ViewController;
import com.shinchoku.shinchokuapp.api.WorkspaceController;
import com.shinchoku.shinchokuapp.security.OidcLogin;
import com.shinchoku.shinchokuapp.security.TokenService;
import com.shinchoku.shinchokuapp.support.TestDatabase;
import java.net.*;
import java.net.http.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

/**
 * HTTP regression tests against a disposable PostgreSQL database. Test helpers create isolated
 * workspaces/identities, then verify permissions, transactions and response behavior through the
 * real API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "local"})
@Import(ApiIntegrationTests.FakeOidcConfiguration.class)
@SuppressWarnings("unchecked")
class ApiIntegrationTests {

  @Value("${local.server.port}")
  int port;

  @Autowired TestDatabase database;

  @Autowired ObjectMapper objectMapper;

  Session ownerSession;

  String workspaceId, teamId, currentUserId, workspaceApiPath;

  class Session {

    final HttpClient http;

    final String bearerToken;

    String csrfToken;

    Session(String sessionToken, String bearerToken) throws Exception {
      this.bearerToken = bearerToken;
      var cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
      if (sessionToken != null) {
        var sessionCookie = new HttpCookie(TokenService.COOKIE, sessionToken);
        sessionCookie.setPath("/");
        sessionCookie.setVersion(0);
        cookieManager.getCookieStore().add(URI.create(url("/")), sessionCookie);
      }
      http = HttpClient.newBuilder().cookieHandler(cookieManager).build();
      csrfToken = send("GET", "/api/v1/auth/csrf", null, 200).get("token").toString();
    }

    Map<String, Object> send(
        String method, String path, Map<String, Object> requestBody, int expected)
        throws Exception {
      return send(method, path, requestBody, expected, true, "http://localhost:5173");
    }

    Map<String, Object> send(
        String method,
        String path,
        Map<String, Object> requestBody,
        int expected,
        boolean withCsrf,
        String origin)
        throws Exception {
      var requestBuilder =
          HttpRequest.newBuilder(URI.create(url(path)))
              .header("Content-Type", "application/json")
              .header("Origin", origin);
      if (bearerToken != null) {
        requestBuilder.header("Authorization", "Bearer " + bearerToken);
      }
      if (withCsrf && csrfToken != null) {
        requestBuilder.header("X-XSRF-TOKEN", csrfToken);
      }
      var result =
          http.send(
              requestBuilder
                  .method(
                      method,
                      requestBody == null
                          ? HttpRequest.BodyPublishers.noBody()
                          : HttpRequest.BodyPublishers.ofString(
                              objectMapper.writeValueAsString(requestBody)))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(
          expected, result.statusCode(), method + " " + path + " returned " + result.body());
      return result.body().isBlank() ? Map.of() : objectMapper.readValue(result.body(), Map.class);
    }
  }

  String url(String path) {
    return "http://localhost:" + port + path;
  }

  String id(Map<String, Object> row) {
    return row.get("id").toString();
  }

  List<Map<String, Object>> items(Map<String, Object> page) {
    return (List<Map<String, Object>>) page.get("items");
  }

  @BeforeEach
  void setup() throws Exception {
    ownerSession = new Session(null, null);
    currentUserId = id(ownerSession.send("POST", "/api/dev/login", Map.of(), 200));
    workspaceId =
        id(
            ownerSession.send(
                "POST",
                "/api/v1/workspaces",
                parameters("name", "Test", "slug", "test-" + UUID.randomUUID()),
                201));
    workspaceApiPath = "/api/v1/workspaces/" + workspaceId;
    teamId =
        id(
            ownerSession.send(
                "POST",
                workspaceApiPath + "/teams",
                parameters("name", "Engineering", "key", "ENG"),
                201));
  }

  Map<String, Object> issue(String title, Map<String, Object> additionalFields) throws Exception {
    var requestBody = parameters("teamId", teamId, "title", title);
    requestBody.putAll(additionalFields);
    return ownerSession.send("POST", workspaceApiPath + "/issues", requestBody, 201);
  }

  Session actor(String workspaceId, String role) throws Exception {
    UUID newUserId = UUID.randomUUID();
    database.insertReturningRow(
        "users",
        parameters(
            "id", newUserId, "email", newUserId + "@test.local", "display_name", "Test user"));
    database.insertReturningRow(
        "workspace_members",
        parameters("workspace_id", toUuid(workspaceId), "user_id", newUserId, "role", role));
    String rawToken = TokenService.generateToken();
    database.insertReturningRow(
        "sessions",
        parameters(
            "user_id",
            newUserId,
            "token_hash",
            TokenService.hashToken(rawToken),
            "expires_at",
            Timestamp.from(Instant.now().plusSeconds(3600))));
    return new Session(rawToken, null);
  }

  @Test
  void optimisticUpdatesAndActivityAreAtomic() throws Exception {
    var row = issue("Initial", Map.of());
    String path = workspaceApiPath + "/issues/" + id(row);
    var next =
        ownerSession.send("PATCH", path, parameters("expectedVersion", 1, "state", "done"), 200);
    assertEquals(2, next.get("version"));
    assertNotNull(next.get("completedAt"));
    ownerSession.send("PATCH", path, parameters("expectedVersion", 1, "title", "Stale"), 409);
    assertEquals(2, items(ownerSession.send("GET", path + "/activity", null, 200)).size());
    next =
        ownerSession.send(
            "PATCH",
            path,
            parameters("expectedVersion", 2, "state", "todo", "assigneeId", null),
            200);
    assertNull(next.get("completedAt"));
  }

  @Test
  void strictPatchesAndTenantReferencesAreValidated() throws Exception {
    String path = workspaceApiPath + "/issues/" + id(issue("Strict", Map.of()));
    ownerSession.send("PATCH", path, parameters("expectedVersion", 1), 422);
    ownerSession.send(
        "PATCH", path, parameters("expectedVersion", 1, "creatorId", currentUserId), 422);
    ownerSession.send("PATCH", path, parameters("expectedVersion", 1, "title", null), 422);
    ownerSession.send("GET", workspaceApiPath + "/issues?limit=101", null, 422);
    ownerSession.send("GET", workspaceApiPath + "/issues?cursor=bad", null, 400);
    String otherWorkspaceId =
        id(
            ownerSession.send(
                "POST",
                "/api/v1/workspaces",
                parameters("name", "Other", "slug", "other-" + UUID.randomUUID()),
                201));
    ownerSession.send(
        "GET",
        "/api/v1/workspaces/"
            + otherWorkspaceId
            + "/issues/"
            + path.substring(path.lastIndexOf('/') + 1),
        null,
        404);
    String foreignLabel =
        id(
            ownerSession.send(
                "POST",
                "/api/v1/workspaces/" + otherWorkspaceId + "/labels",
                parameters("name", "Foreign"),
                201));
    ownerSession.send(
        "POST",
        workspaceApiPath + "/issues",
        parameters("teamId", teamId, "title", "Invalid", "labelIds", List.of(foreignLabel)),
        422);
  }

  @Test
  void labelsCountsAndCursorsUseTheSameFilters() throws Exception {
    String
        firstLabelId =
            id(
                ownerSession.send(
                    "POST", workspaceApiPath + "/labels", parameters("name", "A"), 201)),
        secondLabelId =
            id(
                ownerSession.send(
                    "POST", workspaceApiPath + "/labels", parameters("name", "B"), 201));
    issue("Both", parameters("labelIds", List.of(firstLabelId, secondLabelId)));
    issue("Only A", parameters("labelIds", List.of(firstLabelId)));
    issue("Neither", Map.of());
    assertEquals(
        1,
        items(
                ownerSession.send(
                    "GET",
                    workspaceApiPath
                        + "/issues?labelIds="
                        + firstLabelId
                        + ","
                        + secondLabelId
                        + "&labelMatch=all",
                    null,
                    200))
            .size());
    assertEquals(
        2,
        items(
                ownerSession.send(
                    "GET",
                    workspaceApiPath
                        + "/issues?labelIds="
                        + firstLabelId
                        + ","
                        + secondLabelId
                        + "&labelMatch=any",
                    null,
                    200))
            .size());
    var view =
        ownerSession.send(
            "POST",
            workspaceApiPath + "/views",
            parameters(
                "name",
                "Both",
                "visibility",
                "workspace",
                "labelMatch",
                "all",
                "labelIds",
                List.of(firstLabelId, secondLabelId)),
            201);
    assertEquals(1, view.get("issueCount"));
    var page = ownerSession.send("GET", workspaceApiPath + "/issues?limit=1", null, 200);
    String first = id(items(page).getFirst());
    var next =
        ownerSession.send(
            "GET",
            workspaceApiPath + "/issues?limit=1&cursor=" + page.get("nextCursor"),
            null,
            200);
    assertNotEquals(first, id(items(next).getFirst()));
    ownerSession.send("DELETE", workspaceApiPath + "/labels/" + firstLabelId, null, 409);
  }

  @Test
  void guestGrantsAreAppliedBeforePaginationAndCounts() throws Exception {
    String projectId =
        id(
            ownerSession.send(
                "POST",
                workspaceApiPath + "/projects",
                parameters("teamId", teamId, "name", "Granted project"),
                201));
    var allowed = issue("Allowed", parameters("projectId", projectId));
    issue("Private", Map.of());
    var guest = actor(workspaceId, "guest");
    String guestId = id(guest.send("GET", "/api/v1/me", null, 200));
    assertTrue(items(guest.send("GET", workspaceApiPath + "/issues", null, 200)).isEmpty());
    String grantId =
        id(
            ownerSession.send(
                "POST",
                workspaceApiPath + "/guest-grants",
                parameters("userId", guestId, "teamId", teamId, "projectId", projectId),
                201));
    assertEquals(
        id(allowed),
        id(items(guest.send("GET", workspaceApiPath + "/issues?limit=1", null, 200)).getFirst()));
    assertEquals(
        1,
        guest
            .send("GET", workspaceApiPath + "/teams/" + teamId + "/insights", null, 200)
            .get("total"));
    guest.send(
        "POST",
        workspaceApiPath + "/issues",
        parameters("teamId", teamId, "title", "Forbidden"),
        403);
    ownerSession.send("DELETE", workspaceApiPath + "/guest-grants/" + grantId, null, 204);
    assertTrue(items(guest.send("GET", workspaceApiPath + "/issues", null, 200)).isEmpty());
  }

  @Test
  void apiKeysNeverExpandPermissionsOrLeakSecrets() throws Exception {
    var key =
        ownerSession.send(
            "POST",
            workspaceApiPath + "/api-keys",
            parameters(
                "name",
                "Read only",
                "scopes",
                List.of("read"),
                "expiresAt",
                Instant.now().plusSeconds(3600).toString()),
            201);
    var client = new Session(null, key.get("token").toString());
    client.send("GET", workspaceApiPath + "/issues", null, 200);
    client.send(
        "POST", workspaceApiPath + "/issues", parameters("teamId", teamId, "title", "No"), 403);
    client.send("GET", "/api/v1/me/sessions", null, 403);
    String serializedResponse =
        objectMapper.writeValueAsString(
            ownerSession.send("GET", workspaceApiPath + "/api-keys", null, 200));
    assertFalse(serializedResponse.contains(key.get("token").toString()));
    assertFalse(serializedResponse.contains("tokenHash"));
    ownerSession.send("DELETE", workspaceApiPath + "/api-keys/" + id(key), null, 204);
    client.send("GET", workspaceApiPath + "/issues", null, 401);
  }

  @Test
  void ownerLifecyclePreservesHistoryAndRevokesAccess() throws Exception {
    ownerSession.send(
        "PATCH", workspaceApiPath + "/members/" + currentUserId, parameters("role", "member"), 409);
    var member = actor(workspaceId, "member");
    String memberId = id(member.send("GET", "/api/v1/me", null, 200));
    ownerSession.send(
        "POST",
        workspaceApiPath + "/teams/" + teamId + "/members",
        parameters("userId", memberId, "role", "member"),
        201);
    var row =
        member.send(
            "POST",
            workspaceApiPath + "/issues",
            parameters("teamId", teamId, "title", "Keep history"),
            201);
    ownerSession.send("DELETE", workspaceApiPath + "/members/" + memberId, null, 204);
    member.send("GET", workspaceApiPath + "/issues", null, 401);
    ownerSession.send("GET", workspaceApiPath + "/issues/" + id(row), null, 200);
  }

  @Test
  void cookieWritesRequireCsrfAndAnAllowedOrigin() throws Exception {
    ownerSession.send(
        "POST",
        workspaceApiPath + "/issues",
        parameters("teamId", teamId, "title", "No csrf"),
        403,
        false,
        "http://localhost:5173");
    ownerSession.send(
        "POST",
        workspaceApiPath + "/issues",
        parameters("teamId", teamId, "title", "Bad origin"),
        403,
        true,
        "https://untrusted.invalid");
  }

  @Test
  void concurrentIssueNumbersAreUnique() throws Exception {
    try (var executor = Executors.newFixedThreadPool(5)) {
      List<Callable<Map<String, Object>>> concurrentTasks = new ArrayList<>();
      for (int taskIndex = 0; taskIndex < 12; taskIndex++) {
        int taskNumber = taskIndex;
        concurrentTasks.add(() -> issue("Parallel " + taskNumber, Map.of()));
      }
      var result = executor.invokeAll(concurrentTasks);
      Set<Object> numbers = new HashSet<>();
      for (var taskResult : result) {
        numbers.add(taskResult.get().get("number"));
      }
      assertEquals(12, numbers.size());
    }
  }

  @Test
  void triageAcceptanceIsConcurrentAndIdempotent() throws Exception {
    String sourceTeamId =
        id(
            ownerSession.send(
                "POST",
                workspaceApiPath + "/teams",
                parameters("name", "Source", "key", "SRC"),
                201));
    String request =
        id(
            ownerSession.send(
                "POST",
                workspaceApiPath + "/teams/" + teamId + "/triage",
                parameters("sourceTeamId", sourceTeamId, "title", "Accept once"),
                201));
    String path = workspaceApiPath + "/teams/" + teamId + "/triage/" + request + "/decision";
    try (var executor = Executors.newFixedThreadPool(2)) {
      var concurrentTasks =
          List.<Callable<Map<String, Object>>>of(
              () -> ownerSession.send("POST", path, parameters("decision", "accepted"), 200),
              () -> ownerSession.send("POST", path, parameters("decision", "accepted"), 200));
      var results = executor.invokeAll(concurrentTasks);
      assertEquals(
          results.get(0).get().get("acceptedIssueId"), results.get(1).get().get("acceptedIssueId"));
    }
    assertEquals(
        1, items(ownerSession.send("GET", workspaceApiPath + "/issues", null, 200)).size());
    ownerSession.send("POST", path, parameters("decision", "declined"), 409);
  }

  @Test
  void cyclesRejectOverlapsAndInvalidWindows() throws Exception {
    String path = workspaceApiPath + "/teams/" + teamId + "/cycles";
    ownerSession.send(
        "POST",
        path,
        parameters("number", 1, "startsOn", "2026-01-01", "endsOn", "2026-01-14"),
        201);
    ownerSession.send(
        "POST",
        path,
        parameters("number", 2, "startsOn", "2026-01-14", "endsOn", "2026-01-27"),
        409);
    ownerSession.send(
        "POST",
        path,
        parameters("number", 2, "startsOn", "2026-01-15", "endsOn", "2026-01-28"),
        201);
    ownerSession.send(
        "POST",
        path,
        parameters("number", 3, "startsOn", "2026-02-01", "endsOn", "2026-02-14"),
        201);
    ownerSession.send(
        "POST",
        path,
        parameters("number", 4, "startsOn", "2026-02-15", "endsOn", "2026-02-27"),
        422);
  }

  @TestConfiguration
  static class FakeOidcConfiguration {

    @Bean
    ClientRegistrationRepository testClients() {
      var registration =
          ClientRegistration.withRegistrationId("google")
              .clientId("test-client")
              .clientSecret("test-secret")
              .authorizationGrantType(
                  org.springframework.security.oauth2.core.AuthorizationGrantType
                      .AUTHORIZATION_CODE)
              .redirectUri("http://localhost/login/oauth2/code/google")
              .scope("openid", "email", "profile")
              .authorizationUri("https://idp.invalid/authorize")
              .tokenUri("https://idp.invalid/token")
              .jwkSetUri("https://idp.invalid/jwks")
              .userInfoUri("https://idp.invalid/userinfo")
              .userNameAttributeName("sub")
              .build();
      return new InMemoryClientRegistrationRepository(registration);
    }
  }

  @Test
  void invitationsRequireMatchingEmailAndAreIdempotent() throws Exception {
    var guest = actor(workspaceId, "guest");
    String guestId = id(guest.send("GET", "/api/v1/me", null, 200));
    String rawToken = TokenService.generateToken();
    String email =
        database
            .findOne(
                "SELECT email FROM users WHERE id=:userId", parameters("userId", toUuid(guestId)))
            .get("email")
            .toString();
    database.insertReturningRow(
        "invitations",
        parameters(
            "workspace_id",
            toUuid(workspaceId),
            "invited_by",
            toUuid(currentUserId),
            "email",
            email,
            "role",
            "member",
            "token_hash",
            TokenService.hashToken(rawToken),
            "expires_at",
            Timestamp.from(Instant.now().plusSeconds(3600))));
    ownerSession.send("POST", "/api/v1/invitations/accept", parameters("token", rawToken), 403);
    guest.send("POST", "/api/v1/invitations/accept", parameters("token", rawToken), 200);
    guest.send("POST", "/api/v1/invitations/accept", parameters("token", rawToken), 200);
    assertEquals(
        "guest",
        database
            .findOne(
                "SELECT role FROM workspace_members WHERE workspace_id=:workspaceId AND"
                    + " user_id=:userId",
                parameters("workspaceId", toUuid(workspaceId), "userId", toUuid(guestId)))
            .get("role"));
    ownerSession.send(
        "PATCH", workspaceApiPath + "/members/" + guestId, parameters("role", "viewer"), 200);
  }

  @Test
  void sessionsAndConnectionsSupportRevocationWithoutLeakingHashes() throws Exception {
    String rawToken = TokenService.generateToken();
    var session =
        database.insertReturningRow(
            "sessions",
            parameters(
                "user_id",
                toUuid(currentUserId),
                "token_hash",
                TokenService.hashToken(rawToken),
                "expires_at",
                Timestamp.from(Instant.now().plusSeconds(3600))));
    String serializedResponse =
        objectMapper.writeValueAsString(ownerSession.send("GET", "/api/v1/me/sessions", null, 200));
    assertFalse(serializedResponse.contains("tokenHash"));
    ownerSession.send("DELETE", "/api/v1/me/sessions/" + id(session), null, 204);
    var first =
        database.insertReturningRow(
            "oauth_connections",
            parameters(
                "user_id",
                toUuid(currentUserId),
                "provider",
                "github",
                "provider_subject",
                UUID.randomUUID().toString()));
    var second =
        database.insertReturningRow(
            "oauth_connections",
            parameters(
                "user_id",
                toUuid(currentUserId),
                "provider",
                "google",
                "provider_subject",
                UUID.randomUUID().toString()));
    ownerSession.send("DELETE", "/api/v1/me/connections/" + id(first), null, 204);
    ownerSession.send("DELETE", "/api/v1/me/connections/" + id(second), null, 409);
    database.executeUpdate(
        "DELETE FROM oauth_connections WHERE user_id=:userId",
        parameters("userId", toUuid(currentUserId)));
  }

  @Test
  void connectionStartChecksRedirectAllowlistAndUsesPkce() throws Exception {
    ownerSession.send(
        "POST",
        "/api/v1/me/connections/authorize?provider=google",
        parameters("redirectUri", "https://attacker.invalid"),
        422);
    assertEquals(
        "http://localhost:5173/oauth2/authorization/google",
        ownerSession
            .send(
                "POST",
                "/api/v1/me/connections/authorize?provider=google",
                parameters("redirectUri", "http://localhost:5173"),
                200)
            .get("authorizationUrl"));
    var repository = new FakeOidcConfiguration().testClients();
    var resolver =
        new DefaultOAuth2AuthorizationRequestResolver(repository, "/oauth2/authorization");
    resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    var request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
    request.setServletPath("/oauth2/authorization/google");
    var authorization = resolver.resolve(request);
    assertNotNull(authorization.getState());
    assertNotNull(authorization.getAdditionalParameters().get("code_challenge"));
    assertEquals("S256", authorization.getAdditionalParameters().get("code_challenge_method"));
  }

  @Test
  void concurrentCyclesCannotOverlap() throws Exception {
    String path = workspaceApiPath + "/teams/" + teamId + "/cycles";
    try (var executor = Executors.newFixedThreadPool(2)) {
      List<Callable<Integer>> concurrentTasks = new ArrayList<>();
      for (int taskIndex = 1; taskIndex <= 2; taskIndex++) {
        int number = taskIndex;
        concurrentTasks.add(
            () -> {
              var request =
                  HttpRequest.newBuilder(URI.create(url(path)))
                      .header("Origin", "http://localhost:5173")
                      .header("Content-Type", "application/json")
                      .header("X-XSRF-TOKEN", ownerSession.csrfToken)
                      .POST(
                          HttpRequest.BodyPublishers.ofString(
                              objectMapper.writeValueAsString(
                                  parameters(
                                      "number",
                                      number,
                                      "startsOn",
                                      "2026-05-01",
                                      "endsOn",
                                      "2026-05-14"))))
                      .build();
              return ownerSession
                  .http
                  .send(request, HttpResponse.BodyHandlers.ofString())
                  .statusCode();
            });
      }
      List<Integer> responseStatusCodes = new ArrayList<>();
      for (var result : executor.invokeAll(concurrentTasks)) {
        responseStatusCodes.add(result.get());
      }
      Collections.sort(responseStatusCodes);
      assertEquals(List.of(201, 409), responseStatusCodes);
    }
  }

  @Test
  void allContractOperationsHaveAnExplicitControllerMethod() {
    Set<String> implemented = new HashSet<>();
    for (Class<?> controller :
        List.of(
            WorkspaceController.class,
            PlanningController.class,
            IssueController.class,
            ViewController.class,
            TriageController.class,
            ProfileController.class,
            InsightsController.class)) {
      for (var method : controller.getDeclaredMethods()) {
        var operation = method.getAnnotation(ContractOperation.class);
        if (operation != null) {
          implemented.add(operation.value());
        }
      }
    }
    var paths =
        (Map<String, Map<String, Map<String, Object>>>) contextContract.document().get("paths");
    Set<String> expected = new HashSet<>();
    paths
        .values()
        .forEach(
            methods ->
                methods
                    .values()
                    .forEach(operation -> expected.add(operation.get("operationId").toString())));
    assertEquals(expected, implemented);
    assertEquals(71, implemented.size());
  }

  @Autowired Contract contextContract;

  @Autowired OidcLogin oidcLogin;

  @Test
  void oidcLinkingRequiresAnUnexpiredOriginalSession() throws Exception {
    var request = new MockHttpServletRequest();
    request.getSession().setAttribute("linkUser", currentUserId);
    var response = new MockHttpServletResponse();
    var claims =
        parameters(
            "sub",
            "new-subject-" + UUID.randomUUID(),
            "email",
            "verified-" + UUID.randomUUID() + "@test.local",
            "email_verified",
            true,
            "name",
            "Verified user");
    var idToken =
        new OidcIdToken("test-id-token", Instant.now(), Instant.now().plusSeconds(300), claims);
    var principal = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
    var authentication =
        new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
    oidcLogin.onAuthenticationSuccess(request, response, authentication);
    assertEquals(401, response.getStatus());
    assertEquals(
        0,
        database.count(
            "SELECT count(*) FROM oauth_connections WHERE provider_subject=:s",
            parameters("s", claims.get("sub"))));
  }

  @Test
  void unknownRoutesReturnTheSharedErrorEnvelope() throws Exception {
    var result = ownerSession.send("GET", "/api/v1/does-not-exist", null, 404);
    assertEquals("NOT_FOUND", result.get("code"));
    assertNotNull(result.get("requestId"));
  }

  @Test
  void typedPatchKeepsOmittedFieldsAndClearsExplicitNull() throws Exception {
    var created = issue("Before", parameters("assigneeId", currentUserId));
    String path = workspaceApiPath + "/issues/" + id(created);
    var renamed =
        ownerSession.send("PATCH", path, parameters("expectedVersion", 1, "title", "After"), 200);
    assertEquals(currentUserId, renamed.get("assigneeId"));
    var cleared =
        ownerSession.send(
            "PATCH",
            path,
            parameters("expectedVersion", 2, "assigneeId", null, "labelIds", List.of()),
            200);
    assertNull(cleared.get("assigneeId"));
    assertEquals(List.of(), cleared.get("labelIds"));
    assertEquals(3, cleared.get("version"));
  }

  @Test
  void concurrentEntityEditsAllowOneVersionWinner() throws Exception {
    String path = workspaceApiPath + "/issues/" + id(issue("Contended", Map.of()));
    try (var executor = Executors.newFixedThreadPool(2)) {
      List<Callable<Integer>> tasks = new ArrayList<>();
      for (int index = 0; index < 2; index++) {
        int editorNumber = index;
        tasks.add(
            () -> {
              var request =
                  HttpRequest.newBuilder(URI.create(url(path)))
                      .header("Origin", "http://localhost:5173")
                      .header("Content-Type", "application/json")
                      .header("X-XSRF-TOKEN", ownerSession.csrfToken)
                      .method(
                          "PATCH",
                          HttpRequest.BodyPublishers.ofString(
                              objectMapper.writeValueAsString(
                                  parameters(
                                      "expectedVersion", 1, "title", "Editor " + editorNumber))))
                      .build();
              return ownerSession
                  .http
                  .send(request, HttpResponse.BodyHandlers.ofString())
                  .statusCode();
            });
      }
      List<Integer> statuses = new ArrayList<>();
      for (var result : executor.invokeAll(tasks)) {
        statuses.add(result.get());
      }
      Collections.sort(statuses);
      assertEquals(List.of(200, 409), statuses);
    }
    assertEquals(2, ownerSession.send("GET", path, null, 200).get("version"));
    assertEquals(2, items(ownerSession.send("GET", path + "/activity", null, 200)).size());
  }

  @Test
  void issueSearchTreatsSqlWildcardsAsLiteralText() throws Exception {
    issue("Ship_100%", Map.of());
    issue("ShipX100percent", Map.of());
    var page = ownerSession.send("GET", workspaceApiPath + "/issues?q=Ship_100%25", null, 200);
    assertEquals(1, items(page).size());
    assertEquals("Ship_100%", items(page).getFirst().get("title"));
  }
}
