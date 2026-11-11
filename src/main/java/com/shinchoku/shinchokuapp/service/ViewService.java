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
 * Persists saved filter definitions using SavedView and explicit join entities. Issue membership
 * and counts are computed for the current authorized reader.
 */
@Service
@Transactional
public class ViewService {

  private final SavedViewRepository savedViewRepository;

  private final SavedViewLabelRepository savedViewLabelRepository;

  private final SavedViewTeamRepository savedViewTeamRepository;

  private final LabelRepository labelRepository;

  private final Access workspaceAccess;

  private final IssueService issueService;

  private final JpaPagination pagination;

  public ViewService(
      SavedViewRepository savedViewRepository,
      SavedViewLabelRepository savedViewLabelRepository,
      SavedViewTeamRepository savedViewTeamRepository,
      LabelRepository labelRepository,
      Access workspaceAccess,
      IssueService issueService,
      JpaPagination pagination) {
    this.savedViewRepository = savedViewRepository;
    this.savedViewLabelRepository = savedViewLabelRepository;
    this.savedViewTeamRepository = savedViewTeamRepository;
    this.labelRepository = labelRepository;
    this.workspaceAccess = workspaceAccess;
    this.issueService = issueService;
    this.pagination = pagination;
  }

  public PageResponse<SavedViewResponse> listViews(UUID workspaceId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    return pagination.byTime(
        SavedView.class,
        visibleViews(workspaceId),
        cursor,
        "updatedAt",
        SavedView::getUpdatedAt,
        true,
        this::toResponse);
  }

  /**
   * Personal views are owner-only. Workspace views are readable subject to current membership, with
   * issue access filtered separately.
   */
  private Specification<SavedView> visibleViews(UUID workspaceId) {
    return and(
        workspace(workspaceId),
        any(
            equalTo("ownerId", Caller.current().userId()),
            equalTo("visibility", ViewVisibility.WORKSPACE)));
  }

  private SavedView requireView(UUID workspaceId, UUID viewId) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    return savedViewRepository
        .findOne(and(visibleViews(workspaceId), equalTo("id", viewId)))
        .orElseThrow(ApiException::missing);
  }

  /**
   * Members may edit their own views; administrators can edit shared workspace views but cannot
   * access another user's private personal view.
   */
  private void requireEditable(SavedView view) {
    workspaceAccess.requireWorkspaceWritePermission(view.getWorkspaceId());
    if (!view.getOwnerId().equals(Caller.current().userId())
        && (view.getVisibility() != ViewVisibility.WORKSPACE
            || !workspaceAccess.isWorkspaceAdministrator(view.getWorkspaceId()))) {
      throw ApiException.forbidden();
    }
  }

  public SavedViewResponse getView(UUID workspaceId, UUID viewId) {
    return toResponse(requireView(workspaceId, viewId));
  }

  public SavedViewResponse createView(UUID workspaceId, SavedViewCreateRequest request) {
    workspaceAccess.requireWorkspaceWritePermission(workspaceId);
    validateReferences(workspaceId, request.assigneeId(), request.labelIds(), request.teamIds());
    SavedView view = new SavedView();
    view.setWorkspaceId(workspaceId);
    view.setOwnerId(Caller.current().userId());
    view.setName(request.name());
    view.setVisibility(request.visibility());
    view.setLabelMatch(request.labelMatch());
    view.setAssigneeId(request.assigneeId());
    if (request.search() != null) {
      view.setSearch(request.search());
    }
    if (request.states() != null) {
      view.setStates(request.states().stream().map(IssueState::value).toArray(String[]::new));
    }
    view = savedViewRepository.saveAndFlush(view);
    replaceRelations(view, request.labelIds(), request.teamIds());
    return toResponse(view);
  }

  public SavedViewResponse updateView(
      UUID workspaceId, UUID viewId, SavedViewPatchRequest request) {
    SavedView view = requireView(workspaceId, viewId);
    requireEditable(view);
    validateReferences(
        workspaceId, request.getAssigneeId(), request.getLabelIds(), request.getTeamIds());
    if (request.hasName()) {
      view.setName(request.getName());
    }
    if (request.hasVisibility()) {
      view.setVisibility(request.getVisibility());
    }
    if (request.hasLabelMatch()) {
      view.setLabelMatch(request.getLabelMatch());
    }
    if (request.hasAssigneeId()) {
      view.setAssigneeId(request.getAssigneeId());
    }
    if (request.hasSearch()) {
      view.setSearch(request.getSearch());
    }
    if (request.hasStates()) {
      view.setStates(request.getStates().stream().map(IssueState::value).toArray(String[]::new));
    }
    view.setUpdatedAt(Instant.now());
    savedViewRepository.saveAndFlush(view);
    replaceRelations(
        view,
        request.hasLabelIds() ? request.getLabelIds() : null,
        request.hasTeamIds() ? request.getTeamIds() : null);
    return toResponse(view);
  }

  public void deleteView(UUID workspaceId, UUID viewId) {
    SavedView view = requireView(workspaceId, viewId);
    requireEditable(view);
    savedViewLabelRepository.deleteAllByIdWorkspaceIdAndIdViewId(workspaceId, viewId);
    savedViewTeamRepository.deleteAllByIdWorkspaceIdAndIdViewId(workspaceId, viewId);
    savedViewLabelRepository.flush();
    savedViewTeamRepository.flush();
    savedViewRepository.delete(view);
    savedViewRepository.flush();
  }

  private void validateReferences(
      UUID workspaceId, UUID assigneeId, List<UUID> labelIds, List<UUID> teamIds) {
    workspaceAccess.requireActiveWorkspaceMember(workspaceId, assigneeId);
    if (labelIds != null) {
      for (UUID labelId : labelIds) {
        if (!labelRepository.existsByWorkspaceIdAndId(workspaceId, labelId)) {
          throw ApiException.invalid("Unknown workspace label");
        }
      }
    }
    if (teamIds != null) {
      for (UUID teamId : teamIds) {
        workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
      }
    }
  }

  /** Null means omitted; an empty list deliberately removes every relationship of that type. */
  private void replaceRelations(SavedView view, List<UUID> labelIds, List<UUID> teamIds) {
    if (labelIds != null) {
      savedViewLabelRepository.deleteAllByIdWorkspaceIdAndIdViewId(
          view.getWorkspaceId(), view.getId());
      savedViewLabelRepository.flush();
      for (UUID labelId : labelIds) {
        SavedViewLabel relation = new SavedViewLabel();
        relation.setId(new SavedViewLabelId(view.getWorkspaceId(), view.getId(), labelId));
        savedViewLabelRepository.save(relation);
      }
      savedViewLabelRepository.flush();
    }
    if (teamIds != null) {
      savedViewTeamRepository.deleteAllByIdWorkspaceIdAndIdViewId(
          view.getWorkspaceId(), view.getId());
      savedViewTeamRepository.flush();
      for (UUID teamId : teamIds) {
        SavedViewTeam relation = new SavedViewTeam();
        relation.setId(new SavedViewTeamId(view.getWorkspaceId(), view.getId(), teamId));
        savedViewTeamRepository.save(relation);
      }
      savedViewTeamRepository.flush();
    }
  }

  private List<UUID> labelIds(SavedView view) {
    return savedViewLabelRepository
        .findAllByIdWorkspaceIdAndIdViewId(view.getWorkspaceId(), view.getId())
        .stream()
        .map(relation -> relation.getId().getLabelId())
        .toList();
  }

  private List<UUID> teamIds(SavedView view) {
    return savedViewTeamRepository
        .findAllByIdWorkspaceIdAndIdViewId(view.getWorkspaceId(), view.getId())
        .stream()
        .map(relation -> relation.getId().getTeamId())
        .toList();
  }

  private IssueFilters filters(SavedView view) {
    return new IssueFilters(
        teamIds(view),
        Arrays.stream(view.getStates()).map(IssueState::fromValue).toList(),
        labelIds(view),
        view.getLabelMatch(),
        view.getAssigneeId() == null ? null : view.getAssigneeId().toString(),
        null,
        null,
        view.getSearch(),
        false);
  }

  /**
   * Reads the stored relation entities and computes issueCount with the exact same filter used to
   * list this reader's issues.
   */
  private SavedViewResponse toResponse(SavedView view) {
    IssueFilters filters = filters(view);
    return new SavedViewResponse(
        view.getId(),
        view.getName(),
        view.getVisibility(),
        view.getLabelMatch(),
        filters.labelIds(),
        filters.teamIds(),
        filters.states(),
        view.getAssigneeId(),
        view.getSearch(),
        view.getOwnerId(),
        issueService.countIssues(view.getWorkspaceId(), filters));
  }

  public PageResponse<IssueResponse> listViewIssues(
      UUID workspaceId, UUID viewId, CursorRequest cursor) {
    SavedView view = requireView(workspaceId, viewId);
    return issueService.listIssues(workspaceId, filters(view), cursor);
  }
}
