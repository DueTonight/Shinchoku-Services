package com.shinchoku.shinchokuapp.api;

import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.service.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Typed HTTP boundary for profile operations. Business rules and transactions belong to the
 * service.
 */
@RestController
@RequestMapping("/api/v1")
public class ProfileController {

  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @ContractOperation("getProfile")
  @GetMapping("/me")
  public ResponseEntity<ProfileResponse> getProfile() {
    return ResponseEntity.status(200).body(profileService.getProfile());
  }

  @ContractOperation("updateProfile")
  @PatchMapping("/me")
  public ResponseEntity<ProfileResponse> updateProfile(
      @Valid @RequestBody ProfilePatchRequest requestBody) {
    return ResponseEntity.status(200).body(profileService.updateProfile(requestBody));
  }

  @ContractOperation("listSessions")
  @GetMapping("/me/sessions")
  public ResponseEntity<PageResponse<SessionResponse>> listSessions(
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(profileService.listSessions(new CursorRequest(cursor, limit)));
  }

  @ContractOperation("revokeSession")
  @DeleteMapping("/me/sessions/{sessionId}")
  public ResponseEntity<Void> revokeSession(
      @PathVariable("sessionId") UUID sessionId, HttpServletResponse response) {
    profileService.revokeSession(sessionId, response);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("logout")
  @PostMapping("/auth/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
    profileService.logout(request, response);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("listConnections")
  @GetMapping("/me/connections")
  public ResponseEntity<PageResponse<ConnectionResponse>> listConnections(
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(profileService.listConnections(new CursorRequest(cursor, limit)));
  }

  @ContractOperation("disconnectProvider")
  @DeleteMapping("/me/connections/{connectionId}")
  public ResponseEntity<Void> disconnectProvider(@PathVariable("connectionId") UUID connectionId) {
    profileService.disconnectProvider(connectionId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("startConnection")
  @PostMapping("/me/connections/authorize")
  public ResponseEntity<AuthorizationUrlResponse> startConnection(
      @RequestParam(name = "provider", required = true) String provider,
      @Valid @RequestBody ConnectionStartRequest requestBody,
      HttpServletRequest request) {
    return ResponseEntity.status(200)
        .body(profileService.startConnection(provider, requestBody, request));
  }

  @ContractOperation("listApiKeys")
  @GetMapping("/workspaces/{workspaceId}/api-keys")
  public ResponseEntity<PageResponse<ApiKeyResponse>> listApiKeys(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(profileService.listApiKeys(workspaceId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createApiKey")
  @PostMapping("/workspaces/{workspaceId}/api-keys")
  public ResponseEntity<ApiKeyIssuedResponse> createApiKey(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody ApiKeyCreateRequest requestBody) {
    return ResponseEntity.status(201).body(profileService.createApiKey(workspaceId, requestBody));
  }

  @ContractOperation("revokeApiKey")
  @DeleteMapping("/workspaces/{workspaceId}/api-keys/{keyId}")
  public ResponseEntity<Void> revokeApiKey(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("keyId") UUID keyId) {
    profileService.revokeApiKey(workspaceId, keyId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("listGithubRepositories")
  @GetMapping("/workspaces/{workspaceId}/github/repositories")
  public ResponseEntity<PageResponse<RepositoryResponse>> listGithubRepositories(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "teamId", required = false) UUID teamId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(
            profileService.listGithubRepositories(
                workspaceId, teamId, new CursorRequest(cursor, limit)));
  }
}
