package com.kcs.zolang.dto.response.workload;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;

import io.kubernetes.client.openapi.models.V1Job;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.Builder;

@Builder
@Schema(name = "JobSimpleDto", description = "JobSimpleDto")
public record JobSimpleDto(
    String name,
    String namespace,
    Integer completions,
    String duration,
    String age,
    Map<String, String> labels,
    String status
) {

    public static JobSimpleDto fromEntity(V1Job job) {
        return JobSimpleDto.builder()
            .name(job.getMetadata().getName())
            .namespace(job.getMetadata().getNamespace())
            .completions(job.getSpec().getCompletions())
            .duration(job.getStatus().getCompletionTime().toLocalDateTime().format(
                DateTimeFormatter.ofPattern("HH:mm:ss")))
            .age(getAge(job.getMetadata().getCreationTimestamp().toLocalDateTime()))
            .labels(job.getMetadata().getLabels())
            .status(job.getStatus().getConditions().get(0).getType())
            .build();
    }
}
