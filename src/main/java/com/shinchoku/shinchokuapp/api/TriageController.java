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
 * Typed HTTP boundary for triage operations. Business rules and transactions belong to the service.
 */
@RestController
@RequestMapping("/api/v1")
public class TriageController {

  private final TriageService triageService;

  public TriageController(TriageService triageService) {
    this.triageService = triageService;
  }

  @ContractOperation("listTriage")
  @GetMapping("/workspaces/{workspaceId}/teams/{teamId}/triage")
  public ResponseEntity<PageResponse<TriageResponse>> listTriage(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @RequestParam(name = "state", required = false) TriageState state,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(
            triageService.listTriage(workspaceId, teamId, state, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("submitTriage")
  @PostMapping("/workspaces/{workspaceId}/teams/{teamId}/triage")
  public ResponseEntity<TriageResponse> submitTriage(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @Valid @RequestBody TriageCreateRequest requestBody) {
    return ResponseEntity.status(201)
        .body(triageService.submitTriage(workspaceId, teamId, requestBody));
  }

  @ContractOperation("decideTriage")
  @PostMapping("/workspaces/{workspaceId}/teams/{teamId}/triage/{requestId}/decision")
  public ResponseEntity<TriageResponse> decideTriage(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @PathVariable("requestId") UUID requestId,
      @Valid @RequestBody TriageDecisionRequest requestBody) {
    return ResponseEntity.status(200)
        .body(triageService.decideTriage(workspaceId, teamId, requestId, requestBody));
  }
}
