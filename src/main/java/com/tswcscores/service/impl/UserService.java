package com.tswcscores.service.impl;

import com.tswcscores.entity.User;
import com.tswcscores.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional
  public User registerOrGet(Long telegramId, String username, String firstName, String lastName) {
    return userRepository
        .findByTelegramId(telegramId)
        .orElseGet(
            () -> {
              User user =
                  User.builder()
                      .telegramId(telegramId)
                      .username(username)
                      .firstName(firstName)
                      .lastName(lastName)
                      .registeredAt(LocalDateTime.now())
                      .build();
              log.info("Registered new user: {} ({})", user.getDisplayName(), telegramId);
              return userRepository.save(user);
            });
  }

  public Optional<User> findByTelegramId(Long telegramId) {
    return userRepository.findByTelegramId(telegramId);
  }

  public List<User> getLeaderboard() {
    return userRepository.findLeaderboard();
  }

  @Transactional
  public User updateTimezone(Long telegramId, String timezone) {
    User user =
        userRepository
            .findByTelegramId(telegramId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    user.setTimezone(timezone);
    log.info("Updated timezone for user {}: {}", telegramId, timezone);
    return userRepository.save(user);
  }
}
