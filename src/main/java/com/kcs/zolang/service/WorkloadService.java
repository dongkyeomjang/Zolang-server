package com.kcs.zolang.service;

import static com.kcs.zolang.utility.MonitoringUtil.getAge;
import static io.kubernetes.client.extended.kubectl.Kubectl.top;

import com.kcs.zolang.dto.response.CommonControllerDto;
import com.kcs.zolang.dto.response.ControllerCronJobDto;
import com.kcs.zolang.dto.response.PodControlledDto;
import com.kcs.zolang.dto.response.PodDetailDto;
import com.kcs.zolang.dto.response.PodPersistentVolumeClaimDto;
import com.kcs.zolang.dto.response.PodSimpleDto;
import com.kcs.zolang.dto.response.WorkloadOverviewDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.extended.kubectl.exception.KubectlException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1Volume;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final MonitoringUtil monitoringUtil;

    public WorkloadOverviewDto getOverview(Long userId, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
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

    public WorkloadOverviewDto getNameSpaceOverview(Long userId, String namespace, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
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

    public List<PodSimpleDto> getPodList(Long userId, Long clusterId) {
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        try {
            CoreV1Api coreV1Api = new CoreV1Api();
            V1NamespaceList namespaceList = coreV1Api.listNamespace().execute();
            List<PodSimpleDto> podSimpleDtoList = new ArrayList<>();
            for (V1Namespace ns : namespaceList.getItems()) {
                podSimpleDtoList.addAll(top(V1Pod.class,
                    PodMetrics.class).apiClient(client)
                    .namespace(Objects.requireNonNull(ns.getMetadata()).getName()).execute()
                    .stream().map(it -> PodSimpleDto.fromEntity(it.getLeft(), it.getRight()))
                    .toList());
            }
            //모든 네임스페이스의 pod list
            return coreV1Api.listPodForAllNamespaces().execute().getItems().stream()
                .map(PodSimpleDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        } catch (KubectlException e) {
            throw new CommonException(ErrorCode.KUBECTL_ERROR);
        }
    }

    public List<PodSimpleDto> getPodListByNamespace(Long userId, String namespace, Long clusterId) {
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        try {
            CoreV1Api coreV1Api = new CoreV1Api();
            //파드 사용량 추출
            List<PodSimpleDto> podUsage = top(V1Pod.class, PodMetrics.class).apiClient(
                    client).namespace(namespace).execute().stream()
                .map(it -> PodSimpleDto.fromEntity(it.getLeft(), it.getRight())).toList();
            //특정 네임스페이스의 pod list
            return coreV1Api.listNamespacedPod(namespace).execute().getItems().stream()
                .map(PodSimpleDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        } catch (KubectlException e) {
            throw new RuntimeException(e);
        }
    }

    public PodDetailDto getPodDetail(Long userId, String name, String namespace, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            CoreV1Api coreV1Api = new CoreV1Api();
            V1Pod pod = coreV1Api.readNamespacedPod(name, namespace).execute();
            String podNamespace = Objects.requireNonNull(pod.getMetadata()).getNamespace();
            List<PodControlledDto> controlledDtoList = pod.getMetadata().getOwnerReferences()
                .stream().map(it ->
                    getControlled(it.getKind(), it.getName(), podNamespace))
                .toList();
            List<PodPersistentVolumeClaimDto> pvcDtoList = new ArrayList<>();
            List<V1Volume> podSpec = pod.getSpec().getVolumes();
            for (V1Volume v : podSpec) {
                if (v.getPersistentVolumeClaim() != null) {
                    pvcDtoList.add(getPersistentVolumeClaim(coreV1Api,
                        v.getPersistentVolumeClaim().getClaimName(), podNamespace));
                }
            }
            return PodDetailDto.fromEntity(pod,
                getAge(Objects.requireNonNull(pod.getMetadata().getCreationTimestamp())
                    .toLocalDateTime()),
                controlledDtoList, pvcDtoList);
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<CommonControllerDto> getDeploymentList(Long userId, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            return appsV1Api.listDeploymentForAllNamespaces().execute()
                .getItems().stream().map(CommonControllerDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<CommonControllerDto> getDaemonSetList(Long userId, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            return appsV1Api.listDaemonSetForAllNamespaces().execute()
                .getItems().stream().map(CommonControllerDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<CommonControllerDto> getReplicaSetList(Long userId, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            return appsV1Api.listReplicaSetForAllNamespaces().execute()
                .getItems().stream().map(CommonControllerDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<CommonControllerDto> getStatefulSetList(Long userId, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            return appsV1Api.listStatefulSetForAllNamespaces().execute()
                .getItems().stream().map(CommonControllerDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<ControllerCronJobDto> getCronJobList(Long userId, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            BatchV1Api batchV1Api = new BatchV1Api();
            return batchV1Api.listCronJobForAllNamespaces().execute()
                .getItems().stream().map(ControllerCronJobDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<CommonControllerDto> getJobList(Long userId, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            BatchV1Api batchV1Api = new BatchV1Api();
            return batchV1Api.listJobForAllNamespaces().execute()
                .getItems().stream().map(CommonControllerDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    private PodControlledDto getControlled(String kind, String name, String namespace) {
        AppsV1Api appsV1Api = new AppsV1Api();
        BatchV1Api batchV1Api = new BatchV1Api();
        try {
            switch (kind) {
                case "Deployment":
                    return PodControlledDto.fromEntity(
                        appsV1Api.readNamespacedDeployment(name, namespace).execute());
                case "DaemonSet":
                    return PodControlledDto.fromEntity(
                        appsV1Api.readNamespacedDaemonSet(name, namespace).execute());
                case "ReplicaSet":
                    return PodControlledDto.fromEntity(
                        appsV1Api.readNamespacedReplicaSet(name, namespace).execute());
                case "StatefulSet":
                    return PodControlledDto.fromEntity(
                        appsV1Api.readNamespacedStatefulSet(name, namespace).execute());
                case "CronJob":
                    return PodControlledDto.fromEntity(
                        batchV1Api.readNamespacedCronJob(name, namespace).execute());
                case "Job":
                    return PodControlledDto.fromEntity(
                        batchV1Api.readNamespacedJob(name, namespace).execute());
            }
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
        return null;
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
                replicaSetCount[1]++;
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

    private PodPersistentVolumeClaimDto getPersistentVolumeClaim(CoreV1Api coreV1Api,
        String name, String podNamespace) {
        try {
            return PodPersistentVolumeClaimDto.fromEntity(
                coreV1Api.readNamespacedPersistentVolumeClaim(name, podNamespace).execute());
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }
}
