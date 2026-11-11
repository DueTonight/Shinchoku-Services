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
 * Creates and decides cross-team requests through typed entities. A locked triage row makes
 * acceptance idempotent in the same transaction as issue creation.
 */
@Service
@Transactional
public class TriageService {

  private final TriageRequestRepository triageRequestRepository;

  private final TeamMemberRepository teamMemberRepository;

  private final IssueService issueService;

  private final Access workspaceAccess;

  private final JpaPagination pagination;

  public TriageService(
      TriageRequestRepository triageRequestRepository,
      TeamMemberRepository teamMemberRepository,
      IssueService issueService,
      Access workspaceAccess,
      JpaPagination pagination) {
    this.triageRequestRepository = triageRequestRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.issueService = issueService;
    this.workspaceAccess = workspaceAccess;
    this.pagination = pagination;
  }

  private void requireVisible(UUID workspaceId, UUID teamId) {
    workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
    if (workspaceAccess.isGuest(workspaceId)) {
      throw ApiException.forbidden();
    }
  }

  public PageResponse<TriageResponse> listTriage(
      UUID workspaceId, UUID teamId, TriageState state, CursorRequest cursor) {
    requireVisible(workspaceId, teamId);
    Specification<TriageRequest> filter =
        and(workspace(workspaceId), equalTo("targetTeamId", teamId));
    if (state != null) {
      filter = filter.and(equalTo("state", state));
    }
    return pagination.byTime(
        TriageRequest.class,
        filter,
        cursor,
        "createdAt",
        TriageRequest::getCreatedAt,
        false,
        ResponseMapper::triage);
  }

  public TriageResponse submitTriage(UUID workspaceId, UUID teamId, TriageCreateRequest request) {
    requireVisible(workspaceId, teamId);
    workspaceAccess.requireTeamLeadPermission(workspaceId, request.sourceTeamId());
    if (request.sourceTeamId().equals(teamId)) {
      throw ApiException.invalid("Source and target teams must differ");
    }
    if (teamMemberRepository
        .findByIdWorkspaceIdAndIdTeamIdAndIdUserIdAndRemovedAtIsNull(
            workspaceId, request.sourceTeamId(), Caller.current().userId())
        .isEmpty()) {
      throw ApiException.forbidden();
    }
    TriageRequest triage = new TriageRequest();
    triage.setWorkspaceId(workspaceId);
    triage.setSourceTeamId(request.sourceTeamId());
    triage.setTargetTeamId(teamId);
    triage.setRequesterId(Caller.current().userId());
    triage.setTitle(request.title());
    if (request.description() != null) {
      triage.setDescription(request.description());
    }
    return ResponseMapper.triage(triageRequestRepository.saveAndFlush(triage));
  }

  /**
   * A pessimistic lock makes repeat acceptance idempotent and protects creation of the target
   * issue.
   */
  public TriageResponse decideTriage(
      UUID workspaceId, UUID teamId, UUID requestId, TriageDecisionRequest request) {
    requireVisible(workspaceId, teamId);
    workspaceAccess.requireTeamLeadPermission(workspaceId, teamId);
    TriageRequest triage =
        triageRequestRepository
            .findLocked(workspaceId, requestId)
            .orElseThrow(ApiException::missing);
    if (!triage.getTargetTeamId().equals(teamId)) {
      throw ApiException.missing();
    }
    if (triage.getState() != TriageState.PENDING) {
      if (triage.getState() != request.decision()) {
        throw ApiException.conflict("A conflicting decision has already been recorded");
      }
      return ResponseMapper.triage(triage);
    }
    if (request.decision() == TriageState.ACCEPTED) {
      IssueResponse issue =
          issueService.createIssue(
              workspaceId,
              new IssueCreateRequest(
                  teamId,
                  triage.getTitle(),
                  triage.getDescription(),
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  List.of()));
      if (!issue.teamId().equals(teamId)) {
        throw ApiException.conflict("Accepted issue has an invalid target team");
      }
      triage.setAcceptedIssueId(issue.id());
    }
    triage.setState(request.decision());
    triage.setDecidedBy(Caller.current().userId());
    triage.setDecidedAt(Instant.now());
    triage.setDecisionReason(request.reason());
    return ResponseMapper.triage(triageRequestRepository.saveAndFlush(triage));
  }
}
