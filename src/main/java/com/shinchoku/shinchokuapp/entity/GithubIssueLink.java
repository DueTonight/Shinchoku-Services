package com.shinchoku.shinchokuapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shinchoku.shinchokuapp.entity.enums.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/**
 * Persistent model for github_issue_links. Flyway owns its constraints; JPA only validates the
 * schema.
 */
@Entity
@Table(name = "github_issue_links")
public class GithubIssueLink {

  @EmbeddedId private GithubIssueLinkId id;

  @Column(name = "repository_id", nullable = false)
  private UUID repositoryId;

  @Column(name = "github_number", nullable = false)
  private Integer githubNumber;

  @Column(name = "github_node_id", nullable = false, columnDefinition = "text")
  private String githubNodeId;

  @Column(name = "url", nullable = false, columnDefinition = "text")
  private String url;

  @Column(name = "last_synced_at", nullable = true)
  private Instant lastSyncedAt;

  public GithubIssueLink() {}

  public GithubIssueLinkId getId() {
    return id;
  }

  public void setId(GithubIssueLinkId id) {
    this.id = id;
  }

  public UUID getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(UUID repositoryId) {
    this.repositoryId = repositoryId;
  }

  public Integer getGithubNumber() {
    return githubNumber;
  }

  public void setGithubNumber(Integer githubNumber) {
    this.githubNumber = githubNumber;
  }

  public String getGithubNodeId() {
    return githubNodeId;
  }

  public void setGithubNodeId(String githubNodeId) {
    this.githubNodeId = githubNodeId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Instant getLastSyncedAt() {
    return lastSyncedAt;
  }

  public void setLastSyncedAt(Instant lastSyncedAt) {
    this.lastSyncedAt = lastSyncedAt;
  }

  /** Navigation only; the explicit ID fields and database composite keys control ownership. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "repository_id", insertable = false, updatable = false)
  private GithubRepository githubRepository;

  @JsonIgnore
  public GithubRepository getRepository() {
    return githubRepository;
  }
}
