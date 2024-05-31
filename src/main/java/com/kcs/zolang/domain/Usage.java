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
@Table(name = "usages")
public class Usage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cpu_usage", nullable = false)
    private Double cpuUsage;
    @Column(name = "cpu_capacity", nullable = false)
    private Double cpuCapacity;
    @Column(name = "cpu_allo", nullable = false)
    private Double cpuAllo;

    @Column(name = "memory_usage", nullable = false)
    private Long memoryUsage;
    @Column(name = "memory_capacity", nullable = false)
    private Long memoryCapacity;
    @Column(name = "memory_allo", nullable = false)
    private Long memoryAllo;

    @Column(name = "pod_usage", nullable = false)
    private Integer podUsage;
    @Column(name = "pod_capacity", nullable = false)
    private Integer podCapacity;
    @Column(name = "pod_allo", nullable = false)
    private Integer podAllo;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @Builder
    public Usage(User user, Long id, Double cpuUsage, Double cpuCapacity, Double cpuAllo,
                 Long memoryUsage, Long memoryCapacity, Long memoryAllo,
                 Integer podUsage, Integer podCapacity, Integer podAllo) {
        this.user = user;
        this.id = id;
        this.cpuUsage = cpuUsage;
        this.cpuCapacity = cpuCapacity;
        this.cpuAllo = cpuAllo;
        this.memoryUsage = memoryUsage;
        this.memoryCapacity = memoryCapacity;
        this.memoryAllo = memoryAllo;
        this.podUsage = podUsage;
        this.podCapacity = podCapacity;
        this.podAllo = podAllo;
        this.createdAt = LocalDateTime.now();
    }

}

