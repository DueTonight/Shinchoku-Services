package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

  Optional<User> findByEmail(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select entity from User entity where entity.id=:id")
  Optional<User> findLocked(UUID id);
}
