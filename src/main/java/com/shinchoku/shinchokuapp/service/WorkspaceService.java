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
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace, team, membership and security-policy workflows implemented with persistent entities.
 * The service transaction includes every related entity change; repository reads never replace
 * authorization checks.
 */
@Service
@Transactional
public class WorkspaceService {

  private final WorkspaceRepository workspaceRepository;

  private final WorkspaceMemberRepository workspaceMemberRepository;

  private final UserRepository userRepository;

  private final TeamRepository teamRepository;

  private final TeamMemberRepository teamMemberRepository;

  private final WorkspaceSecurityPolicyRepository workspaceSecurityPolicyRepository;

  private final InvitationRepository invitationRepository;

  private final GuestGrantRepository guestGrantRepository;

  private final ApiKeyRepository apiKeyRepository;

  private final UserSessionRepository userSessionRepository;

  private final IssueRepository issueRepository;

  private final ProjectRepository projectRepository;

  private final IssueService issueService;

  private final InvitationDelivery invitationDelivery;

  private final Access workspaceAccess;

  private final JpaPagination pagination;

  private final EntityManager entityManager;

  public WorkspaceService(
      WorkspaceRepository workspaceRepository,
      WorkspaceMemberRepository workspaceMemberRepository,
      UserRepository userRepository,
      TeamRepository teamRepository,
      TeamMemberRepository teamMemberRepository,
      WorkspaceSecurityPolicyRepository workspaceSecurityPolicyRepository,
      InvitationRepository invitationRepository,
      GuestGrantRepository guestGrantRepository,
      ApiKeyRepository apiKeyRepository,
      UserSessionRepository userSessionRepository,
      IssueRepository issueRepository,
      ProjectRepository projectRepository,
      IssueService issueService,
      InvitationDelivery invitationDelivery,
      Access workspaceAccess,
      JpaPagination pagination,
      EntityManager entityManager) {
    this.workspaceRepository = workspaceRepository;
    this.workspaceMemberRepository = workspaceMemberRepository;
    this.userRepository = userRepository;
    this.teamRepository = teamRepository;
    this.teamMemberRepository = teamMemberRepository;
    this.workspaceSecurityPolicyRepository = workspaceSecurityPolicyRepository;
    this.invitationRepository = invitationRepository;
    this.guestGrantRepository = guestGrantRepository;
    this.apiKeyRepository = apiKeyRepository;
    this.userSessionRepository = userSessionRepository;
    this.issueRepository = issueRepository;
    this.projectRepository = projectRepository;
    this.issueService = issueService;
    this.invitationDelivery = invitationDelivery;
    this.workspaceAccess = workspaceAccess;
    this.pagination = pagination;
    this.entityManager = entityManager;
  }

  public PageResponse<WorkspaceResponse> listWorkspaces(CursorRequest cursor) {
    UUID userId = Caller.current().userId();
    Specification<Workspace> visible =
        (root, query, builder) -> {
          var membership = query.subquery(UUID.class);
          var member = membership.from(WorkspaceMember.class);
          membership
              .select(member.get("id").get("workspaceId"))
              .where(
                  builder.equal(member.get("id").get("workspaceId"), root.get("id")),
                  builder.equal(member.get("id").get("userId"), userId),
                  builder.isNull(member.get("removedAt")));
          return builder.exists(membership);
        };
    if (!Caller.current().isSessionAuthenticated()) {
      visible = visible.and(equalTo("id", Caller.current().apiKeyWorkspaceId()));
    }
    return pagination.byTime(
        Workspace.class,
        visible,
        cursor,
        "createdAt",
        Workspace::getCreatedAt,
        false,
        ResponseMapper::workspace);
  }

  /**
   * Creates the workspace, initial owner membership and security policy in one transaction so no
   * ownerless workspace is committed.
   */
  public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request) {
    Caller.current().requireSession();
    Workspace workspace = new Workspace();
    workspace.setName(request.name());
    workspace.setSlug(request.slug());
    workspace = workspaceRepository.saveAndFlush(workspace);
    WorkspaceMember owner = new WorkspaceMember();
    owner.setId(new WorkspaceMemberId(workspace.getId(), Caller.current().userId()));
    owner.setRole(WorkspaceRole.OWNER);
    workspaceMemberRepository.save(owner);
    WorkspaceSecurityPolicy policy = new WorkspaceSecurityPolicy();
    policy.setWorkspaceId(workspace.getId());
    workspaceSecurityPolicyRepository.save(policy);
    return ResponseMapper.workspace(workspace);
  }

  public WorkspaceResponse getWorkspace(UUID workspaceId) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    return ResponseMapper.workspace(
        workspaceRepository.findById(workspaceId).orElseThrow(ApiException::missing));
  }

  public WorkspaceResponse updateWorkspace(UUID workspaceId, WorkspacePatchRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    Workspace workspace =
        workspaceRepository.findById(workspaceId).orElseThrow(ApiException::missing);
    if (request.hasName()) {
      workspace.setName(request.getName());
    }
    if (request.hasSlug()) {
      workspace.setSlug(request.getSlug());
    }
    return ResponseMapper.workspace(workspaceRepository.saveAndFlush(workspace));
  }

  public PageResponse<MemberResponse> listMembers(UUID workspaceId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    return pagination.page(
        WorkspaceMember.class,
        and(equalTo("id.workspaceId", workspaceId), active()),
        cursor,
        "joinedAt",
        "id.userId",
        "userId",
        false,
        member -> new CursorKey(member.getJoinedAt(), member.getId().getUserId()),
        this::memberResponse);
  }

  private MemberResponse memberResponse(WorkspaceMember member) {
    User user =
        userRepository.findById(member.getId().getUserId()).orElseThrow(ApiException::missing);
    List<UUID> teamIds =
        teamMemberRepository
            .findAllByIdWorkspaceIdAndIdUserIdAndRemovedAtIsNull(
                member.getId().getWorkspaceId(), member.getId().getUserId())
            .stream()
            .map(teamMember -> teamMember.getId().getTeamId())
            .toList();
    return new MemberResponse(
        user.getId(),
        user.getDisplayName(),
        user.getEmail(),
        member.getRole(),
        teamIds,
        member.getJoinedAt());
  }

  /**
   * Locks the parent workspace before refreshing the target membership. The refresh avoids making
   * owner decisions from a persistence-context snapshot loaded before the lock.
   */
  private WorkspaceMember memberForChange(UUID workspaceId, UUID userId) {
    workspaceRepository.findLocked(workspaceId).orElseThrow(ApiException::missing);
    WorkspaceMember member =
        workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserIdAndRemovedAtIsNull(workspaceId, userId)
            .orElseThrow(ApiException::missing);
    entityManager.refresh(member);
    if (member.getRemovedAt() != null) {
      throw ApiException.missing();
    }
    return member;
  }

  /**
   * Counts current active owners while the workspace lock is held. A concurrent request cannot
   * remove the remaining owner between this check and commit.
   */
  private void protectOwner(UUID workspaceId, WorkspaceMember member, WorkspaceRole newRole) {
    if (member.getRole() == WorkspaceRole.OWNER
        && newRole != WorkspaceRole.OWNER
        && workspaceMemberRepository.countByIdWorkspaceIdAndRoleAndRemovedAtIsNull(
                workspaceId, WorkspaceRole.OWNER)
            <= 1) {
      throw ApiException.conflict("The last workspace owner cannot be removed or demoted");
    }
  }

  /**
   * Checks the caller's role, locks ownership changes, and applies the requested role to the
   * managed membership entity.
   */
  public MemberResponse updateMember(UUID workspaceId, UUID userId, MemberPatchRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    WorkspaceMember member = memberForChange(workspaceId, userId);
    if ((member.getRole() == WorkspaceRole.OWNER || request.getRole() == WorkspaceRole.OWNER)
        && workspaceAccess.requireWorkspaceMembership(workspaceId).getRole()
            != WorkspaceRole.OWNER) {
      throw ApiException.forbidden();
    }
    protectOwner(workspaceId, member, request.getRole());
    member.setRole(request.getRole());
    workspaceMemberRepository.saveAndFlush(member);
    return memberResponse(member);
  }

  /**
   * Soft removal preserves historical FK references while current assignments and credentials are
   * withdrawn.
   */
  public void removeMember(UUID workspaceId, UUID userId) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    WorkspaceMember member = memberForChange(workspaceId, userId);
    if (member.getRole() == WorkspaceRole.OWNER
        && workspaceAccess.requireWorkspaceMembership(workspaceId).getRole()
            != WorkspaceRole.OWNER) {
      throw ApiException.forbidden();
    }
    protectOwner(workspaceId, member, null);
    Instant removedAt = Instant.now();
    member.setRemovedAt(removedAt);
    for (TeamMember teamMember :
        teamMemberRepository.findAll(
            and(equalTo("id.workspaceId", workspaceId), equalTo("id.userId", userId), active()))) {
      teamMember.setRemovedAt(removedAt);
    }
    guestGrantRepository.deleteAll(
        guestGrantRepository.findAll(and(workspace(workspaceId), equalTo("userId", userId))));
    for (ApiKey key :
        apiKeyRepository.findAll(and(workspace(workspaceId), equalTo("ownerId", userId)))) {
      key.setRevokedAt(removedAt);
    }
    for (UserSession session : userSessionRepository.findAllByUserId(userId)) {
      session.setRevokedAt(removedAt);
    }
    for (Issue issue :
        issueRepository.findAll(and(workspace(workspaceId), equalTo("assigneeId", userId)))) {
      issueService.recordActivity(
          issue, ActivityKind.UPDATED, Collections.singletonMap("assigneeId", null));
      issue.setAssigneeId(null);
      issue.setUpdatedAt(removedAt);
    }
    for (Project project :
        projectRepository.findAll(and(workspace(workspaceId), equalTo("leadId", userId)))) {
      project.setLeadId(null);
      project.setUpdatedAt(removedAt);
    }
    workspaceMemberRepository.flush();
  }

  public PageResponse<InvitationResponse> listInvitations(UUID workspaceId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    return pagination.byTime(
        Invitation.class,
        workspace(workspaceId),
        cursor,
        "expiresAt",
        Invitation::getExpiresAt,
        false,
        ResponseMapper::invitation);
  }

  /**
   * Persists only the token hash. The raw token is sent through the configured delivery channel,
   * not returned in invitation metadata.
   */
  public InvitationResponse inviteMember(UUID workspaceId, InvitationCreateRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    invitationDelivery.ensureAvailable();
    String rawToken = TokenService.generateToken();
    Invitation invitation = new Invitation();
    invitation.setWorkspaceId(workspaceId);
    invitation.setInvitedBy(Caller.current().userId());
    invitation.setEmail(request.email().toLowerCase(Locale.ROOT));
    invitation.setRole(request.role());
    invitation.setTokenHash(TokenService.hashToken(rawToken));
    invitation.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
    invitation = invitationRepository.saveAndFlush(invitation);
    invitationDelivery.send(invitation.getEmail(), rawToken);
    return ResponseMapper.invitation(invitation);
  }

  public void revokeInvitation(UUID workspaceId, UUID invitationId) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    Invitation invitation =
        invitationRepository
            .findByWorkspaceIdAndId(workspaceId, invitationId)
            .orElseThrow(ApiException::missing);
    invitation.setRevokedAt(Instant.now());
  }

  /**
   * The repository locks the invitation. Its email must match the authenticated user, and retrying
   * acceptance preserves an existing active membership's role.
   */
  public WorkspaceResponse acceptInvitation(InvitationAcceptRequest request) {
    Caller.current().requireSession();
    Invitation invitation =
        invitationRepository
            .findByTokenHash(TokenService.hashToken(request.token()))
            .orElseThrow(ApiException::missing);
    User user =
        userRepository.findById(Caller.current().userId()).orElseThrow(ApiException::missing);
    if (!user.getEmail().equals(invitation.getEmail())) {
      throw ApiException.forbidden();
    }
    if (invitation.getRevokedAt() != null || invitation.getExpiresAt().isBefore(Instant.now())) {
      throw ApiException.conflict("Invitation is expired or revoked");
    }
    if (invitation.getAcceptedAt() == null) {
      WorkspaceMemberId membershipId =
          new WorkspaceMemberId(invitation.getWorkspaceId(), user.getId());
      WorkspaceMember member = workspaceMemberRepository.findById(membershipId).orElse(null);
      if (member == null) {
        member = new WorkspaceMember();
        member.setId(membershipId);
        member.setRole(invitation.getRole());
      } else if (member.getRemovedAt() != null) {
        member.setRole(invitation.getRole());
      }
      member.setRemovedAt(null);
      workspaceMemberRepository.save(member);
      invitation.setAcceptedAt(Instant.now());
    }
    return ResponseMapper.workspace(
        workspaceRepository
            .findById(invitation.getWorkspaceId())
            .orElseThrow(ApiException::missing));
  }

  public PageResponse<GuestGrantResponse> listGuestGrants(UUID workspaceId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    return pagination.byId(GuestGrant.class, workspace(workspaceId), cursor, ResponseMapper::grant);
  }

  /**
   * Validates the guest, team and optional project before creating the grant; a project from
   * another team is rejected.
   */
  public GuestGrantResponse grantGuestAccess(UUID workspaceId, GuestGrantCreateRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    workspaceAccess.requireActiveWorkspaceMember(workspaceId, request.userId());
    workspaceAccess.requireTeamAccess(workspaceId, request.teamId(), false);
    if (workspaceMemberRepository
            .findById(new WorkspaceMemberId(workspaceId, request.userId()))
            .orElseThrow(ApiException::missing)
            .getRole()
        != WorkspaceRole.GUEST) {
      throw ApiException.invalid("Access grants require a guest member");
    }
    if (request.projectId() != null
        && !workspaceAccess
            .requireProjectAccess(workspaceId, request.projectId(), false)
            .getTeamId()
            .equals(request.teamId())) {
      throw ApiException.invalid("Project belongs to another team");
    }
    GuestGrant grant = new GuestGrant();
    grant.setWorkspaceId(workspaceId);
    grant.setUserId(request.userId());
    grant.setTeamId(request.teamId());
    grant.setProjectId(request.projectId());
    return ResponseMapper.grant(guestGrantRepository.saveAndFlush(grant));
  }

  public void revokeGuestAccess(UUID workspaceId, UUID grantId) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    guestGrantRepository.delete(
        guestGrantRepository
            .findByWorkspaceIdAndId(workspaceId, grantId)
            .orElseThrow(ApiException::missing));
    guestGrantRepository.flush();
  }

  public PageResponse<TeamResponse> listTeams(UUID workspaceId, CursorRequest cursor) {
    workspaceAccess.requireWorkspaceMembership(workspaceId);
    return pagination.byId(
        Team.class,
        and(workspace(workspaceId), workspaceAccess.visibleTeams(workspaceId)),
        cursor,
        ResponseMapper::team);
  }

  /** Persists the team and a lead membership for its authenticated creator. */
  public TeamResponse createTeam(UUID workspaceId, TeamCreateRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    Team team = new Team();
    team.setWorkspaceId(workspaceId);
    team.setName(request.name());
    team.setKey(request.key());
    if (request.description() != null) {
      team.setDescription(request.description());
    }
    team = teamRepository.saveAndFlush(team);
    TeamMember lead = new TeamMember();
    lead.setId(new TeamMemberId(workspaceId, team.getId(), Caller.current().userId()));
    lead.setRole(TeamRole.LEAD);
    teamMemberRepository.save(lead);
    return ResponseMapper.team(team);
  }

  public TeamResponse getTeam(UUID workspaceId, UUID teamId) {
    return ResponseMapper.team(workspaceAccess.requireTeamAccess(workspaceId, teamId, false));
  }

  public TeamResponse updateTeam(UUID workspaceId, UUID teamId, TeamPatchRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    Team team = workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
    if (request.hasName()) {
      team.setName(request.getName());
    }
    if (request.hasDescription()) {
      team.setDescription(request.getDescription());
    }
    if (request.hasArchived()) {
      team.setArchivedAt(request.getArchived() ? Instant.now() : null);
    }
    return ResponseMapper.team(teamRepository.saveAndFlush(team));
  }

  public PageResponse<TeamMemberResponse> listTeamMembers(
      UUID workspaceId, UUID teamId, CursorRequest cursor) {
    workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
    if (workspaceAccess.isGuest(workspaceId)) {
      throw ApiException.forbidden();
    }
    Specification<TeamMember> visible =
        (root, query, builder) -> {
          var sub = query.subquery(UUID.class);
          var member = sub.from(WorkspaceMember.class);
          sub.select(member.get("id").get("userId"))
              .where(
                  builder.equal(member.get("id").get("workspaceId"), workspaceId),
                  builder.equal(member.get("id").get("userId"), root.get("id").get("userId")),
                  builder.isNull(member.get("removedAt")));
          return builder.exists(sub);
        };
    return pagination.page(
        TeamMember.class,
        and(
            equalTo("id.workspaceId", workspaceId),
            equalTo("id.teamId", teamId),
            active(),
            visible),
        cursor,
        "",
        "id.userId",
        "userId",
        false,
        member -> new CursorKey(null, member.getId().getUserId()),
        ResponseMapper::teamMember);
  }

  public TeamMemberResponse addTeamMember(
      UUID workspaceId, UUID teamId, TeamMemberRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    workspaceAccess.requireTeamAccess(workspaceId, teamId, false);
    workspaceAccess.requireActiveWorkspaceMember(workspaceId, request.userId());
    TeamMemberId memberId = new TeamMemberId(workspaceId, teamId, request.userId());
    TeamMember member = teamMemberRepository.findById(memberId).orElseGet(TeamMember::new);
    member.setId(memberId);
    member.setRole(request.role());
    member.setRemovedAt(null);
    return ResponseMapper.teamMember(teamMemberRepository.saveAndFlush(member));
  }

  public TeamMemberResponse updateTeamMember(
      UUID workspaceId, UUID teamId, UUID userId, TeamMemberPatchRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    TeamMember member =
        teamMemberRepository
            .findByIdWorkspaceIdAndIdTeamIdAndIdUserIdAndRemovedAtIsNull(
                workspaceId, teamId, userId)
            .orElseThrow(ApiException::missing);
    member.setRole(request.getRole());
    return ResponseMapper.teamMember(teamMemberRepository.saveAndFlush(member));
  }

  public void removeTeamMember(UUID workspaceId, UUID teamId, UUID userId) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    TeamMember member =
        teamMemberRepository
            .findByIdWorkspaceIdAndIdTeamIdAndIdUserIdAndRemovedAtIsNull(
                workspaceId, teamId, userId)
            .orElseThrow(ApiException::missing);
    member.setRemovedAt(Instant.now());
  }

  public SecurityResponse getSecurity(UUID workspaceId) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    return ResponseMapper.security(
        workspaceSecurityPolicyRepository.findById(workspaceId).orElseThrow(ApiException::missing));
  }

  /**
   * Requires an administrator to already have verified MFA before enabling enforcement, preventing
   * an immediate self-lockout.
   */
  public SecurityResponse updateSecurity(UUID workspaceId, SecurityPatchRequest request) {
    workspaceAccess.requireWorkspaceAdministrator(workspaceId);
    WorkspaceSecurityPolicy policy =
        workspaceSecurityPolicyRepository.findById(workspaceId).orElseThrow(ApiException::missing);
    if (request.hasRequireMfa()) {
      if (request.getRequireMfa() && !Caller.current().mfaVerified()) {
        throw ApiException.invalid("Verify MFA with your identity provider before enforcing it");
      }
      policy.setRequireMfa(request.getRequireMfa());
    }
    if (request.hasSessionTimeoutMinutes()) {
      policy.setSessionTimeoutMinutes(request.getSessionTimeoutMinutes());
    }
    if (request.hasAllowedEmailDomains()) {
      policy.setAllowedEmailDomains(request.getAllowedEmailDomains().toArray(String[]::new));
    }
    return ResponseMapper.security(workspaceSecurityPolicyRepository.saveAndFlush(policy));
  }
}
