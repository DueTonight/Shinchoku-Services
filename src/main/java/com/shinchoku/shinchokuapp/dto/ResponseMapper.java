package com.shinchoku.shinchokuapp.dto;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import java.util.*;

/**
 * Explicit entity-to-response mappings. Persistence-only fields, credential hashes and lazy
 * relationships never become public API payloads.
 */
public final class ResponseMapper {

  private ResponseMapper() {}

  public static ProfileResponse profile(User user) {
    return new ProfileResponse(
        user.getId(),
        user.getDisplayName(),
        user.getAvatarUrl(),
        user.getTimezone(),
        user.getTheme(),
        user.getEmail(),
        user.getCreatedAt());
  }

  public static WorkspaceResponse workspace(Workspace workspace) {
    return new WorkspaceResponse(workspace.getId(), workspace.getName(), workspace.getSlug());
  }

  public static TeamResponse team(Team team) {
    return new TeamResponse(
        team.getId(), team.getName(), team.getKey(), team.getDescription(), team.getArchivedAt());
  }

  public static ProjectResponse project(Project project) {
    return new ProjectResponse(
        project.getId(),
        project.getTeamId(),
        project.getName(),
        project.getDescription(),
        project.getState(),
        project.getLeadId(),
        project.getPlannedStart(),
        project.getTargetDate(),
        project.getPlanningNotes(),
        project.getArchivedAt());
  }

  public static MilestoneResponse milestone(Milestone milestone) {
    return new MilestoneResponse(milestone.getId(), milestone.getName(), milestone.getTargetDate());
  }

  public static LabelResponse label(Label label) {
    return new LabelResponse(label.getId(), label.getName());
  }

  public static CommentResponse comment(IssueComment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getBody(),
        comment.getAuthorId(),
        comment.getIssueId(),
        comment.getCreatedAt(),
        comment.getUpdatedAt());
  }

  public static ActivityResponse activity(IssueActivity activity) {
    return new ActivityResponse(
        activity.getId(),
        activity.getIssueId(),
        activity.getActorId(),
        activity.getKind(),
        activity.getChanges(),
        activity.getOccurredAt());
  }

  public static TriageResponse triage(TriageRequest request) {
    return new TriageResponse(
        request.getId(),
        request.getSourceTeamId(),
        request.getTargetTeamId(),
        request.getRequesterId(),
        request.getTitle(),
        request.getDescription(),
        request.getState(),
        request.getDecidedBy(),
        request.getDecidedAt(),
        request.getDecisionReason(),
        request.getAcceptedIssueId(),
        request.getCreatedAt());
  }

  public static InvitationResponse invitation(Invitation invitation) {
    return new InvitationResponse(
        invitation.getId(),
        invitation.getEmail(),
        invitation.getRole(),
        invitation.getExpiresAt(),
        invitation.getAcceptedAt(),
        invitation.getRevokedAt());
  }

  public static GuestGrantResponse grant(GuestGrant grant) {
    return new GuestGrantResponse(
        grant.getId(), grant.getUserId(), grant.getTeamId(), grant.getProjectId());
  }

  public static TeamMemberResponse teamMember(TeamMember member) {
    return new TeamMemberResponse(member.getId().getUserId(), member.getRole());
  }

  public static SecurityResponse security(WorkspaceSecurityPolicy policy) {
    return new SecurityResponse(
        policy.getRequireMfa(),
        policy.getSessionTimeoutMinutes(),
        Arrays.asList(policy.getAllowedEmailDomains()));
  }

  public static SessionResponse session(UserSession session, UUID currentId) {
    return new SessionResponse(
        session.getId(),
        session.getUserAgent(),
        session.getCreatedAt(),
        session.getExpiresAt(),
        session.getId().equals(currentId));
  }

  public static ConnectionResponse connection(OAuthConnection connection) {
    return new ConnectionResponse(
        connection.getId(),
        connection.getProvider(),
        Arrays.asList(connection.getScopes()),
        connection.getCreatedAt());
  }

  public static List<ApiKeyScope> scopes(ApiKey key) {
    return Arrays.stream(key.getScopes()).map(ApiKeyScope::fromValue).toList();
  }

  public static ApiKeyResponse apiKey(ApiKey key) {
    return new ApiKeyResponse(
        key.getId(),
        key.getName(),
        key.getPrefix(),
        scopes(key),
        key.getOwnerId(),
        key.getLastUsedAt(),
        key.getExpiresAt());
  }

  public static ApiKeyIssuedResponse issuedKey(ApiKey key, String token) {
    return new ApiKeyIssuedResponse(
        key.getId(),
        key.getName(),
        key.getPrefix(),
        scopes(key),
        key.getOwnerId(),
        key.getLastUsedAt(),
        key.getExpiresAt(),
        token);
  }

  public static RepositoryResponse repository(GithubRepository repository) {
    return new RepositoryResponse(
        repository.getId(),
        repository.getTeamId(),
        repository.getGithubId(),
        repository.getFullName(),
        repository.getSyncedAt());
  }
}
