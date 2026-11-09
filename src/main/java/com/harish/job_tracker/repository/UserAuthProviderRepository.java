
package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.UserAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAuthProviderRepository
        extends JpaRepository<UserAuthProvider, Long> {

    Optional<UserAuthProvider> findByProviderAndProviderUserId(
            String provider,
            String providerUserId
    );

    List<UserAuthProvider> findByUserId(Long userId);

    boolean existsByProviderAndProviderUserId(
            String provider,
            String providerUserId
    );
}
