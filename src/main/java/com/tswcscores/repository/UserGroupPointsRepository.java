package com.tswcscores.repository;

import com.tswcscores.entity.UserGroupPoints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserGroupPointsRepository extends JpaRepository<UserGroupPoints, Long> {

    Optional<UserGroupPoints> findByUserIdAndChatGroupId(Long userId, Long chatGroupId);

    List<UserGroupPoints> findAllByUserId(Long userId);

    @Query("SELECT ugp FROM UserGroupPoints ugp JOIN FETCH ugp.user WHERE ugp.chatGroup.id = :groupId ORDER BY ugp.points DESC")
    List<UserGroupPoints> findLeaderboardByGroup(Long groupId);
}
