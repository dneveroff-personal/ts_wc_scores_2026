package com.tswcscores.repository;

import com.tswcscores.entity.ChatGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {
  Optional<ChatGroup> findByChatId(Long chatId);
}
