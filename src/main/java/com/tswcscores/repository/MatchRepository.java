package com.tswcscores.repository;

import com.tswcscores.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByExternalId(Integer externalId);

    /** Матчи на следующие N часов */
    @Query("SELECT m FROM Match m WHERE m.utcDate BETWEEN :from AND :to AND m.status IN ('SCHEDULED','TIMED') ORDER BY m.utcDate")
    List<Match> findUpcoming(LocalDateTime from, LocalDateTime to);

    /** Завершённые матчи, очки по которым ещё не посчитаны */
    @Query("SELECT m FROM Match m WHERE m.status = 'FINISHED' AND m.scoresCalculated = false")
    List<Match> findFinishedNotCalculated();

    /** Матчи, которые начнутся через ~60 минут (для напоминаний) */
    @Query("SELECT m FROM Match m WHERE m.utcDate BETWEEN :from AND :to AND m.status IN ('SCHEDULED','TIMED')")
    List<Match> findMatchesStartingBetween(LocalDateTime from, LocalDateTime to);
}
