package com.kcs.zolang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "bills")
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cpu_cost", nullable = false)
    private Double cpuCost;

    @Column(name = "memory_cost", nullable = false)
    private Double memoryCost;

    @Column(name = "pod_cost", nullable = false)
    private Double podCost;

    @Column(name = "runtime_cost", nullable = false)
    private Double runtimeCost;

    @Column(name = "date", nullable = false)
    private String date;


    @Builder
    public Bill(User user, Long id, Double cpuCost, Double memoryCost, Double podCost, Double runtimeCost, String date) {
        this.user = user;
        this.id = id;
        this.cpuCost = cpuCost;
        this.memoryCost = memoryCost;
        this.podCost = podCost;
        this.runtimeCost = runtimeCost;
        this.date = date;
    }

}
