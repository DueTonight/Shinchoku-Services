package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface IssueLabelRepository
    extends JpaRepository<IssueLabel, IssueLabelId>, JpaSpecificationExecutor<IssueLabel> {

  List<IssueLabel> findAllByIdWorkspaceIdAndIdIssueId(UUID workspaceId, UUID issueId);

  void deleteAllByIdWorkspaceIdAndIdIssueId(UUID workspaceId, UUID issueId);
}
