package com.shinchoku.shinchokuapp.api;

import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.service.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Typed HTTP boundary for insights operations. Business rules and transactions belong to the
 * service.
 */
@RestController
@RequestMapping("/api/v1")
public class InsightsController {

  private final InsightsService insightsService;

  public InsightsController(InsightsService insightsService) {
    this.insightsService = insightsService;
  }

  @ContractOperation("getInsights")
  @GetMapping("/workspaces/{workspaceId}/teams/{teamId}/insights")
  public ResponseEntity<InsightsResponse> getInsights(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @RequestParam(name = "cycleId", required = false) UUID cycleId) {
    return ResponseEntity.status(200)
        .body(insightsService.getInsights(workspaceId, teamId, cycleId));
  }
}
