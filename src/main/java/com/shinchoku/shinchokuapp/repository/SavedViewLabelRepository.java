package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface SavedViewLabelRepository
    extends JpaRepository<SavedViewLabel, SavedViewLabelId>,
        JpaSpecificationExecutor<SavedViewLabel> {

  List<SavedViewLabel> findAllByIdWorkspaceIdAndIdViewId(UUID workspaceId, UUID viewId);

  void deleteAllByIdWorkspaceIdAndIdViewId(UUID workspaceId, UUID viewId);
}
