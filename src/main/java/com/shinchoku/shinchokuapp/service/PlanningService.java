package com.shinchoku.shinchokuapp.service;

import static com.shinchoku.shinchokuapp.persistence.JpaPredicates.*;

import com.shinchoku.shinchokuapp.api.ApiException;
import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import com.shinchoku.shinchokuapp.persistence.JpaPagination;
import com.shinchoku.shinchokuapp.repository.*;
import com.shinchoku.shinchokuapp.security.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Typed planning workflows for projects, milestones, cycles and labels. Team locks protect cycle
 * windows; Flyway constraints remain the final database integrity checks.
 */
@Service
@Transactional
public class PlanningService {

  private final ProjectRepository projectRepository;

  private final MilestoneRepository milestoneRepository;

  private final CycleRepository cycleRepository;

  private final TeamRepository teamRepository;

  private final LabelRepository labelRepository;

  private final IssueRepository issueRepository;

  private final IssueService issueService;

  private final Access workspaceAccess;

  private final JpaPagination pagination;

  public PlanningService(
      ProjectRepository projectRepository,
      MilestoneRepository milestoneRepository,
      CycleRepository cycleRepository,
      TeamRepository teamRepository,
      LabelRepository labelRepository,
      IssueRepository issueRepository,
      IssueService issueService,
      Access workspaceAccess,
      JpaPagination pagination) {
    this.projectRepository = projectRepository;
    this.milestoneRepository = milestoneRepository;
    this.cycleRepository = cycleRepository;
    this.teamRepository = teamRepository;
    this.labelRepository = labelRepository;
    this.issueRepository = issueRepository;
    this.issueService = issueService;
    this.workspaceAccess = workspaceAccess;
    this.pagination = pagination;
  }

  public PageResponse<ProjectResponse> listProjects(
      UUID workspaceId, UUID teamId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    Specification<Project> filter =
        and(
            workspace(workspaceId),
            workspaceAccess.visibleTeamResource(workspaceId, "teamId", "id"));
    if (teamId != null) {
      filter = filter.and(equalTo("teamId", teamId));
    }
    return pagination.byTime(
        Project.class,
        filter,
        cursor,
        "updatedAt",
        Project::getUpdatedAt,
        true,
        ResponseMapper::project);
  }

  public ProjectResponse createProject(UUID workspaceId, ProjectCreateRequest request) {
    workspaceAccess.requireTeamAccess(workspaceId, request.teamId(), true);
    workspaceAccess.requireActiveWorkspaceMember(workspaceId, request.leadId());
    validateDates(request.plannedStart(), request.targetDate());
    Project project = new Project();
    project.setWorkspaceId(workspaceId);
    project.setTeamId(request.teamId());
    project.setName(request.name());
    if (request.description() != null) {
      project.setDescription(request.description());
    }
    if (request.state() != null) {
      project.setState(request.state());
    }
    project.setLeadId(request.leadId());
    project.setPlannedStart(request.plannedStart());
    project.setTargetDate(request.targetDate());
    if (request.planningNotes() != null) {
      project.setPlanningNotes(request.planningNotes());
    }
    return ResponseMapper.project(projectRepository.saveAndFlush(project));
  }

  public ProjectResponse getProject(UUID workspaceId, UUID projectId) {
    return ResponseMapper.project(
        workspaceAccess.requireProjectAccess(workspaceId, projectId, false));
  }

  /**
   * Applies only supplied fields, then validates the resulting start/target dates together.
   * Explicit null clears a nullable planning field.
   */
  public ProjectResponse updateProject(
      UUID workspaceId, UUID projectId, ProjectPatchRequest request) {
    Project project = workspaceAccess.requireProjectAccess(workspaceId, projectId, true);
    workspaceAccess.requireActiveWorkspaceMember(workspaceId, request.getLeadId());
    if (request.hasName()) {
      project.setName(request.getName());
    }
    if (request.hasDescription()) {
      project.setDescription(request.getDescription());
    }
    if (request.hasState()) {
      project.setState(request.getState());
    }
    if (request.hasLeadId()) {
      project.setLeadId(request.getLeadId());
    }
    if (request.hasPlannedStart()) {
      project.setPlannedStart(request.getPlannedStart());
    }
    if (request.hasTargetDate()) {
      project.setTargetDate(request.getTargetDate());
    }
    if (request.hasPlanningNotes()) {
      project.setPlanningNotes(request.getPlanningNotes());
    }
    if (request.hasArchived()) {
      project.setArchivedAt(request.getArchived() ? Instant.now() : null);
    }
    validateDates(project.getPlannedStart(), project.getTargetDate());
    project.setUpdatedAt(Instant.now());
    return ResponseMapper.project(projectRepository.saveAndFlush(project));
  }

  private void validateDates(LocalDate plannedStart, LocalDate targetDate) {
    if (plannedStart != null && targetDate != null && targetDate.isBefore(plannedStart)) {
      throw ApiException.invalid("Target date precedes planned start");
    }
  }

  public PageResponse<MilestoneResponse> listMilestones(
      UUID workspaceId, UUID projectId, CursorRequest cursor) {
    workspaceAccess.requireProjectAccess(workspaceId, projectId, false);
    return pagination.byId(
        Milestone.class,
        and(workspace(workspaceId), equalTo("projectId", projectId)),
        cursor,
        ResponseMapper::milestone);
  }

  public MilestoneResponse createMilestone(
      UUID workspaceId, UUID projectId, MilestoneCreateRequest request) {
    Project project = workspaceAccess.requireProjectAccess(workspaceId, projectId, true);
    Milestone milestone = new Milestone();
    milestone.setWorkspaceId(workspaceId);
    milestone.setTeamId(project.getTeamId());
    milestone.setProjectId(projectId);
    milestone.setName(request.name());
    milestone.setTargetDate(request.targetDate());
    return ResponseMapper.milestone(milestoneRepository.saveAndFlush(milestone));
  }

  private Milestone requireMilestone(UUID workspaceId, UUID projectId, UUID milestoneId) {
    workspaceAccess.requireProjectAccess(workspaceId, projectId, true);
    return milestoneRepository
        .findOne(
            and(
                workspace(workspaceId),
                equalTo("projectId", projectId),
                equalTo("id", milestoneId)))
        .orElseThrow(ApiException::missing);
  }

  public MilestoneResponse updateMilestone(
      UUID workspaceId, UUID projectId, UUID milestoneId, MilestonePatchRequest request) {
    Milestone milestone = requireMilestone(workspaceId, projectId, milestoneId);
    if (request.hasName()) {
      milestone.setName(request.getName());
    }
    if (request.hasTargetDate()) {
      milestone.setTargetDate(request.getTargetDate());
    }
    return ResponseMapper.milestone(milestoneRepository.saveAndFlush(milestone));
  }

  public void deleteMilestone(UUID workspaceId, UUID projectId, UUID milestoneId) {
    milestoneRepository.delete(requireMilestone(workspaceId, projectId, milestoneId));
    milestoneRepository.flush();
  }

  public PageResponse<CycleResponse> listCycles(
      UUID workspaceId, UUID teamId, CursorRequest cursor) {
    workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
    return pagination.byTime(
        Cycle.class,
        and(workspace(workspaceId), equalTo("teamId", teamId)),
        cursor,
        "startsOn",
        Cycle::getStartsOn,
        false,
        this::cycleResponse);
  }

  private Cycle requireCycle(UUID workspaceId, UUID teamId, UUID cycleId) {
    workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
    Cycle cycle =
        cycleRepository
            .findByWorkspaceIdAndId(workspaceId, cycleId)
            .orElseThrow(ApiException::missing);
    if (!cycle.getTeamId().equals(teamId)) {
      throw ApiException.missing();
    }
    return cycle;
  }

  public CycleResponse getCycle(UUID workspaceId, UUID teamId, UUID cycleId) {
    return cycleResponse(requireCycle(workspaceId, teamId, cycleId));
  }

  /**
   * The team row serializes overlap checks; querying cycles without this lock is not
   * concurrency-safe.
   */
  private void validateCycleWindow(
      UUID workspaceId, UUID teamId, UUID excludedCycleId, LocalDate start, LocalDate end) {
    workspaceAccess.requireTeamAccess(workspaceId, teamId, true);
    workspaceAccess.requireTeamLeadPermission(workspaceId, teamId);
    teamRepository.findLocked(workspaceId, teamId).orElseThrow(ApiException::missing);
    if (!end.equals(start.plusDays(13))) {
      throw ApiException.invalid("Cycles must have an inclusive 14-day window");
    }
    Specification<Cycle> overlap =
        (root, query, builder) ->
            builder.and(
                builder.equal(root.get("workspaceId"), workspaceId),
                builder.equal(root.get("teamId"), teamId),
                builder.lessThanOrEqualTo(root.get("startsOn"), end),
                builder.greaterThanOrEqualTo(root.get("endsOn"), start));
    if (excludedCycleId != null) {
      overlap =
          overlap.and((root, query, builder) -> builder.notEqual(root.get("id"), excludedCycleId));
    }
    if (cycleRepository.count(overlap) > 0) {
      throw ApiException.conflict("Cycle overlaps an existing cycle");
    }
  }

  /**
   * Validates and locks the team before persisting a new cycle, so competing requests cannot insert
   * overlapping windows.
   */
  public CycleResponse createCycle(UUID workspaceId, UUID teamId, CycleCreateRequest request) {
    validateCycleWindow(workspaceId, teamId, null, request.startsOn(), request.endsOn());
    Cycle cycle = new Cycle();
    cycle.setWorkspaceId(workspaceId);
    cycle.setTeamId(teamId);
    cycle.setNumber(request.number());
    cycle.setStartsOn(request.startsOn());
    cycle.setEndsOn(request.endsOn());
    return cycleResponse(cycleRepository.saveAndFlush(cycle));
  }

  /**
   * Checks the proposed complete date window while holding the team lock, excluding this cycle from
   * its own overlap check.
   */
  public CycleResponse updateCycle(
      UUID workspaceId, UUID teamId, UUID cycleId, CyclePatchRequest request) {
    Cycle cycle = requireCycle(workspaceId, teamId, cycleId);
    LocalDate start = request.hasStartsOn() ? request.getStartsOn() : cycle.getStartsOn(),
        end = request.hasEndsOn() ? request.getEndsOn() : cycle.getEndsOn();
    validateCycleWindow(workspaceId, teamId, cycleId, start, end);
    if (request.hasNumber()) {
      cycle.setNumber(request.getNumber());
    }
    cycle.setStartsOn(start);
    cycle.setEndsOn(end);
    return cycleResponse(cycleRepository.saveAndFlush(cycle));
  }

  /**
   * Derives date status in UTC and progress from the reader's visible, non-cancelled issues. An
   * empty cycle has zero progress.
   */
  private CycleResponse cycleResponse(Cycle cycle) {
    IssueFilters filters =
        new IssueFilters(
            List.of(cycle.getTeamId()), null, null, null, null, cycle.getId(), null, null, false);
    Specification<Issue> visible =
        issueService
            .filter(cycle.getWorkspaceId(), filters)
            .and(
                (root, query, builder) ->
                    builder.notEqual(root.get("state"), IssueState.CANCELLED));
    long total = issueRepository.count(visible),
        done = issueRepository.count(visible.and(equalTo("state", IssueState.DONE)));
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    CycleStatus status =
        today.isBefore(cycle.getStartsOn())
            ? CycleStatus.UPCOMING
            : today.isAfter(cycle.getEndsOn()) ? CycleStatus.COMPLETED : CycleStatus.ACTIVE;
    return new CycleResponse(
        cycle.getId(),
        cycle.getNumber(),
        cycle.getStartsOn(),
        cycle.getEndsOn(),
        cycle.getTeamId(),
        status,
        total,
        done,
        total == 0 ? 0.0 : 100.0 * done / total);
  }

  public PageResponse<LabelResponse> listLabels(UUID workspaceId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    return pagination.byId(Label.class, workspace(workspaceId), cursor, ResponseMapper::label);
  }

  public LabelResponse createLabel(UUID workspaceId, LabelCreateRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    Label label = new Label();
    label.setWorkspaceId(workspaceId);
    label.setName(request.name());
    return ResponseMapper.label(labelRepository.saveAndFlush(label));
  }

  public LabelResponse updateLabel(UUID workspaceId, UUID labelId, LabelPatchRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    Label label =
        labelRepository
            .findByWorkspaceIdAndId(workspaceId, labelId)
            .orElseThrow(ApiException::missing);
    label.setName(request.getName());
    return ResponseMapper.label(labelRepository.saveAndFlush(label));
  }

  public void deleteLabel(UUID workspaceId, UUID labelId) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    labelRepository.delete(
        labelRepository
            .findByWorkspaceIdAndId(workspaceId, labelId)
            .orElseThrow(ApiException::missing));
    labelRepository.flush();
  }
}
