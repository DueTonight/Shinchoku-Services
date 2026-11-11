package com.shinchoku.shinchokuapp.service;

import static com.shinchoku.shinchokuapp.persistence.JpaPredicates.*;

import com.shinchoku.shinchokuapp.api.ApiException;
import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import com.shinchoku.shinchokuapp.persistence.JpaPagination;
import com.shinchoku.shinchokuapp.persistence.JpaPagination.CursorKey;
import com.shinchoku.shinchokuapp.repository.*;
import com.shinchoku.shinchokuapp.security.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Issue business rules on typed Issue, IssueLabel, IssueComment and IssueActivity entities. API
 * responses are DTOs, while Hibernate manages persistence and optimistic versions.
 */
@Service
@Transactional
public class IssueService {
  private static final char SQL_LIKE_ESCAPE = '\\';

  private final IssueRepository issueRepository;

  private final TeamRepository teamRepository;

  private final ProjectRepository projectRepository;

  private final CycleRepository cycleRepository;

  private final LabelRepository labelRepository;

  private final IssueLabelRepository issueLabelRepository;

  private final IssueCommentRepository issueCommentRepository;

  private final IssueActivityRepository issueActivityRepository;

  private final GithubIssueLinkRepository githubIssueLinkRepository;

  private final GithubRepositoryJpaRepository githubRepositoryJpaRepository;

  private final IssueNumberAllocator numberAllocator;

  private final Access workspaceAccess;

  private final JpaPagination pagination;

  private final ObjectMapper objectMapper;

  public IssueService(
      IssueRepository issueRepository,
      TeamRepository teamRepository,
      ProjectRepository projectRepository,
      CycleRepository cycleRepository,
      LabelRepository labelRepository,
      IssueLabelRepository issueLabelRepository,
      IssueCommentRepository issueCommentRepository,
      IssueActivityRepository issueActivityRepository,
      GithubIssueLinkRepository githubIssueLinkRepository,
      GithubRepositoryJpaRepository githubRepositoryJpaRepository,
      IssueNumberAllocator numberAllocator,
      Access workspaceAccess,
      JpaPagination pagination,
      ObjectMapper objectMapper) {
    this.issueRepository = issueRepository;
    this.teamRepository = teamRepository;
    this.projectRepository = projectRepository;
    this.cycleRepository = cycleRepository;
    this.labelRepository = labelRepository;
    this.issueLabelRepository = issueLabelRepository;
    this.issueCommentRepository = issueCommentRepository;
    this.issueActivityRepository = issueActivityRepository;
    this.githubIssueLinkRepository = githubIssueLinkRepository;
    this.githubRepositoryJpaRepository = githubRepositoryJpaRepository;
    this.numberAllocator = numberAllocator;
    this.workspaceAccess = workspaceAccess;
    this.pagination = pagination;
    this.objectMapper = objectMapper;
  }

  /** Builds one typed Criteria filter shared by lists, saved views and counts. */
  public Specification<Issue> filter(UUID workspaceId, IssueFilters filters) {
    Specification<Issue> result =
        and(
            workspace(workspaceId),
            workspaceAccess.visibleTeamResource(workspaceId, "teamId", "projectId"));
    if (!filters.includeArchived()) {
      result = result.and(equalTo("archivedAt", null));
    }
    if (!filters.teamIds().isEmpty()) {
      result = result.and((root, query, builder) -> root.get("teamId").in(filters.teamIds()));
    }
    if (!filters.states().isEmpty()) {
      result = result.and((root, query, builder) -> root.get("state").in(filters.states()));
    }
    if (filters.assigneeId() != null) {
      result =
          result.and(
              equalTo(
                  "assigneeId",
                  filters.assigneeId().equals("me")
                      ? Caller.current().userId()
                      : UUID.fromString(filters.assigneeId())));
    }
    if (filters.cycleId() != null) {
      result = result.and(equalTo("cycleId", filters.cycleId()));
    }
    if (filters.projectId() != null) {
      result = result.and(equalTo("projectId", filters.projectId()));
    }
    if (!filters.search().isBlank()) {
      String pattern =
          "%"
              + filters
                  .search()
                  .toLowerCase(Locale.ROOT)
                  .replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_")
              + "%";
      result =
          result.and(
              (root, query, builder) ->
                  builder.or(
                      builder.like(builder.lower(root.get("title")), pattern, SQL_LIKE_ESCAPE),
                      builder.like(
                          builder.lower(root.get("description")), pattern, SQL_LIKE_ESCAPE)));
    }
    if (!filters.labelIds().isEmpty()) {
      result =
          result.and(
              (root, query, builder) -> {
                var matches = query.subquery(Long.class);
                var relation = matches.from(IssueLabel.class);
                matches
                    .select(builder.count(relation))
                    .where(
                        builder.equal(relation.get("id").get("workspaceId"), workspaceId),
                        builder.equal(relation.get("id").get("issueId"), root.get("id")),
                        relation.get("id").get("labelId").in(filters.labelIds()));
                return filters.labelMatch() == LabelMatch.ALL
                    ? builder.equal(matches, filters.labelIds().size())
                    : builder.greaterThan(matches, 0L);
              });
    }
    return result;
  }

  public PageResponse<IssueResponse> listIssues(
      UUID workspaceId, IssueFilters filters, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    return pagination.page(
        Issue.class,
        filter(workspaceId, filters),
        cursor,
        "updatedAt",
        "id",
        "id",
        true,
        issue -> new CursorKey(issue.getUpdatedAt(), issue.getId()),
        this::toResponse);
  }

  public long countIssues(UUID workspaceId, IssueFilters filters) {
    return issueRepository.count(filter(workspaceId, filters));
  }

  /**
   * Scopes the UUID lookup by workspace and guest grants. A real UUID in another tenant is still
   * reported as not found.
   */
  public Issue requireIssue(UUID workspaceId, UUID issueId) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    return issueRepository
        .findOne(
            and(
                workspace(workspaceId),
                equalTo("id", issueId),
                workspaceAccess.visibleTeamResource(workspaceId, "teamId", "projectId")))
        .orElseThrow(ApiException::missing);
  }

  public IssueResponse getIssue(UUID workspaceId, UUID issueId) {
    return toResponse(requireIssue(workspaceId, issueId));
  }

  /** Entity creation, label relations and activity commit together in this service transaction. */
  public IssueResponse createIssue(UUID workspaceId, IssueCreateRequest request) {
    workspaceAccess.requireTeamAccess(workspaceId, request.teamId(), true);
    validateReferences(
        workspaceId,
        request.teamId(),
        request.assigneeId(),
        request.projectId(),
        request.cycleId(),
        request.labelIds());
    Issue issue = new Issue();
    issue.setWorkspaceId(workspaceId);
    issue.setTeamId(request.teamId());
    issue.setNumber(numberAllocator.reserve(workspaceId, request.teamId()));
    issue.setTitle(request.title());
    if (request.description() != null) {
      issue.setDescription(request.description());
    }
    if (request.state() != null) {
      issue.setState(request.state());
    }
    if (request.priority() != null) {
      issue.setPriority(request.priority());
    }
    issue.setAssigneeId(request.assigneeId());
    issue.setProjectId(request.projectId());
    issue.setCycleId(request.cycleId());
    issue.setDueOn(request.dueOn());
    issue.setCreatorId(Caller.current().userId());
    if (issue.getState() == IssueState.DONE) {
      issue.setCompletedAt(Instant.now());
    }
    issue = issueRepository.saveAndFlush(issue);
    replaceLabels(issue, request.labelIds() == null ? List.of() : request.labelIds());
    recordActivity(issue, ActivityKind.CREATED, auditChanges(request));
    return toResponse(issue);
  }

  /**
   * @Version performs the final compare-and-increment in SQL; the explicit check gives a useful 409
   * early.
   */
  public IssueResponse updateIssue(UUID workspaceId, UUID issueId, IssuePatchRequest request) {
    Issue issue = requireIssue(workspaceId, issueId);
    workspaceAccess.requireTeamAccess(workspaceId, issue.getTeamId(), true);
    if (issue.getVersion() != request.getExpectedVersion()) {
      throw ApiException.conflict("Issue was changed; reload and retry");
    }
    validateReferences(
        workspaceId,
        issue.getTeamId(),
        request.getAssigneeId(),
        request.getProjectId(),
        request.getCycleId(),
        request.getLabelIds());
    if (request.hasTitle()) {
      issue.setTitle(request.getTitle());
    }
    if (request.hasDescription()) {
      issue.setDescription(request.getDescription());
    }
    if (request.hasPriority()) {
      issue.setPriority(request.getPriority());
    }
    if (request.hasAssigneeId()) {
      issue.setAssigneeId(request.getAssigneeId());
    }
    if (request.hasProjectId()) {
      issue.setProjectId(request.getProjectId());
    }
    if (request.hasCycleId()) {
      issue.setCycleId(request.getCycleId());
    }
    if (request.hasDueOn()) {
      issue.setDueOn(request.getDueOn());
    }
    if (request.hasState()) {
      issue.setState(request.getState());
      issue.setCompletedAt(
          request.getState() == IssueState.DONE
              ? (issue.getCompletedAt() == null ? Instant.now() : issue.getCompletedAt())
              : null);
    }
    if (request.hasArchived()) {
      issue.setArchivedAt(request.getArchived() ? Instant.now() : null);
    }
    issue.setUpdatedAt(Instant.now());
    // A racing transaction now fails before labels or audit events are written.
    issueRepository.saveAndFlush(issue);
    if (request.hasLabelIds()) {
      List<UUID> previous = labelIds(issue);
      replaceLabels(issue, request.getLabelIds());
      for (UUID labelId : previous) {
        if (!request.getLabelIds().contains(labelId)) {
          recordActivity(issue, ActivityKind.LABEL_REMOVED, Map.of("labelId", labelId));
        }
      }
      for (UUID labelId : request.getLabelIds()) {
        if (!previous.contains(labelId)) {
          recordActivity(issue, ActivityKind.LABEL_ADDED, Map.of("labelId", labelId));
        }
      }
    }
    recordActivity(issue, ActivityKind.UPDATED, auditChanges(request));
    return toResponse(issue);
  }

  /**
   * Validates active assignees and same-team projects/cycles, plus workspace-scoped labels, before
   * any entity write.
   */
  private void validateReferences(
      UUID workspaceId,
      UUID teamId,
      UUID assigneeId,
      UUID projectId,
      UUID cycleId,
      List<UUID> labelIds) {
    workspaceAccess.requireActiveWorkspaceMember(workspaceId, assigneeId);
    if (projectId != null) {
      Project project = workspaceAccess.requireProjectAccess(workspaceId, projectId, false);
      if (!project.getTeamId().equals(teamId)) {
        throw ApiException.invalid("Project belongs to another team");
      }
      if (project.getArchivedAt() != null) {
        throw ApiException.invalid("Project is archived");
      }
    }
    if (cycleId != null
        && cycleRepository
            .findByWorkspaceIdAndId(workspaceId, cycleId)
            .map(cycle -> !cycle.getTeamId().equals(teamId))
            .orElse(true)) {
      throw ApiException.invalid("Cycle belongs to another team");
    }
    if (labelIds != null) {
      for (UUID labelId : labelIds) {
        if (!labelRepository.existsByWorkspaceIdAndId(workspaceId, labelId)) {
          throw ApiException.invalid("Label is not in this workspace");
        }
      }
    }
  }

  private List<UUID> labelIds(Issue issue) {
    return issueLabelRepository
        .findAllByIdWorkspaceIdAndIdIssueId(issue.getWorkspaceId(), issue.getId())
        .stream()
        .map(relation -> relation.getId().getLabelId())
        .toList();
  }

  /**
   * An explicitly supplied label list replaces the complete join-entity set. Flush removals before
   * reinserting retained IDs to respect composite primary keys.
   */
  private void replaceLabels(Issue issue, List<UUID> requestedLabels) {
    issueLabelRepository.deleteAllByIdWorkspaceIdAndIdIssueId(
        issue.getWorkspaceId(), issue.getId());
    issueLabelRepository.flush();
    for (UUID labelId : requestedLabels) {
      IssueLabel relation = new IssueLabel();
      relation.setId(new IssueLabelId(issue.getWorkspaceId(), issue.getId(), labelId));
      issueLabelRepository.save(relation);
    }
    issueLabelRepository.flush();
  }

  /**
   * Stores the actor from authentication and appends a JSONB audit event in the same transaction as
   * the issue change.
   */
  public void recordActivity(Issue issue, ActivityKind kind, Map<String, Object> changes) {
    IssueActivity event = new IssueActivity();
    event.setWorkspaceId(issue.getWorkspaceId());
    event.setIssueId(issue.getId());
    event.setActorId(Caller.current().userId());
    event.setKind(kind);
    event.setChanges(changes);
    issueActivityRepository.save(event);
  }

  /**
   * Maps are restricted to the genuinely dynamic JSONB audit payload, not persistence models or API
   * DTOs.
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> auditChanges(Object request) {
    Map<String, Object> values = objectMapper.convertValue(request, Map.class);
    if (request instanceof PatchRequest patch) {
      values.keySet().removeIf(field -> !patch.has(field));
    }
    return values;
  }

  /**
   * Builds the approved API representation, including display identifier, label DTOs and optional
   * GitHub link metadata. The entity itself is never serialized.
   */
  public IssueResponse toResponse(Issue issue) {
    Team team =
        teamRepository
            .findByWorkspaceIdAndId(issue.getWorkspaceId(), issue.getTeamId())
            .orElseThrow(ApiException::missing);
    List<LabelResponse> issueLabelDtos =
        labelIds(issue).stream()
            .map(
                labelId ->
                    labelRepository
                        .findByWorkspaceIdAndId(issue.getWorkspaceId(), labelId)
                        .orElseThrow(ApiException::missing))
            .sorted(Comparator.comparing(Label::getName).thenComparing(Label::getId))
            .map(ResponseMapper::label)
            .toList();
    GithubLinkResponse github =
        githubIssueLinkRepository
            .findByIdWorkspaceIdAndIdIssueId(issue.getWorkspaceId(), issue.getId())
            .map(
                link -> {
                  GithubRepository repository =
                      githubRepositoryJpaRepository
                          .findByWorkspaceIdAndId(issue.getWorkspaceId(), link.getRepositoryId())
                          .orElseThrow(ApiException::missing);
                  return new GithubLinkResponse(
                      link.getRepositoryId(),
                      repository.getFullName(),
                      link.getGithubNumber().longValue(),
                      link.getUrl(),
                      link.getLastSyncedAt());
                })
            .orElse(null);
    return new IssueResponse(
        issue.getId(),
        issue.getTeamId(),
        issue.getTitle(),
        issue.getDescription(),
        issue.getState(),
        issue.getPriority(),
        issue.getAssigneeId(),
        issue.getProjectId(),
        issue.getCycleId(),
        issue.getDueOn(),
        issueLabelDtos.stream().map(LabelResponse::id).toList(),
        team.getKey() + "-" + issue.getNumber(),
        issue.getNumber(),
        issue.getWorkspaceId(),
        issue.getCreatorId(),
        issueLabelDtos,
        github,
        issue.getVersion(),
        issue.getCreatedAt(),
        issue.getUpdatedAt(),
        issue.getCompletedAt(),
        issue.getArchivedAt());
  }

  public PageResponse<CommentResponse> listComments(
      UUID workspaceId, UUID issueId, CursorRequest cursor) {
    requireIssue(workspaceId, issueId);
    return pagination.page(
        IssueComment.class,
        and(workspace(workspaceId), equalTo("issueId", issueId), equalTo("deletedAt", null)),
        cursor,
        "createdAt",
        "id",
        "id",
        false,
        comment -> new CursorKey(comment.getCreatedAt(), comment.getId()),
        ResponseMapper::comment);
  }

  public CommentResponse createComment(
      UUID workspaceId, UUID issueId, CommentCreateRequest request) {
    Issue issue = requireIssue(workspaceId, issueId);
    workspaceAccess.requireTeamAccess(workspaceId, issue.getTeamId(), true);
    IssueComment comment = new IssueComment();
    comment.setWorkspaceId(workspaceId);
    comment.setIssueId(issueId);
    comment.setAuthorId(Caller.current().userId());
    comment.setBody(request.body());
    comment = issueCommentRepository.saveAndFlush(comment);
    recordActivity(
        issue, ActivityKind.COMMENTED, Map.of("commentId", comment.getId(), "action", "created"));
    return ResponseMapper.comment(comment);
  }

  /**
   * Requires writable issue/team access, then restricts comment edits to their author or a
   * workspace administrator.
   */
  private IssueComment requireEditableComment(UUID workspaceId, UUID issueId, UUID commentId) {
    Issue issue = requireIssue(workspaceId, issueId);
    workspaceAccess.requireTeamAccess(workspaceId, issue.getTeamId(), true);
    IssueComment comment =
        issueCommentRepository
            .findOne(
                and(
                    workspace(workspaceId),
                    equalTo("issueId", issueId),
                    equalTo("id", commentId),
                    equalTo("deletedAt", null)))
            .orElseThrow(ApiException::missing);
    if (!comment.getAuthorId().equals(Caller.current().userId())
        && !workspaceAccess.isWorkspaceAdministrator(workspaceId)) {
      throw ApiException.forbidden();
    }
    return comment;
  }

  public CommentResponse updateComment(
      UUID workspaceId, UUID issueId, UUID commentId, CommentPatchRequest request) {
    IssueComment comment = requireEditableComment(workspaceId, issueId, commentId);
    comment.setBody(request.getBody());
    comment.setUpdatedAt(Instant.now());
    issueCommentRepository.saveAndFlush(comment);
    recordActivity(
        requireIssue(workspaceId, issueId),
        ActivityKind.COMMENTED,
        Map.of("commentId", commentId, "action", "updated"));
    return ResponseMapper.comment(comment);
  }

  public void deleteComment(UUID workspaceId, UUID issueId, UUID commentId) {
    IssueComment comment = requireEditableComment(workspaceId, issueId, commentId);
    comment.setDeletedAt(Instant.now());
    comment.setUpdatedAt(Instant.now());
    issueCommentRepository.save(comment);
    recordActivity(
        requireIssue(workspaceId, issueId),
        ActivityKind.COMMENTED,
        Map.of("commentId", commentId, "action", "deleted"));
  }

  public PageResponse<ActivityResponse> listActivity(
      UUID workspaceId, UUID issueId, CursorRequest cursor) {
    requireIssue(workspaceId, issueId);
    return pagination.page(
        IssueActivity.class,
        and(workspace(workspaceId), equalTo("issueId", issueId)),
        cursor,
        "occurredAt",
        "id",
        "id",
        false,
        event -> new CursorKey(event.getOccurredAt(), event.getId()),
        ResponseMapper::activity);
  }
}
