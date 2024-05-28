package com.kcs.zolang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "build")
public class Build {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "build_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private CICD CICD;

    @Column(name = "last_commit_message", nullable = false)
    private String lastCommitMessage;

    @Column(name = "build_number", nullable = false)
    private Integer buildNumber;

    @Column(name = "build_status", nullable = false)
    private String buildStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Build(CICD CICD, String lastCommitMessage, Integer buildNumber, String buildStatus) {
        this.CICD = CICD;
        this.lastCommitMessage = lastCommitMessage;
        this.buildNumber = buildNumber;
        this.buildStatus = buildStatus;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String buildStatus) {
        this.buildStatus = buildStatus;
    }
}
