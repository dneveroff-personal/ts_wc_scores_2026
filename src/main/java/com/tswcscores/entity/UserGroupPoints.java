package com.tswcscores.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "user_group_points",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "chat_group_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroupPoints {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_group_id", nullable = false)
  private ChatGroup chatGroup;

  @Column(name = "points", nullable = false)
  @Builder.Default
  private int points = 0;
}
