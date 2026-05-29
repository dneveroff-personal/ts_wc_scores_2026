package com.tswcscores.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private Integer externalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "short_name")
    private String shortName;

    @Column(name = "tla")
    private String tla;

    @Column(name = "crest_url")
    private String crestUrl;

    @Column(name = "group_name")
    private String groupName;

    public String getDisplayName() {
        return shortName != null ? shortName : name;
    }
}
