package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.enums.WorkspaceRole;
import com.shinchoku.shinchokuapp.entity.id.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface WorkspaceMemberRepository
    extends JpaRepository<WorkspaceMember, WorkspaceMemberId>,
        JpaSpecificationExecutor<WorkspaceMember> {

  Optional<WorkspaceMember> findByIdWorkspaceIdAndIdUserIdAndRemovedAtIsNull(
      UUID workspaceId, UUID userId);

  List<WorkspaceMember> findAllByIdWorkspaceIdAndRemovedAtIsNull(UUID workspaceId);

  long countByIdWorkspaceIdAndRoleAndRemovedAtIsNull(UUID workspaceId, WorkspaceRole role);
}
