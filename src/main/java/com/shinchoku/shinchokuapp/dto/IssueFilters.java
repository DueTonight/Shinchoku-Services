package com.shinchoku.shinchokuapp.dto;

import com.shinchoku.shinchokuapp.entity.enums.*;
import java.util.*;

/**
 * Shared issue-list/count/view filters. Defaults never broaden the caller's resource permissions.
 */
public record IssueFilters(
    List<UUID> teamIds,
    List<IssueState> states,
    List<UUID> labelIds,
    LabelMatch labelMatch,
    String assigneeId,
    UUID cycleId,
    UUID projectId,
    String search,
    boolean includeArchived) {

  public IssueFilters {
    teamIds = teamIds == null ? List.of() : List.copyOf(teamIds);
    states = states == null ? List.of() : List.copyOf(states);
    labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
    labelMatch = labelMatch == null ? LabelMatch.ANY : labelMatch;
    search = search == null ? "" : search;
  }

  public static IssueFilters empty() {
    return new IssueFilters(null, null, null, null, null, null, null, null, false);
  }
}
