
package com.harish.job_tracker.repository;

import com.harish.job_tracker.entity.VerificationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationChallengeRepository
        extends JpaRepository<VerificationChallenge, Long> {

    Optional<VerificationChallenge>
    findTopByDestinationAndChannelAndVerifiedAtIsNullOrderByCreatedAtDesc(
            String destination,
            String channel
    );

    Optional<VerificationChallenge>
findTopByDestinationAndChannelAndVerifiedAtIsNotNullOrderByCreatedAtDesc(
        String destination,
        String channel
);

    long countByDestinationAndChannelAndCreatedAtAfter(
            String destination,
            String channel,
            LocalDateTime createdAt
    );
}
