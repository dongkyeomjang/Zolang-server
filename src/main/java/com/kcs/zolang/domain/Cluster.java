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
@Table(name = "cluster")
public class Cluster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cluster_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "cluster_name", nullable = false)
    private String clusterName;

    @Column(name = "secret_token", nullable = false, columnDefinition = "TEXT")
    private String secretToken;

    @Column(name = "domain_url", nullable = false)
    private String domainUrl;

    @Column(name = "version", nullable = false)
    private String version;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Cluster(User user, String clusterName, String provider, String secretToken, String domainUrl, String version, String certPath) {
        this.user = user;
        this.clusterName = clusterName;
        this.provider = provider;
        this.secretToken = secretToken;
        this.domainUrl = domainUrl;
        this.version = version;
        this.createdAt = LocalDateTime.now();
    }
    public void update(String clusterName, String secretToken, String domainUrl, String version) {
        this.clusterName = clusterName;
        this.secretToken = secretToken;
        this.domainUrl = domainUrl;
        this.version = version;
    }
}
