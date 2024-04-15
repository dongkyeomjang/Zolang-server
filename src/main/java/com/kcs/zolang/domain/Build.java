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
    private Repository repository;

    @Column(name = "repository_number", nullable = false)
    private Integer repositoryNumber;

    @Column(name = "build_number", nullable = false)
    private Integer buildNumber;

    @Column(name = "build_status", nullable = false) // 0이면 빌드중, 1이면 빌드 성공, 2이면 빌드 실패
    private Integer buildStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Build(Repository repository, Integer repositoryNumber, Integer buildNumber, Integer buildStatus) {
        this.repository = repository;
        this.repositoryNumber = repositoryNumber;
        this.buildNumber = buildNumber;
        this.buildStatus = buildStatus;
        this.createdAt = LocalDateTime.now();
    }

    public void update(Integer buildStatus) {
        this.buildStatus = buildStatus;
    }
}
