package com.shinchoku.shinchokuapp;

import static org.junit.jupiter.api.Assertions.*;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import com.shinchoku.shinchokuapp.repository.*;
import jakarta.persistence.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies that the persistence model is genuinely mapped and used, including PostgreSQL-specific
 * fields.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaPersistenceTests {

  @Autowired EntityManager entityManager;

  @Autowired UserRepository userRepository;

  @Autowired WorkspaceRepository workspaceRepository;

  @Autowired WorkspaceMemberRepository workspaceMemberRepository;

  @Autowired TeamRepository teamRepository;

  @Autowired IssueRepository issueRepository;

  @Autowired IssueActivityRepository issueActivityRepository;

  @Autowired SavedViewRepository savedViewRepository;

  @Autowired OAuthConnectionRepository oAuthConnectionRepository;

  private User user() {
    User user = new User();
    user.setEmail(UUID.randomUUID() + "@entity.test");
    user.setDisplayName("Entity test user");
    return userRepository.saveAndFlush(user);
  }

  private Workspace workspace() {
    Workspace workspace = new Workspace();
    workspace.setName("Entity test");
    workspace.setSlug(UUID.randomUUID().toString());
    return workspaceRepository.saveAndFlush(workspace);
  }

  private Issue issue(User user, Workspace workspace) {
    WorkspaceMember member = new WorkspaceMember();
    member.setId(new WorkspaceMemberId(workspace.getId(), user.getId()));
    member.setRole(WorkspaceRole.OWNER);
    workspaceMemberRepository.saveAndFlush(member);
    Team team = new Team();
    team.setWorkspaceId(workspace.getId());
    team.setName("Engineering");
    team.setKey("ENG");
    team = teamRepository.saveAndFlush(team);
    Issue issue = new Issue();
    issue.setWorkspaceId(workspace.getId());
    issue.setTeamId(team.getId());
    issue.setCreatorId(user.getId());
    issue.setNumber(1L);
    issue.setTitle("Typed issue");
    return issueRepository.saveAndFlush(issue);
  }

  @Test
  void allTwentyFiveTablesHaveEntities() {
    Set<String> names = new HashSet<>();
    entityManager.getMetamodel().getEntities().forEach(entity -> names.add(entity.getName()));
    assertEquals(25, names.size());
    assertTrue(
        names.containsAll(
            Set.of(
                "Issue",
                "WorkspaceMember",
                "TeamMember",
                "IssueLabel",
                "UserSession",
                "ApiKey",
                "GithubIssueLink")));
  }

  @Test
  void entityRelationshipsAndVersionSurviveReload() {
    User user = user();
    Workspace workspace = workspace();
    Issue issue = issue(user, workspace);
    UUID issueId = issue.getId();
    assertEquals(1, issue.getVersion());
    entityManager.clear();
    Issue loaded = issueRepository.findByWorkspaceIdAndId(workspace.getId(), issueId).orElseThrow();
    assertEquals("ENG", loaded.getTeam().getKey());
    assertEquals(user.getEmail(), loaded.getCreator().getEmail());
    assertEquals(workspace.getId(), loaded.getWorkspace().getId());
    loaded.setTitle("Updated through entity");
    issueRepository.saveAndFlush(loaded);
    assertEquals(2, loaded.getVersion());
    WorkspaceMember member =
        workspaceMemberRepository
            .findById(new WorkspaceMemberId(workspace.getId(), user.getId()))
            .orElseThrow();
    assertEquals(WorkspaceRole.OWNER, member.getRole());
  }

  @Test
  void postgresArraysAndJsonbRoundTripAsTypedFields() {
    User user = user();
    Workspace workspace = workspace();
    Issue issue = issue(user, workspace);
    SavedView view = new SavedView();
    view.setWorkspaceId(workspace.getId());
    view.setOwnerId(user.getId());
    view.setName("Work");
    view.setStates(new String[] {"todo", "done"});
    view = savedViewRepository.saveAndFlush(view);
    IssueActivity event = new IssueActivity();
    event.setWorkspaceId(workspace.getId());
    event.setIssueId(issue.getId());
    event.setActorId(user.getId());
    event.setKind(ActivityKind.UPDATED);
    event.setChanges(Map.of("state", "done", "version", 2));
    event = issueActivityRepository.saveAndFlush(event);
    OAuthConnection connection = new OAuthConnection();
    connection.setUserId(user.getId());
    connection.setProvider("test-provider");
    connection.setProviderSubject(UUID.randomUUID().toString());
    connection.setScopes(new String[] {"read:user", "repo"});
    connection = oAuthConnectionRepository.saveAndFlush(connection);
    UUID viewId = view.getId(), eventId = event.getId(), connectionId = connection.getId();
    entityManager.clear();
    assertArrayEquals(
        new String[] {"todo", "done"},
        savedViewRepository.findById(viewId).orElseThrow().getStates());
    assertEquals(
        "done", issueActivityRepository.findById(eventId).orElseThrow().getChanges().get("state"));
    assertArrayEquals(
        new String[] {"read:user", "repo"},
        oAuthConnectionRepository.findById(connectionId).orElseThrow().getScopes());
  }
}
