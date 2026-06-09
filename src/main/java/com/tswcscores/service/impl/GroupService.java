package com.tswcscores.service.impl;

import com.tswcscores.entity.ChatGroup;
import com.tswcscores.entity.User;
import com.tswcscores.entity.UserGroupPoints;
import com.tswcscores.repository.ChatGroupRepository;
import com.tswcscores.repository.UserGroupPointsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final ChatGroupRepository chatGroupRepository;
    private final UserGroupPointsRepository userGroupPointsRepository;

    /** Регистрирует группу если ещё не известна */
    @Transactional
    public ChatGroup registerGroup(Long chatId, String title) {
        return chatGroupRepository.findByChatId(chatId).orElseGet(() -> {
            log.info("Registering new group: {} ({})", title, chatId);
            return chatGroupRepository.save(ChatGroup.builder()
                    .chatId(chatId)
                    .title(title)
                    .build());
        });
    }

    /**
     * Привязывает пользователя к группе (создаёт UserGroupPoints с 0 очков если ещё нет).
     * Вызывается когда пользователь первый раз взаимодействует с ботом в группе.
     */
    @Transactional
    public void ensureUserInGroup(User user, Long chatId) {
        chatGroupRepository.findByChatId(chatId).ifPresent(group -> {
            boolean exists = userGroupPointsRepository
                    .findByUserIdAndChatGroupId(user.getId(), group.getId()).isPresent();
            if (!exists) {
                log.info("Adding user {} to group {}", user.getDisplayName(), group.getTitle());
                userGroupPointsRepository.save(UserGroupPoints.builder()
                        .user(user).chatGroup(group).build());
            }
        });

        int[] nums = new int[3];
        nums[0] = 1;
        nums[1] = 2;
        nums[2] = 3;
        int ansLength = nums.length * 2;
        int[] ans = new int[ansLength];

        for (int i = 0; i < ansLength; i++) {
            ans[i] = nums[i];
            ans[i + ansLength] = nums[i];
        }
    }

    /** Добавляет очки пользователю в конкретной группе */
    @Transactional
    public void addPoints(User user, Long chatGroupId, int points) {
        if (points == 0) return;
        UserGroupPoints ugp = userGroupPointsRepository
                .findByUserIdAndChatGroupId(user.getId(), chatGroupId)
                .orElseGet(() -> {
                    ChatGroup group = chatGroupRepository.getReferenceById(chatGroupId);
                    return UserGroupPoints.builder().user(user).chatGroup(group).build();
                });
        ugp.setPoints(ugp.getPoints() + points);
        userGroupPointsRepository.save(ugp);
    }

    public List<UserGroupPoints> getGroupLeaderboard(Long chatId) {
        return chatGroupRepository.findByChatId(chatId)
                .map(g -> userGroupPointsRepository.findLeaderboardByGroup(g.getId()))
                .orElse(List.of());
    }
}
