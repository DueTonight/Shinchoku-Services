package com.shinchoku.shinchokuapp.security;

import static com.shinchoku.shinchokuapp.persistence.JpaPredicates.*;

import com.shinchoku.shinchokuapp.api.ApiException;
import com.shinchoku.shinchokuapp.dto.*;
import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import com.shinchoku.shinchokuapp.repository.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entity-based authorization shared by services. Memberships, roles, grants and security policy
 * decide access independently of JPA associations or database foreign keys.
 */
@Service
@Transactional(readOnly = true)
public class Access {

  private final WorkspaceMemberRepository workspaceMemberRepository;

  private final WorkspaceSecurityPolicyRepository workspaceSecurityPolicyRepository;

  private final UserRepository userRepository;

  private final UserSessionRepository userSessionRepository;

  private final TeamRepository teamRepository;

  private final TeamMemberRepository teamMemberRepository;

  private final ProjectRepository projectRepository;

  private final GuestGrantRepository guestGrantRepository;

  public Access(
      WorkspaceMemberRepository workspaceMemberRepository,
      WorkspaceSecurityPolicyRepository workspaceSecurityPolicyRepository,
      UserRepository userRepository,
      UserSessionRepository userSessionRepository,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      ProjectRepository projectRepository,
      GuestGrantRepository guestGrantRepository) {
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.workspaceSecurityPolicyRepository = workspaceSecurityPolicyRepository;
    this.userRepository = userRepository;
    this.userSessionRepository = userSessionRepository;
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.projectRepository = projectRepository;
    this.guestGrantRepository = guestGrantRepository;
  }

  /**
   * Checks current active membership, API-key tenancy, allowed email domains, MFA assurance and
   * workspace session timeout.
   */
  public WorkspaceMember requireWorkspaceMembership(UUID workspaceId) {
    Caller caller = Caller.current();
    caller.requireScope("read");
    if (!caller.isSessionAuthenticated() && !workspaceId.equals(caller.apiKeyWorkspaceId())) {
      throw ApiException.missing();
    }
    WorkspaceMember member =
        workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserIdAndRemovedAtIsNull(workspaceId, caller.userId())
            .orElseThrow(ApiException::missing);
    workspaceSecurityPolicyRepository
        .findById(workspaceId)
        .ifPresent(
            policy -> {
              if (policy.getRequireMfa() && !caller.mfaVerified()) {
                throw new ApiException(
                    403,
                    "MFA_REQUIRED",
                    "Workspace requires MFA verified by the identity provider");
              }
              User user =
                  userRepository.findById(caller.userId()).orElseThrow(ApiException::missing);
              if (policy.getAllowedEmailDomains().length > 0
                  && Arrays.stream(policy.getAllowedEmailDomains())
                      .noneMatch(
                          domain ->
                              user.getEmail().endsWith("@" + domain.toLowerCase(Locale.ROOT)))) {
                throw ApiException.forbidden();
              }
              if (caller.isSessionAuthenticated()) {
                UserSession session =
                    userSessionRepository
                        .findById(caller.sessionId())
                        .orElseThrow(ApiException::missing);
                if (!session
                    .getCreatedAt()
                    .isAfter(Instant.now().minusSeconds(policy.getSessionTimeoutMinutes() * 60L))) {
                  throw new ApiException(
                      401, "SESSION_EXPIRED", "Workspace session timeout exceeded");
                }
              }
            });
    return member;
  }

  public boolean isWorkspaceAdministrator(UUID workspaceId) {
    return Set.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN)
        .contains(requireWorkspaceMembership(workspaceId).getRole());
  }

  /**
   * Requires an owner/admin role and, for API keys, an admin scope. A key never grants a role its
   * owner does not hold.
   */
  public void requireWorkspaceAdministrator(UUID workspaceId) {
    if (!isWorkspaceAdministrator(workspaceId)) {
      throw ApiException.forbidden();
    }
    Caller.current().requireScope("admin");
  }

  /** Rejects read-only workspace roles and applies the key's write-scope limit independently. */
  public void requireWorkspaceWritePermission(UUID workspaceId) {
    if (!Set.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER)
        .contains(requireWorkspaceMembership(workspaceId).getRole())) {
      throw ApiException.forbidden();
    }
    Caller.current().requireScope("write");
  }

  public boolean isGuest(UUID workspaceId) {
    return requireWorkspaceMembership(workspaceId).getRole() == WorkspaceRole.GUEST;
  }

  /**
   * Loads a team from the requested workspace; writes require a non-archived team and a writable
   * membership or administrator role.
   */
  public Team requireTeamAccess(UUID workspaceId, UUID teamId, boolean writeRequired) {
    Team team =
        teamRepository
            .findByWorkspaceIdAndId(workspaceId, teamId)
            .orElseThrow(ApiException::missing);
    if (isGuest(workspaceId)
        && guestGrantRepository.count(
                and(
                    workspace(workspaceId),
                    equalTo("teamId", teamId),
                    equalTo("userId", Caller.current().userId())))
            == 0) {
      throw ApiException.missing();
    }
    if (writeRequired) {
      requireWorkspaceWritePermission(workspaceId);
      if (team.getArchivedAt() != null) {
        throw ApiException.conflict("Team is archived");
      }
      if (!isWorkspaceAdministrator(workspaceId)) {
        TeamMember member =
            teamMemberRepository
                .findByIdWorkspaceIdAndIdTeamIdAndIdUserIdAndRemovedAtIsNull(
                    workspaceId, teamId, Caller.current().userId())
                .orElseThrow(ApiException::forbidden);
        if (member.getRole() == TeamRole.VIEWER) {
          throw ApiException.forbidden();
        }
      }
    }
    return team;
  }

  public void requireTeamLeadPermission(UUID workspaceId, UUID teamId) {
    requireTeamAccess(workspaceId, teamId, false);
    requireWorkspaceWritePermission(workspaceId);
    if (!isWorkspaceAdministrator(workspaceId)
        && teamMemberRepository
            .findByIdWorkspaceIdAndIdTeamIdAndIdUserIdAndRemovedAtIsNull(
                workspaceId, teamId, Caller.current().userId())
            .map(member -> member.getRole() != TeamRole.LEAD)
            .orElse(true)) {
      throw ApiException.forbidden();
    }
  }

  /**
   * Checks a project-specific guest grant and its parent team's permissions; project visibility
   * does not imply access to every project in that team.
   */
  public Project requireProjectAccess(UUID workspaceId, UUID projectId, boolean writeRequired) {
    Project project =
        projectRepository
            .findByWorkspaceIdAndId(workspaceId, projectId)
            .orElseThrow(ApiException::missing);
    if (isGuest(workspaceId)
        && guestGrantRepository.count(
                and(
                    workspace(workspaceId),
                    equalTo("teamId", project.getTeamId()),
                    equalTo("userId", Caller.current().userId()),
                    any(equalTo("projectId", null), equalTo("projectId", projectId))))
            == 0) {
      throw ApiException.missing();
    }
    requireTeamAccess(workspaceId, project.getTeamId(), writeRequired);
    return project;
  }

  /**
   * Checks referenced assignees/leads against active tenant membership, not merely the existence of
   * a global User entity.
   */
  public void requireActiveWorkspaceMember(UUID workspaceId, UUID userId) {
    if (userId != null
        && workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserIdAndRemovedAtIsNull(workspaceId, userId)
            .isEmpty()) {
      throw ApiException.invalid("Referenced user is not an active workspace member");
    }
  }

  /**
   * Guest grants become a Criteria subquery before pagination/counts, not a filter on an already
   * fetched page.
   */
  public <E> Specification<E> visibleTeamResource(
      UUID workspaceId, String teamProperty, String projectProperty) {
    if (!isGuest(workspaceId)) {
      return (root, query, builder) -> builder.conjunction();
    }
    UUID userId = Caller.current().userId();
    return (root, query, builder) -> {
      var subquery = query.subquery(UUID.class);
      var grant = subquery.from(GuestGrant.class);
      var projectCondition =
          projectProperty == null
              ? builder.isNull(grant.get("projectId"))
              : builder.or(
                  builder.isNull(grant.get("projectId")),
                  builder.equal(grant.get("projectId"), path(root, projectProperty)));
      subquery
          .select(grant.get("id"))
          .where(
              builder.equal(grant.get("workspaceId"), workspaceId),
              builder.equal(grant.get("userId"), userId),
              builder.equal(grant.get("teamId"), path(root, teamProperty)),
              projectCondition);
      return builder.exists(subquery);
    };
  }

  public Specification<Team> visibleTeams(UUID workspaceId) {
    if (!isGuest(workspaceId)) {
      return (root, query, builder) -> builder.conjunction();
    }
    UUID userId = Caller.current().userId();
    return (root, query, builder) -> {
      var sub = query.subquery(UUID.class);
      var grant = sub.from(GuestGrant.class);
      sub.select(grant.get("id"))
          .where(
              builder.equal(grant.get("workspaceId"), workspaceId),
              builder.equal(grant.get("userId"), userId),
              builder.equal(grant.get("teamId"), root.get("id")));
      return builder.exists(sub);
    };
  }
}
