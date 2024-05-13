package com.kcs.zolang.dto.response;

import java.io.Serializable;
import lombok.Builder;

@Builder
public record WorkloadOverviewDto(
    SimpleOverviewDto pod,
    SimpleOverviewDto deployment,
    SimpleOverviewDto replicaSet,
    SimpleOverviewDto statefulSet,
    SimpleOverviewDto daemonSet,
    SimpleOverviewDto job,
    SimpleOverviewDto cronJob
) implements Serializable {

    public static WorkloadOverviewDto of(int[] deploymentCount, int[] daemonSetCount,
        int[] replicaSetCount, int[] statefulSetCount, int[] cronJobCount, int[] jobCount,
        int[] podCount) {
        return WorkloadOverviewDto.builder().pod(SimpleOverviewDto.of(podCount[0], podCount[1]))
            .deployment(SimpleOverviewDto.of(deploymentCount[0], deploymentCount[1]))
            .replicaSet(SimpleOverviewDto.of(replicaSetCount[0], replicaSetCount[1]))
            .statefulSet(SimpleOverviewDto.of(statefulSetCount[0], statefulSetCount[1]))
            .daemonSet(SimpleOverviewDto.of(daemonSetCount[0], daemonSetCount[1]))
            .job(SimpleOverviewDto.of(jobCount[0], jobCount[1]))
            .cronJob(SimpleOverviewDto.of(cronJobCount[0], cronJobCount[1]))
            .build();
    }
}
