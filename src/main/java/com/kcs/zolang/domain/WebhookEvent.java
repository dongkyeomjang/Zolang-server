package com.kcs.zolang.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventId;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Builder
    public WebhookEvent(String eventId, LocalDateTime receivedAt) {
        this.eventId = eventId;
        this.receivedAt = receivedAt;
    }
}
