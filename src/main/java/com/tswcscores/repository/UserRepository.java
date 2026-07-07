package com.tswcscores.repository;

import com.tswcscores.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByTelegramId(Long telegramId);

  boolean existsByTelegramId(Long telegramId);

  @Query("SELECT u FROM User u WHERE u.active = true ORDER BY u.totalPoints DESC")
  List<User> findLeaderboard();
}
