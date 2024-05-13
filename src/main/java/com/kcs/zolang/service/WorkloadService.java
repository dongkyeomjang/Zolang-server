package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.PodSimpleDto;
import com.kcs.zolang.dto.response.WorkloadOverviewDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final MonitoringUtil monitoringUtil;

    public WorkloadOverviewDto getOverview(Long userId) {
        monitoringUtil.getV1Api(userId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            CoreV1Api coreV1api = new CoreV1Api();
            BatchV1Api batchV1Api = new BatchV1Api();
            //deployment list
            List<V1Deployment> deployment = appsV1Api.listDeploymentForAllNamespaces().execute()
                .getItems();
            List<V1DaemonSet> daemonSet = appsV1Api.listDaemonSetForAllNamespaces().execute()
                .getItems();
            List<V1ReplicaSet> replicaSet = appsV1Api.listReplicaSetForAllNamespaces().execute()
                .getItems();
            List<V1StatefulSet> statefulSet = appsV1Api.listStatefulSetForAllNamespaces().execute()
                .getItems();
            List<V1CronJob> cronJobList = batchV1Api.listCronJobForAllNamespaces().execute()
                .getItems();
            List<V1Job> jobList = batchV1Api.listJobForAllNamespaces().execute().getItems();
            List<V1Pod> podList = coreV1api.listPodForAllNamespaces().execute().getItems();
            return getCountWorkloadOverview(deployment, daemonSet, replicaSet, statefulSet,
                cronJobList, jobList, podList);
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public WorkloadOverviewDto getNameSpaceOverview(Long userId, String namespace) {
        monitoringUtil.getV1Api(userId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            CoreV1Api coreV1api = new CoreV1Api();
            BatchV1Api batchV1Api = new BatchV1Api();
            //특정 네임스페이스의 deployment list
            List<V1Deployment> deployment = appsV1Api.listNamespacedDeployment(namespace).execute()
                .getItems();
            List<V1DaemonSet> daemonSet = appsV1Api.listNamespacedDaemonSet(namespace).execute()
                .getItems();
            List<V1ReplicaSet> replicaSet = appsV1Api.listNamespacedReplicaSet(namespace).execute()
                .getItems();
            List<V1StatefulSet> statefulSet = appsV1Api.listNamespacedStatefulSet(namespace)
                .execute().getItems();
            List<V1CronJob> cronJobList = batchV1Api.listNamespacedCronJob(namespace).execute()
                .getItems();
            List<V1Job> jobList = batchV1Api.listNamespacedJob(namespace).execute().getItems();
            List<V1Pod> podList = coreV1api.listNamespacedPod(namespace).execute().getItems();
            return getCountWorkloadOverview(deployment, daemonSet, replicaSet, statefulSet,
                cronJobList, jobList, podList);
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<PodSimpleDto> getPodList(Long userId) {
        monitoringUtil.getV1Api(userId);
        try {
            CoreV1Api api = new CoreV1Api();
            //모든 네임스페이스의 pod list
            return api.listPodForAllNamespaces().execute().getItems().stream().map(
                PodSimpleDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<PodSimpleDto> getPodListByNamespace(Long userId, String namespace) {
        monitoringUtil.getV1Api(userId);
        try {
            CoreV1Api api = new CoreV1Api();
            //특정 네임스페이스의 pod list
            return api.listNamespacedPod(namespace).execute().getItems().stream().map(
                PodSimpleDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    private WorkloadOverviewDto getCountWorkloadOverview(List<V1Deployment> deployment,
        List<V1DaemonSet> daemonSet, List<V1ReplicaSet> replicaSet, List<V1StatefulSet> statefulSet,
        List<V1CronJob> cronJobList, List<V1Job> jobList, List<V1Pod> podList) {
        int[] deploymentCount = {deployment.size(), 0};
        for (V1Deployment d : deployment) {
            //getAvailableReplicas()은 현재 사용중인 레플리카 수
            if (d.getStatus() != null) {
                if (d.getStatus().getAvailableReplicas() != null) {
                    deploymentCount[1]++;
                }
            }
        }
        int[] daemonSetCount = {daemonSet.size(), 0};
        for (V1DaemonSet d : daemonSet) {
            //getNumberAvailable()은 현재 사용중인 레플리카 수
            if (d.getStatus() != null) {
                if (d.getStatus().getNumberAvailable() != null) {
                    daemonSetCount[1]++;
                }
            }
        }
        int[] replicaSetCount = {replicaSet.size(), 0};
        for (V1ReplicaSet r : replicaSet) {
            if (r.getStatus() != null) {
                if (r.getStatus().getReplicas() != null) {
                    replicaSetCount[1]++;
                }
            }
        }
        int[] statefulSetCount = {statefulSet.size(), 0};
        for (V1StatefulSet s : statefulSet) {
            if (s.getStatus() != null) {
                if (s.getStatus().getAvailableReplicas() != null) {
                    statefulSetCount[1]++;
                }
            }
        }
        int[] cronJobCount = {cronJobList.size(), 0};
        for (V1CronJob c : cronJobList) {
            //getActive()은 현재 실행중인 크론잡
            if (c.getStatus() != null) {
                if (c.getStatus().getActive() != null) {
                    cronJobCount[1]++;
                }
            }
        }
        int[] jobCount = {jobList.size(), 0};
        for (V1Job j : jobList) {
            if (j.getStatus() != null) {
                if (j.getStatus().getActive() != null) {
                    jobCount[1]++;
                }
            }
        }
        int[] podCount = {podList.size(), 0};
        for (V1Pod p : podList) {
            if (p.getStatus() != null) {
                if (p.getStatus().getPhase().equals("Running")) {
                    podCount[1]++;
                }
            }
        }
        return WorkloadOverviewDto.of(deploymentCount, daemonSetCount, replicaSetCount,
            statefulSetCount, cronJobCount, jobCount, podCount);
    }
}
