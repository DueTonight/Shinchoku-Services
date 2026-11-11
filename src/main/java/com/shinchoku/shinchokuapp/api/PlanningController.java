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
 * Typed HTTP boundary for planning operations. Business rules and transactions belong to the
 * service.
 */
@RestController
@RequestMapping("/api/v1")
public class PlanningController {

  private final PlanningService planningService;

  public PlanningController(PlanningService planningService) {
    this.planningService = planningService;
  }

  @ContractOperation("listProjects")
  @GetMapping("/workspaces/{workspaceId}/projects")
  public ResponseEntity<PageResponse<ProjectResponse>> listProjects(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "teamId", required = false) UUID teamId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(planningService.listProjects(workspaceId, teamId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createProject")
  @PostMapping("/workspaces/{workspaceId}/projects")
  public ResponseEntity<ProjectResponse> createProject(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody ProjectCreateRequest requestBody) {
    return ResponseEntity.status(201).body(planningService.createProject(workspaceId, requestBody));
  }

  @ContractOperation("getProject")
  @GetMapping("/workspaces/{workspaceId}/projects/{projectId}")
  public ResponseEntity<ProjectResponse> getProject(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("projectId") UUID projectId) {
    return ResponseEntity.status(200).body(planningService.getProject(workspaceId, projectId));
  }

  @ContractOperation("updateProject")
  @PatchMapping("/workspaces/{workspaceId}/projects/{projectId}")
  public ResponseEntity<ProjectResponse> updateProject(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("projectId") UUID projectId,
      @Valid @RequestBody ProjectPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(planningService.updateProject(workspaceId, projectId, requestBody));
  }

  @ContractOperation("listMilestones")
  @GetMapping("/workspaces/{workspaceId}/projects/{projectId}/milestones")
  public ResponseEntity<PageResponse<MilestoneResponse>> listMilestones(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("projectId") UUID projectId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(
            planningService.listMilestones(
                workspaceId, projectId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createMilestone")
  @PostMapping("/workspaces/{workspaceId}/projects/{projectId}/milestones")
  public ResponseEntity<MilestoneResponse> createMilestone(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("projectId") UUID projectId,
      @Valid @RequestBody MilestoneCreateRequest requestBody) {
    return ResponseEntity.status(201)
        .body(planningService.createMilestone(workspaceId, projectId, requestBody));
  }

  @ContractOperation("updateMilestone")
  @PatchMapping("/workspaces/{workspaceId}/projects/{projectId}/milestones/{milestoneId}")
  public ResponseEntity<MilestoneResponse> updateMilestone(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("projectId") UUID projectId,
      @PathVariable("milestoneId") UUID milestoneId,
      @Valid @RequestBody MilestonePatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(planningService.updateMilestone(workspaceId, projectId, milestoneId, requestBody));
  }

  @ContractOperation("deleteMilestone")
  @DeleteMapping("/workspaces/{workspaceId}/projects/{projectId}/milestones/{milestoneId}")
  public ResponseEntity<Void> deleteMilestone(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("projectId") UUID projectId,
      @PathVariable("milestoneId") UUID milestoneId) {
    planningService.deleteMilestone(workspaceId, projectId, milestoneId);
    return ResponseEntity.noContent().build();
  }

  @ContractOperation("listCycles")
  @GetMapping("/workspaces/{workspaceId}/teams/{teamId}/cycles")
  public ResponseEntity<PageResponse<CycleResponse>> listCycles(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(planningService.listCycles(workspaceId, teamId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createCycle")
  @PostMapping("/workspaces/{workspaceId}/teams/{teamId}/cycles")
  public ResponseEntity<CycleResponse> createCycle(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @Valid @RequestBody CycleCreateRequest requestBody) {
    return ResponseEntity.status(201)
        .body(planningService.createCycle(workspaceId, teamId, requestBody));
  }

  @ContractOperation("getCycle")
  @GetMapping("/workspaces/{workspaceId}/teams/{teamId}/cycles/{cycleId}")
  public ResponseEntity<CycleResponse> getCycle(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @PathVariable("cycleId") UUID cycleId) {
    return ResponseEntity.status(200).body(planningService.getCycle(workspaceId, teamId, cycleId));
  }

  @ContractOperation("updateCycle")
  @PatchMapping("/workspaces/{workspaceId}/teams/{teamId}/cycles/{cycleId}")
  public ResponseEntity<CycleResponse> updateCycle(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("teamId") UUID teamId,
      @PathVariable("cycleId") UUID cycleId,
      @Valid @RequestBody CyclePatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(planningService.updateCycle(workspaceId, teamId, cycleId, requestBody));
  }

  @ContractOperation("listLabels")
  @GetMapping("/workspaces/{workspaceId}/labels")
  public ResponseEntity<PageResponse<LabelResponse>> listLabels(
      @PathVariable("workspaceId") UUID workspaceId,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    return ResponseEntity.status(200)
        .body(planningService.listLabels(workspaceId, new CursorRequest(cursor, limit)));
  }

  @ContractOperation("createLabel")
  @PostMapping("/workspaces/{workspaceId}/labels")
  public ResponseEntity<LabelResponse> createLabel(
      @PathVariable("workspaceId") UUID workspaceId,
      @Valid @RequestBody LabelCreateRequest requestBody) {
    return ResponseEntity.status(201).body(planningService.createLabel(workspaceId, requestBody));
  }

  @ContractOperation("updateLabel")
  @PatchMapping("/workspaces/{workspaceId}/labels/{labelId}")
  public ResponseEntity<LabelResponse> updateLabel(
      @PathVariable("workspaceId") UUID workspaceId,
      @PathVariable("labelId") UUID labelId,
      @Valid @RequestBody LabelPatchRequest requestBody) {
    return ResponseEntity.status(200)
        .body(planningService.updateLabel(workspaceId, labelId, requestBody));
  }

  @ContractOperation("deleteLabel")
  @DeleteMapping("/workspaces/{workspaceId}/labels/{labelId}")
  public ResponseEntity<Void> deleteLabel(
      @PathVariable("workspaceId") UUID workspaceId, @PathVariable("labelId") UUID labelId) {
    planningService.deleteLabel(workspaceId, labelId);
    return ResponseEntity.noContent().build();
  }
}
