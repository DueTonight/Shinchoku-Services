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
 * Typed HTTP boundary for issue operations. Business rules and transactions belong to the service.
 */
@RestController
@RequestMapping("/api/v1")
public class IssueController {

  private final IssueService issueService;

  public IssueController(IssueService issueService) {
    this.issueService = issueService;
  }

  @ContractOperation("listIssues")
  @GetMapping("/workspaces/{workspaceId}/issues")
  public ResponseEntity<PageResponse<IssueResponse>> listIssues(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "teamIds", required = false) List<UUID> teamIds,
      @RequestParam(name = "states", required = false) List<IssueState> states,
      @RequestParam(name = "labelIds", required = false) List<UUID> labelIds,
      @RequestParam(name = "labelMatch", defaultValue = "any") LabelMatch labelMatch,
      @RequestParam(name = "assigneeId", required = false) String assigneeId,
      @RequestParam(name = "cycleId", required = false) UUID cycleId,
      @RequestParam(name = "projectId", required = false) UUID projectId,
      @RequestParam(name = "q", required = false) String search,
      @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(
            issueService.listIssues(
                workspaceId,
                new IssueFilters(
                    teamIds,
                    states,
                    labelIds,
                    labelMatch,
                    assigneeId,
                    cycleId,
                    projectId,
                    search,
                    includeArchived),
                new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createIssue")
  @PostMapping("/workspaces/{workspaceId}/issues")
  public ResponseEntity<IssueResponse> createIssue(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody IssueCreateRequest requestBody) {
    return ResponseEntity.status(201).body(issueService.createIssue(workspaceId, requestBody));
  }

  @ContractOperation("getIssue")
  @GetMapping("/workspaces/{workspaceId}/issues/{issueId}")
  public ResponseEntity<IssueResponse> getIssue(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("issueId") UUID issueId) {
    return ResponseEntity.status(200).body(issueService.getIssue(workspaceId, issueId));
  }

  @ContractOperation("updateIssue")
  @PatchMapping("/workspaces/{workspaceId}/issues/{issueId}")
  public ResponseEntity<IssueResponse> updateIssue(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("issueId") UUID issueId,
      @Valid @RequestBody IssuePatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(issueService.updateIssue(workspaceId, issueId, requestBody));
  }

  @ContractOperation("listComments")
  @GetMapping("/workspaces/{workspaceId}/issues/{issueId}/comments")
  public ResponseEntity<PageResponse<CommentResponse>> listComments(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("issueId") UUID issueId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(issueService.listComments(workspaceId, issueId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createComment")
  @PostMapping("/workspaces/{workspaceId}/issues/{issueId}/comments")
  public ResponseEntity<CommentResponse> createComment(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("issueId") UUID issueId,
      @Valid @RequestBody CommentCreateRequest requestBody) {
    return ResponseEntity.status(201)
        .body(issueService.createComment(workspaceId, issueId, requestBody));
  }

  @ContractOperation("updateComment")
  @PatchMapping("/workspaces/{workspaceId}/issues/{issueId}/comments/{commentId}")
  public ResponseEntity<CommentResponse> updateComment(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("issueId") UUID issueId,
      @PathVariable("commentId") UUID commentId,
      @Valid @RequestBody CommentPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(issueService.updateComment(workspaceId, issueId, commentId, requestBody));
  }

  @ContractOperation("deleteComment")
  @DeleteMapping("/workspaces/{workspaceId}/issues/{issueId}/comments/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("issueId") UUID issueId,
      @PathVariable("commentId") UUID commentId) {
    issueService.deleteComment(workspaceId, issueId, commentId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("listActivity")
  @GetMapping("/workspaces/{workspaceId}/issues/{issueId}/activity")
  public ResponseEntity<PageResponse<ActivityResponse>> listActivity(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("issueId") UUID issueId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(issueService.listActivity(workspaceId, issueId, new CursorRequest(cursor, limit)));
  }
}
