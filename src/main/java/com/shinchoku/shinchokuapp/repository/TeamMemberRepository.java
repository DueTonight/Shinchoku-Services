package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TeamMemberRepository
    extends JpaRepository<TeamMember, TeamMemberId>, JpaSpecificationExecutor<TeamMember> {

  Optional<TeamMember> findByIdWorkspaceIdAndIdTeamIdAndIdUserIdAndRemovedAtIsNull(
      UUID workspaceId, UUID teamId, UUID userId);

  List<TeamMember> findAllByIdWorkspaceIdAndIdUserIdAndRemovedAtIsNull(
      UUID workspaceId, UUID userId);
}
