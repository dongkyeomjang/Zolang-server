package com.kcs.zolang.dto.response.workload;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;
import static com.kcs.zolang.utility.MonitoringUtil.getDuration;

import io.kubernetes.client.openapi.models.V1Job;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "JobSimpleDto", description = "JobSimpleDto")
public record JobSimpleDto(
    @Schema(description = "Job 이름")
    String name,
    @Schema(description = "네임스페이스")
    String namespace,
    @Schema(description = "Job 완료수")
    Integer completions,
    @Schema(description = "Job 전체수")
    Integer total,
    @Schema(description = "Job 걸린시간")
    @Nullable
    String duration,
    @Schema(description = "Job 생성시간")
    String age,
    @Schema(description = "Job 라벨")
    Map<String, String> labels,
    @Schema(description = "Job 상태")
    String status
) {

    public static JobSimpleDto fromEntity(V1Job job) {
        OffsetDateTime startTime = job.getStatus().getStartTime();
        OffsetDateTime completionTime = job.getStatus().getCompletionTime();
        String duration = null;
        if (startTime != null && completionTime != null) {
            duration = getDuration(Duration.between(startTime, completionTime));
        }
        return JobSimpleDto.builder()
            .name(job.getMetadata().getName())
            .namespace(job.getMetadata().getNamespace())
            .completions(
                job.getSpec().getCompletions() == null ? 0 : job.getSpec().getCompletions())
            .total(job.getSpec().getParallelism() == null ? 0 : job.getSpec().getParallelism())
            .duration(duration)
            .age(getAge(job.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .labels(job.getMetadata().getLabels())
            .status(job.getStatus().getConditions() == null ? null
                : job.getStatus().getConditions().get(0).getType())
            .build();
    }
}
