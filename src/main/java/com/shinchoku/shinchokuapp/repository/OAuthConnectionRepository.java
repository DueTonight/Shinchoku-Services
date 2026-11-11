package com.shinchoku.shinchokuapp.repository;

import com.shinchoku.shinchokuapp.entity.*;
import com.shinchoku.shinchokuapp.entity.id.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface OAuthConnectionRepository
    extends JpaRepository<OAuthConnection, UUID>, JpaSpecificationExecutor<OAuthConnection> {

  Optional<OAuthConnection> findByProviderAndProviderSubject(
      String provider, String providerSubject);

  long countByUserId(UUID userId);
}
