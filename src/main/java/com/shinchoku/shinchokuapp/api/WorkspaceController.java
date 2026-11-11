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
 * Typed HTTP boundary for workspace operations. Business rules and transactions belong to the
 * service.
 */
@RestController
@RequestMapping("/api/v1")
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  public WorkspaceController(WorkspaceService workspaceService) {
    this.workspaceService = workspaceService;
  }

  @ContractOperation("listWorkspaces")
  @GetMapping("/workspaces")
  public ResponseEntity<PageResponse<WorkspaceResponse>> listWorkspaces(
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(workspaceService.listWorkspaces(new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createWorkspace")
  @PostMapping("/workspaces")
  public ResponseEntity<WorkspaceResponse> createWorkspace(
      @Valid @RequestBody WorkspaceCreateRequest requestBody) {
    return ResponseEntity.status(201).body(workspaceService.createWorkspace(requestBody));
  }

  @ContractOperation("getWorkspace")
  @GetMapping("/workspaces/{workspaceId}")
  public ResponseEntity<WorkspaceResponse> getWorkspace(
      @PathVariable("workspaceId") UUID workspaceId) {
    return ResponseEntity.status(200).body(workspaceService.getWorkspace(workspaceId));
  }

  @ContractOperation("updateWorkspace")
  @PatchMapping("/workspaces/{workspaceId}")
  public ResponseEntity<WorkspaceResponse> updateWorkspace(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody WorkspacePatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(workspaceService.updateWorkspace(workspaceId, requestBody));
  }

  @ContractOperation("listMembers")
  @GetMapping("/workspaces/{workspaceId}/members")
  public ResponseEntity<PageResponse<MemberResponse>> listMembers(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(workspaceService.listMembers(workspaceId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("updateMember")
  @PatchMapping("/workspaces/{workspaceId}/members/{userId}")
  public ResponseEntity<MemberResponse> updateMember(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("userId") UUID userId,
      @Valid @RequestBody MemberPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(workspaceService.updateMember(workspaceId, userId, requestBody));
  }

  @ContractOperation("removeMember")
  @DeleteMapping("/workspaces/{workspaceId}/members/{userId}")
  public ResponseEntity<Void> removeMember(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("userId") UUID userId) {
    workspaceService.removeMember(workspaceId, userId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("listInvitations")
  @GetMapping("/workspaces/{workspaceId}/invitations")
  public ResponseEntity<PageResponse<InvitationResponse>> listInvitations(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(workspaceService.listInvitations(workspaceId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("inviteMember")
  @PostMapping("/workspaces/{workspaceId}/invitations")
  public ResponseEntity<InvitationResponse> inviteMember(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody InvitationCreateRequest requestBody) {
    return ResponseEntity.status(201).body(workspaceService.inviteMember(workspaceId, requestBody));
  }

  @ContractOperation("revokeInvitation")
  @DeleteMapping("/workspaces/{workspaceId}/invitations/{invitationId}")
  public ResponseEntity<Void> revokeInvitation(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("invitationId") UUID invitationId) {
    workspaceService.revokeInvitation(workspaceId, invitationId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("acceptInvitation")
  @PostMapping("/invitations/accept")
  public ResponseEntity<WorkspaceResponse> acceptInvitation(
      @Valid @RequestBody InvitationAcceptRequest requestBody) {
    return ResponseEntity.status(200).body(workspaceService.acceptInvitation(requestBody));
  }

  @ContractOperation("listGuestGrants")
  @GetMapping("/workspaces/{workspaceId}/guest-grants")
  public ResponseEntity<PageResponse<GuestGrantResponse>> listGuestGrants(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(workspaceService.listGuestGrants(workspaceId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("grantGuestAccess")
  @PostMapping("/workspaces/{workspaceId}/guest-grants")
  public ResponseEntity<GuestGrantResponse> grantGuestAccess(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody GuestGrantCreateRequest requestBody) {
    return ResponseEntity.status(201)
        .body(workspaceService.grantGuestAccess(workspaceId, requestBody));
  }

  @ContractOperation("revokeGuestAccess")
  @DeleteMapping("/workspaces/{workspaceId}/guest-grants/{grantId}")
  public ResponseEntity<Void> revokeGuestAccess(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("grantId") UUID grantId) {
    workspaceService.revokeGuestAccess(workspaceId, grantId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("listTeams")
  @GetMapping("/workspaces/{workspaceId}/teams")
  public ResponseEntity<PageResponse<TeamResponse>> listTeams(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(workspaceService.listTeams(workspaceId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createTeam")
  @PostMapping("/workspaces/{workspaceId}/teams")
  public ResponseEntity<TeamResponse> createTeam(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody TeamCreateRequest requestBody) {
    return ResponseEntity.status(201).body(workspaceService.createTeam(workspaceId, requestBody));
  }

  @ContractOperation("getTeam")
  @GetMapping("/workspaces/{workspaceId}/teams/{teamId}")
  public ResponseEntity<TeamResponse> getTeam(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("teamId") UUID teamId) {
    return ResponseEntity.status(200).body(workspaceService.getTeam(workspaceId, teamId));
  }

  @ContractOperation("updateTeam")
  @PatchMapping("/workspaces/{workspaceId}/teams/{teamId}")
  public ResponseEntity<TeamResponse> updateTeam(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @Valid @RequestBody TeamPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(workspaceService.updateTeam(workspaceId, teamId, requestBody));
  }

  @ContractOperation("listTeamMembers")
  @GetMapping("/workspaces/{workspaceId}/teams/{teamId}/members")
  public ResponseEntity<PageResponse<TeamMemberResponse>> listTeamMembers(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(
            workspaceService.listTeamMembers(
                workspaceId, teamId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("addTeamMember")
  @PostMapping("/workspaces/{workspaceId}/teams/{teamId}/members")
  public ResponseEntity<TeamMemberResponse> addTeamMember(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @Valid @RequestBody TeamMemberRequest requestBody) {
    return ResponseEntity.status(201)
        .body(workspaceService.addTeamMember(workspaceId, teamId, requestBody));
  }

  @ContractOperation("removeTeamMember")
  @DeleteMapping("/workspaces/{workspaceId}/teams/{teamId}/members/{userId}")
  public ResponseEntity<Void> removeTeamMember(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @PathVariable("userId") UUID userId) {
    workspaceService.removeTeamMember(workspaceId, teamId, userId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("updateTeamMember")
  @PatchMapping("/workspaces/{workspaceId}/teams/{teamId}/members/{userId}")
  public ResponseEntity<TeamMemberResponse> updateTeamMember(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @PathVariable("userId") UUID userId,
      @Valid @RequestBody TeamMemberPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(workspaceService.updateTeamMember(workspaceId, teamId, userId, requestBody));
  }

  @ContractOperation("getSecurity")
  @GetMapping("/workspaces/{workspaceId}/security")
  public ResponseEntity<SecurityResponse> getSecurity(
      @PathVariable("workspaceId") UUID workspaceId) {
    return ResponseEntity.status(200).body(workspaceService.getSecurity(workspaceId));
  }

  @ContractOperation("updateSecurity")
  @PatchMapping("/workspaces/{workspaceId}/security")
  public ResponseEntity<SecurityResponse> updateSecurity(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody SecurityPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(workspaceService.updateSecurity(workspaceId, requestBody));
  }
}
