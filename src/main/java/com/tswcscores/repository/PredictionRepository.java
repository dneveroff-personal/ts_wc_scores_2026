package com.tswcscores.repository;

import com.tswcscores.entity.Prediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    Optional<Prediction> findByUserIdAndMatchId(Long userId, Long matchId);

    List<Prediction> findByMatchId(Long matchId);

    @Query("SELECT p FROM Prediction p JOIN FETCH p.match m WHERE p.user.id = :userId ORDER BY m.utcDate DESC")
    List<Prediction> findByUserIdWithMatch(Long userId);

    /** Пользователи без прогноза на конкретный матч */
    @Query("""
        SELECT u.telegramId FROM User u
        WHERE u.active = true
          AND u.id NOT IN (
              SELECT p.user.id FROM Prediction p WHERE p.match.id = :matchId
          )
    """)
    List<Long> findTelegramIdsWithoutPrediction(Long matchId);
}
