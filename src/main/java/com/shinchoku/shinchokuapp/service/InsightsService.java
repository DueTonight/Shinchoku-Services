package com.shinchoku.shinchokuapp.service;

import static com.shinchoku.shinchokuapp.persistence.JpaPredicates.*;

import com.shinchoku.shinchokuapp.api.ApiException;
import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import com.shinchoku.shinchokuapp.repository.*;
import com.shinchoku.shinchokuapp.security.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates typed metrics and chart DTOs from authorized Issue and IssueActivity entities.
 * Summaries remain explicitly rule-based.
 */
@Service
@Transactional(readOnly = true)
public class InsightsService {

  private final IssueRepository issueRepository;

  private final IssueActivityRepository issueActivityRepository;

  private final CycleRepository cycleRepository;

  private final IssueService issueService;

  private final Access workspaceAccess;

  public InsightsService(
      IssueRepository issueRepository,
      IssueActivityRepository issueActivityRepository,
      CycleRepository cycleRepository,
      IssueService issueService,
      Access workspaceAccess) {
    this.issueRepository = issueRepository;
    this.issueActivityRepository = issueActivityRepository;
    this.cycleRepository = cycleRepository;
    this.issueService = issueService;
    this.workspaceAccess = workspaceAccess;
  }

  public InsightsResponse getInsights(UUID workspaceId, UUID teamId, UUID cycleId) {
    workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
    if (cycleId != null
        && cycleRepository
            .findByWorkspaceIdAndId(workspaceId, cycleId)
            .map(cycle -> !cycle.getTeamId().equals(teamId))
            .orElse(true)) {
      throw ApiException.missing();
    }
    IssueFilters filters =
        new IssueFilters(List.of(teamId), null, null, null, null, cycleId, null, null, false);
    List<Issue> authorizedIssues =
        issueRepository.findAll(
            issueService
                .filter(workspaceId, filters)
                .and(
                    (root, query, builder) ->
                        builder.notEqual(root.get("state"), IssueState.CANCELLED)));
    long total = authorizedIssues.size(),
        done = countInState(authorizedIssues, IssueState.DONE),
        inProgress = countInState(authorizedIssues, IssueState.IN_PROGRESS),
        blocked = countInState(authorizedIssues, IssueState.BLOCKED);
    Set<UUID> issueIds = authorizedIssues.stream().map(Issue::getId).collect(Collectors.toSet());
    List<IssueActivity> events =
        issueIds.isEmpty()
            ? List.of()
            : issueActivityRepository.findAll(
                and(
                    workspace(workspaceId),
                    (root, query, builder) -> root.get("issueId").in(issueIds),
                    (root, query, builder) ->
                        root.get("kind").in(List.of(ActivityKind.CREATED, ActivityKind.UPDATED))),
                Sort.by("occurredAt", "id"));
    return new InsightsResponse(
        teamId,
        cycleId,
        total,
        done,
        inProgress,
        blocked,
        total == 0 ? 0.0 : 100.0 * done / total,
        chart(authorizedIssues, events),
        new InsightSummaryResponse(
            "rule_based",
            done + " of " + total + " issues completed; " + blocked + " blocked.",
            Instant.now()));
  }

  private long countInState(List<Issue> issues, IssueState state) {
    return issues.stream().filter(issue -> issue.getState() == state).count();
  }

  /** Replay immutable state events instead of treating today's state as yesterday's state. */
  private List<InsightPointResponse> chart(List<Issue> issues, List<IssueActivity> events) {
    List<InsightPointResponse> result = new ArrayList<>();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    for (int daysAgo = 13; daysAgo >= 0; daysAgo--) {
      LocalDate date = today.minusDays(daysAgo);
      Instant cutoff = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
      long completed = 0, remaining = 0;
      for (Issue issue : issues) {
        if (!issue.getCreatedAt().isBefore(cutoff)) {
          continue;
        }
        String state = "todo";
        boolean completedOnDate = false;
        for (IssueActivity event : events) {
          if (event.getIssueId().equals(issue.getId())) {
            Object recordedState = event.getChanges().get("state");
            if (recordedState != null && event.getOccurredAt().isBefore(cutoff)) {
              state = recordedState.toString();
            }
            if ("done".equals(recordedState)
                && event.getOccurredAt().atOffset(ZoneOffset.UTC).toLocalDate().equals(date)) {
              completedOnDate = true;
            }
          }
        }
        if (!state.equals("done") && !state.equals("cancelled")) {
          remaining++;
        }
        if (completedOnDate) {
          completed++;
        }
      }
      result.add(new InsightPointResponse(date, completed, remaining));
    }
    return result;
  }
}
