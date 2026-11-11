package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface UserSessionRepository
    extends JpaRepository<UserSession, UUID>, JpaSpecificationExecutor<UserSession> {

  Optional<UserSession> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
      String tokenHash, java.time.Instant now);

  List<UserSession> findAllByUserId(UUID userId);
}
