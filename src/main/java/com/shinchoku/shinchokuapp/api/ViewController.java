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
 * Typed HTTP boundary for view operations. Business rules and transactions belong to the service.
 */
@RestController
@RequestMapping("/api/v1")
public class ViewController {

  private final ViewService viewService;

  public ViewController(ViewService viewService) {
    this.viewService = viewService;
  }

  @ContractOperation("listViews")
  @GetMapping("/workspaces/{workspaceId}/views")
  public ResponseEntity<PageResponse<SavedViewResponse>> listViews(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(viewService.listViews(workspaceId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createView")
  @PostMapping("/workspaces/{workspaceId}/views")
  public ResponseEntity<SavedViewResponse> createView(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody SavedViewCreateRequest requestBody) {
    return ResponseEntity.status(201).body(viewService.createView(workspaceId, requestBody));
  }

  @ContractOperation("getView")
  @GetMapping("/workspaces/{workspaceId}/views/{viewId}")
  public ResponseEntity<SavedViewResponse> getView(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("viewId") UUID viewId) {
    return ResponseEntity.status(200).body(viewService.getView(workspaceId, viewId));
  }

  @ContractOperation("updateView")
  @PatchMapping("/workspaces/{workspaceId}/views/{viewId}")
  public ResponseEntity<SavedViewResponse> updateView(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("viewId") UUID viewId,
      @Valid @RequestBody SavedViewPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(viewService.updateView(workspaceId, viewId, requestBody));
  }

  @ContractOperation("deleteView")
  @DeleteMapping("/workspaces/{workspaceId}/views/{viewId}")
  public ResponseEntity<Void> deleteView(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("viewId") UUID viewId) {
    viewService.deleteView(workspaceId, viewId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("listViewIssues")
  @GetMapping("/workspaces/{workspaceId}/views/{viewId}/issues")
  public ResponseEntity<PageResponse<IssueResponse>> listViewIssues(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("viewId") UUID viewId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(viewService.listViewIssues(workspaceId, viewId, new CursorRequest(cursor, limit)));
  }
}
