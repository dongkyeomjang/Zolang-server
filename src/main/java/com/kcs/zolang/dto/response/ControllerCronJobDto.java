package com.kcs.zolang.dto.response;

import static com.kcs.zolang.utility.MonitoringUtil.DATE_TIME_FORMATTER;
import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1CronJob;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

@Builder
@Schema(name = "ControllerCronJobDto", description = "크론잡 Dto")
public record ControllerCronJobDto(
    @Schema(description = "크론잡 이름", example = "cronjob-name")
    String name,
    @Schema(description = "크론잡 레이블", example = "app:nginx")
    Map<String, String> labels,
    @Schema(description = "크론잡 스케줄", example = "0 0 * * *")
    String schedule,
    @Schema(description = "크론잡 일시정지 여부", example = "false")
    boolean suspend,
    @Schema(description = "크론잡 활성화된 파드 수", example = "1")
    int active,
    @Schema(description = "크론잡 마지막 스케줄링 시간", example = "1d")
    String lastScheduling,
    @Schema(description = "크론잡 마지막 스케줄링 일시", example = "2021. 12. 01. 오후 12:00:00")
    String lastScheduleDateTime,
    @Schema(description = "크론잡 생성 시간", example = "1d")
    String age,
    @Schema(description = "크론잡 생성 일시", example = "2021. 12 .01. 오후 12:00:00")
    String creationDateTime) {

    public static ControllerCronJobDto fromEntity(V1CronJob cronJob) {
        return ControllerCronJobDto.builder()
            .name(Objects.requireNonNull(cronJob.getMetadata()).getName())
            .labels(cronJob.getMetadata().getLabels())
            .schedule(Objects.requireNonNull(cronJob.getSpec()).getSchedule())
            .suspend(cronJob.getSpec().getSuspend() != null && cronJob.getSpec().getSuspend())
            .active(cronJob.getStatus().getActive() == null ? 0
                : cronJob.getStatus().getActive().size())
            .lastScheduling(getAge(cronJob.getStatus().getLastScheduleTime().toLocalDateTime()))
            .lastScheduleDateTime(
                cronJob.getStatus().getLastScheduleTime().toLocalDateTime() == null ? null
                    : cronJob.getStatus().getLastScheduleTime().toLocalDateTime().format(
                        DATE_TIME_FORMATTER))
            .age(getAge(Objects.requireNonNull(cronJob.getMetadata().getCreationTimestamp())
                .toLocalDateTime()))
            .creationDateTime(
                cronJob.getMetadata().getCreationTimestamp().toLocalDateTime() == null ? null
                    : cronJob.getMetadata().getCreationTimestamp().toLocalDateTime()
                        .format(DATE_TIME_FORMATTER))
            .build();
    }
}
