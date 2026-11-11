package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface InvitationRepository extends WorkspaceScopedRepository<Invitation> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Invitation> findByTokenHash(String tokenHash);
}
