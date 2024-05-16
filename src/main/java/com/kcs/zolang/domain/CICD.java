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
@Table(name = "repository")
public class CICD {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repository_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "repository_name", nullable = false)
    private String repositoryName;

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public CICD(User user, String repositoryName, String branchName) {
        this.user = user;
        this.repositoryName = repositoryName;
        this.createdAt = LocalDateTime.now();
        this.branchName = branchName;
    }

    public void update(String repositoryName, String branchName) {
        this.repositoryName = repositoryName;
        this.branchName = branchName;
    }
}
