package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.network.ServiceListDto;
import com.kcs.zolang.dto.response.workload.CommonControllerDetailDto;
import com.kcs.zolang.dto.response.workload.CommonControllerDto;
import com.kcs.zolang.dto.response.workload.CommonControllerListDto;
import com.kcs.zolang.dto.response.workload.ControllerCronJobDto;
import com.kcs.zolang.dto.response.workload.CronJobListDto;
import com.kcs.zolang.dto.response.workload.DeploymentDetailDto;
import com.kcs.zolang.dto.response.workload.JobListDto;
import com.kcs.zolang.dto.response.workload.JobSimpleDto;
import com.kcs.zolang.dto.response.workload.PodControlledDto;
import com.kcs.zolang.dto.response.workload.PodDetailDto;
import com.kcs.zolang.dto.response.workload.PodListDto;
import com.kcs.zolang.dto.response.workload.PodMetricsDto;
import com.kcs.zolang.dto.response.workload.PodPersistentVolumeClaimDto;
import com.kcs.zolang.dto.response.workload.PodSimpleDto;
import com.kcs.zolang.dto.response.workload.UsageDto;
import com.kcs.zolang.dto.response.workload.WorkloadOverviewDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1CronJobList;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetList;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import io.kubernetes.client.openapi.models.V1ReplicaSetList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import io.kubernetes.client.openapi.models.V1StatefulSetList;
import io.kubernetes.client.openapi.models.V1Volume;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final MonitoringUtil monitoringUtil;

    private final RedisTemplate<String, Object> redisTemplate;


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
            throw getApiError(e);
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
            throw getApiError(e);
        }
    }

    public PodListDto getPodList(Long userId, Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        int m = getMinute(clusterId);
        try {
            CoreV1Api coreV1Api = new CoreV1Api();
            V1PodList podList = coreV1Api.listPodForAllNamespaces().limit(10)
                ._continue(continueToken).execute();
            if (podList.getItems().isEmpty()) {
                return null;
            }
            List<PodSimpleDto> podSimpleDtoList = getPodSimpleDtoList(clusterId, podList.getItems(),
                m);

            List<String> totalKeys = new ArrayList<>();
            for (int i = 26; i > 0; i -= 2) {
                totalKeys.add(
                    "cluster-usage:" + clusterId + ":totalCpuUsage:" + ((60 + (m - i)) % 60));
            }
            List<UsageDto> totalUsage = getUsage(totalKeys);
            String startName = podList.getItems().get(0).getMetadata().getName();
            List<String> podNames = coreV1Api.listPodForAllNamespaces().execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(podNames, startName);
            return PodListDto.fromEntity(totalUsage, podSimpleDtoList,
                podList.getMetadata().getContinue(), startIndex, podNames.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public PodListDto getPodListByNamespace(Long userId, String namespace, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        int m = getMinute(clusterId);
        try {
            //파드 사용량 추출
            CoreV1Api coreV1Api = new CoreV1Api();
            V1PodList podList = coreV1Api.listNamespacedPod(namespace).execute();
            if (podList.getItems().isEmpty()) {
                return null;
            }
            List<PodSimpleDto> podSimpleDtoList = getPodSimpleDtoList(clusterId, podList.getItems(),
                m);
            List<String> keys = new ArrayList<>();
            for (int i = 26; i > 0; i -= 2) {
                keys.add("cluster-usage:" + clusterId + ":" + namespace + ":" + (
                    (60 + (m - i)) % 60));
            }
            List<UsageDto> totalUsage = getUsage(keys);

            List<String> podNames = coreV1Api.listNamespacedPod(namespace).execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            String startName = podList.getItems().get(0).getMetadata().getName();
            int startIndex = getStartIndex(podNames, startName);
            //특정 네임스페이스의 pod list
            return PodListDto.fromEntity(totalUsage, podSimpleDtoList,
                podList.getMetadata().getContinue(), startIndex, podNames.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public PodDetailDto getPodDetail(Long userId, String name, String namespace, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        int m = getMinute(clusterId);
        try {
            List<UsageDto> podUsage = getPodMetrics(clusterId, name, m);
            CoreV1Api coreV1Api = new CoreV1Api();
            V1Pod pod = coreV1Api.readNamespacedPod(name, namespace).execute();
            if (pod == null) {
                return null;
            }
            V1OwnerReference ownerReference = pod.getMetadata().getOwnerReferences().get(0);
            PodControlledDto controlledDto = getControlled(ownerReference.getKind(),
                ownerReference.getName(), namespace);
            List<PodPersistentVolumeClaimDto> pvcDtoList = new ArrayList<>();
            List<V1Volume> volumes = pod.getSpec().getVolumes();
            if (volumes != null) {
                for (V1Volume v : volumes) {
                    if (v.getPersistentVolumeClaim() != null) {
                        pvcDtoList.add(getPersistentVolumeClaim(coreV1Api,
                            v.getPersistentVolumeClaim().getClaimName(), namespace));
                    }
                }
            }
            return PodDetailDto.fromEntity(pod, controlledDto, pvcDtoList, podUsage, volumes);
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getDeploymentList(Long userId, Long clusterId,
        String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1DeploymentList deploymentList = appsV1Api.listDeploymentForAllNamespaces().limit(10)
                ._continue(continueToken).execute();
            if (deploymentList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllerDto = deploymentList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listDeploymentForAllNamespaces().execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            String startName = commonControllerDto.get(0).name();
            int startIndex = getStartIndex(names, startName);
            return CommonControllerListDto.fromEntity(commonControllerDto,
                deploymentList.getMetadata().getContinue(), startIndex, names.size());

        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public DeploymentDetailDto getDeploymentDetail(Long userId, String name, String namespace,
        Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1Deployment deployment = appsV1Api.readNamespacedDeployment(name, namespace).execute();
            return DeploymentDetailDto.fromEntity(deployment);
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getDeploymentListByNamespace(Long userId, String namespace,
        Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1DeploymentList deploymentList = appsV1Api.listNamespacedDeployment(namespace)
                .limit(10)._continue(continueToken).execute();
            if (deploymentList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllers = deploymentList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listNamespacedDeployment(namespace).execute().getItems()
                .stream()
                .map(it -> it.getMetadata().getName()).toList();
            String startName = commonControllers.get(0).name();
            int startIndex = getStartIndex(names, startName);
            return CommonControllerListDto.fromEntity(commonControllers,
                deploymentList.getMetadata().getContinue(), startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getDaemonSetList(Long userId, Long clusterId,
        String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1DaemonSetList daemonSetList = appsV1Api.listDaemonSetForAllNamespaces().limit(10)
                ._continue(continueToken).execute();
            if (daemonSetList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllers = daemonSetList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listDaemonSetForAllNamespaces().execute().getItems()
                .stream()
                .map(it -> it.getMetadata().getName()).toList();
            String startName = commonControllers.get(0).name();
            int startIndex = getStartIndex(names, startName);
            return CommonControllerListDto.fromEntity(commonControllers,
                daemonSetList.getMetadata().getContinue(), startIndex, commonControllers.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getDaemonSetListByNamespace(Long userId, String namespace,
        Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1DaemonSetList daemonSetList = appsV1Api.listNamespacedDaemonSet(namespace).limit(10)
                ._continue(continueToken).execute();
            if (daemonSetList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllers = daemonSetList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listNamespacedDaemonSet(namespace).execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            String startName = commonControllers.get(0).name();
            int startIndex = getStartIndex(names, startName);
            return CommonControllerListDto.fromEntity(commonControllers,
                daemonSetList.getMetadata().getContinue(), startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerDetailDto getDaemonSetDetail(Long userId, String name, String namespace,
        Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1DaemonSet daemonSet = appsV1Api.readNamespacedDaemonSet(name, namespace).execute();
            String kind = "DaemonSet";
            String controllerName = daemonSet.getMetadata().getName();
            String namespaceName = daemonSet.getMetadata().getNamespace();
            List<PodSimpleDto> podList = getControllerPodList(clusterId, controllerName,
                namespaceName, kind);
            String k8sApp = daemonSet.getMetadata().getLabels().get("app");
            List<ServiceListDto> serviceList = getControllerServiceList(k8sApp,
                namespaceName);
            return CommonControllerDetailDto.fromEntity(daemonSet, podList, serviceList);
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getReplicaSetList(Long userId, Long clusterId,
        String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1ReplicaSetList replicaSetList = appsV1Api.listReplicaSetForAllNamespaces().limit(10)
                ._continue(continueToken).execute();
            if (replicaSetList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllers = replicaSetList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listReplicaSetForAllNamespaces().execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, commonControllers.get(0).name());
            return CommonControllerListDto.fromEntity(commonControllers,
                replicaSetList.getMetadata().getContinue(), startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getReplicaSetListByNamespace(Long userId, String namespace,
        Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1ReplicaSetList replicaSetList = appsV1Api.listNamespacedReplicaSet(namespace)
                .limit(10)._continue(continueToken).execute();
            if (replicaSetList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllers = replicaSetList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listNamespacedReplicaSet(namespace).execute().getItems()
                .stream()
                .map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, commonControllers.get(0).name());
            return CommonControllerListDto.fromEntity(commonControllers,
                replicaSetList.getMetadata().getContinue(), startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerDetailDto getReplicaSetDetail(Long userId, String name, String namespace,
        Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1ReplicaSet replicaSet = appsV1Api.readNamespacedReplicaSet(name, namespace).execute();
            String kind = "ReplicaSet";
            String controllerName = replicaSet.getMetadata().getName();
            String namespaceName = replicaSet.getMetadata().getNamespace();
            List<PodSimpleDto> podList = getControllerPodList(clusterId, controllerName,
                namespaceName, kind);
            String k8sApp = replicaSet.getMetadata().getLabels().get("app");
            List<ServiceListDto> serviceList = getControllerServiceList(k8sApp,
                namespaceName);
            return CommonControllerDetailDto.fromEntity(replicaSet, podList, serviceList);
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getStatefulSetList(Long userId, Long clusterId,
        String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1StatefulSetList statefulSetList = appsV1Api.listStatefulSetForAllNamespaces()
                .limit(10)._continue(continueToken).execute();
            if (statefulSetList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllers = statefulSetList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listStatefulSetForAllNamespaces().execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, commonControllers.get(0).name());
            return CommonControllerListDto.fromEntity(commonControllers,
                statefulSetList.getMetadata().getContinue(), startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerListDto getStatefulSetListByNamespace(Long userId, String namespace,
        Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1StatefulSetList statefulSetList = appsV1Api.listNamespacedStatefulSet(namespace)
                .limit(10)._continue(continueToken).execute();
            if (statefulSetList.getItems().isEmpty()) {
                return null;
            }
            List<CommonControllerDto> commonControllers = statefulSetList.getItems().stream()
                .map(CommonControllerDto::fromEntity).toList();
            List<String> names = appsV1Api.listNamespacedStatefulSet(namespace).execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, commonControllers.get(0).name());
            return CommonControllerListDto.fromEntity(commonControllers,
                statefulSetList.getMetadata().getContinue(), startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CommonControllerDetailDto getStatefulSetDetail(Long userId, String name,
        String namespace, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            V1StatefulSet statefulSet = appsV1Api.readNamespacedStatefulSet(name, namespace)
                .execute();
            List<PodSimpleDto> pods = getControllerPodList(clusterId,
                statefulSet.getMetadata().getName(), statefulSet.getMetadata().getNamespace(),
                statefulSet.getKind());
            return CommonControllerDetailDto.fromEntity(statefulSet, pods);
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CronJobListDto getCronJobList(Long userId, Long clusterId,
        String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            BatchV1Api batchV1Api = new BatchV1Api();
            V1CronJobList cronJobList = batchV1Api.listCronJobForAllNamespaces().limit(10)
                ._continue(continueToken).execute();
            if (cronJobList.getItems().isEmpty()) {
                return null;
            }
            List<ControllerCronJobDto> cronJobs = cronJobList.getItems().stream()
                .map(ControllerCronJobDto::fromEntity).toList();
            List<String> names = batchV1Api.listCronJobForAllNamespaces().execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, cronJobs.get(0).name());
            return CronJobListDto.fromEntity(cronJobs, cronJobList.getMetadata().getContinue(),
                startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public CronJobListDto getCronJobListByNamespace(Long userId, String namespace,
        Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            BatchV1Api batchV1Api = new BatchV1Api();
            V1CronJobList cronJobList = batchV1Api.listNamespacedCronJob(namespace).limit(10)
                ._continue(continueToken).execute();
            if (cronJobList.getItems().isEmpty()) {
                return null;
            }
            List<ControllerCronJobDto> cronJobs = cronJobList.getItems().stream()
                .map(ControllerCronJobDto::fromEntity).toList();
            List<String> names = batchV1Api.listNamespacedCronJob(namespace).execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, cronJobs.get(0).name());
            return CronJobListDto.fromEntity(cronJobs, cronJobList.getMetadata().getContinue(),
                startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public JobListDto getJobList(Long userId, Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            BatchV1Api batchV1Api = new BatchV1Api();
            V1JobList jobList = batchV1Api.listJobForAllNamespaces().limit(10)
                ._continue(continueToken).execute();
            if (jobList.getItems().isEmpty()) {
                return null;
            }
            List<JobSimpleDto> jobSimpleDto = jobList.getItems().stream()
                .map(JobSimpleDto::fromEntity).toList();
            List<String> names = batchV1Api.listJobForAllNamespaces().execute().getItems().stream()
                .map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, jobSimpleDto.get(0).name());
            return JobListDto.fromEntity(jobSimpleDto, jobList.getMetadata().getContinue(),
                startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public JobListDto getJobListByNamespace(Long userId, String namespace,
        Long clusterId, String continueToken) {
        monitoringUtil.getV1Api(userId, clusterId);
        try {
            BatchV1Api batchV1Api = new BatchV1Api();
            V1JobList jobList = batchV1Api.listNamespacedJob(namespace).limit(10)
                ._continue(continueToken).execute();
            if (jobList.getItems().isEmpty()) {
                return null;
            }
            List<JobSimpleDto> jobSimpleDto = jobList.getItems().stream()
                .map(JobSimpleDto::fromEntity).toList();
            List<String> names = batchV1Api.listNamespacedJob(namespace).execute().getItems()
                .stream().map(it -> it.getMetadata().getName()).toList();
            int startIndex = getStartIndex(names, jobSimpleDto.get(0).name());
            return JobListDto.fromEntity(jobSimpleDto, jobList.getMetadata().getContinue(),
                startIndex, names.size());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    public PodMetricsDto getControllerPodMetrics(Long userId, String name, Long clusterId) {
        monitoringUtil.getV1Api(userId, clusterId);
        int m = getMinute(clusterId);
        String key = "cluster-usage:" + clusterId + ":" + name + ":" + m;
        return PodMetricsDto.fromEntity(getUsage(List.of(key)).get(0),
            getPodMetrics(clusterId, name, m));
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
            throw getApiError(e);
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

    private PodPersistentVolumeClaimDto getPersistentVolumeClaim(CoreV1Api coreV1Api, String name,
        String namespace) {
        try {
            return PodPersistentVolumeClaimDto.fromEntity(
                coreV1Api.readNamespacedPersistentVolumeClaim(name, namespace).execute());
        } catch (ApiException e) {
            throw getApiError(e);
        }
    }

    private List<UsageDto> getUsage(List<String> keys) {
        List<UsageDto> usageDtoList = new ArrayList<>();
        for (String key : keys) {
            usageDtoList.add(redisTemplate.opsForValue().get(key) == null ? null
                : (UsageDto) redisTemplate.opsForValue().get(key));
        }
        return usageDtoList;
    }

    private List<UsageDto> getPodMetrics(Long clusterId, String name, int m) {
        List<String> keys = new ArrayList<>();
        for (int i = 26; i > 0; i = i - 2) {
            keys.add("cluster-usage:" + clusterId + ":" + name + ":" + ((60 + (m - i)) % 60));
        }
        return getUsage(keys);
    }

    private List<PodSimpleDto> getPodSimpleDtoList(Long clusterId, List<V1Pod> podList, int m) {
        List<PodSimpleDto> podSimpleDtoList = new ArrayList<>();
        String key;
        for (V1Pod p : podList) {
            key = "cluster-usage:" + clusterId + ":" + p.getMetadata().getName() + ":" + m;
            podSimpleDtoList.add(
                PodSimpleDto.fromEntity(p, getUsage(List.of(key)).get(0),
                    getPodMetrics(clusterId, p.getMetadata().getName(), m)));
        }
        return podSimpleDtoList;
    }

    private List<PodSimpleDto> getControllerPodList(Long clusterId, String name, String namespace,
        String kind) {
        CoreV1Api coreV1Api = new CoreV1Api();
        List<V1Pod> pods = new ArrayList<>();
        int m = LocalDateTime.now().minusSeconds(10).getMinute();
        try {
            List<V1Pod> podList = coreV1Api.listNamespacedPod(namespace).execute().getItems();
            for (V1Pod item : podList) {
                if (item.getMetadata().getOwnerReferences() != null) {
                    V1OwnerReference owner = item.getMetadata().getOwnerReferences().get(0);
                    if (owner.getKind().equalsIgnoreCase(kind) && owner.getName().equals(name)) {
                        pods.add(item);
                    }
                }
            }
        } catch (ApiException e) {
            throw getApiError(e);
        }
        return getPodSimpleDtoList(clusterId, pods, m);
    }

    private List<ServiceListDto> getControllerServiceList(String k8sApp, String namespace) {
        if (k8sApp == null) {
            return new ArrayList<>();
        }
        CoreV1Api coreV1Api = new CoreV1Api();
        List<V1Service> services = new ArrayList<>();
        try {
            List<V1Service> serviceList = coreV1Api.listNamespacedService(namespace).execute()
                .getItems();
            for (V1Service item : serviceList) {
                String selector = item.getSpec().getSelector().get("app");
                if (selector != null) {
                    if (selector.equals(k8sApp)) {
                        services.add(item);
                    }
                }
            }
        } catch (ApiException e) {
            throw getApiError(e);
        }
        return services.stream().map(ServiceListDto::fromEntity).toList();
    }

    private Integer getStartIndex(List<String> names, String startName) {
        int startCount = 1;
        for (String name : names) {
            if (name.equals(startName)) {
                break;
            }
            startCount++;
        }
        return startCount;
    }

    private CommonException getApiError(ApiException e) {
        if (e.getCode() == 401) {
            return new CommonException(ErrorCode.INVALID_TOKEN_ERROR);
        }
        if (e.getCode() == 403) {
            return new CommonException(ErrorCode.ACCESS_DENIED);
        }
        if (e.getCode() == 404) {
            return new CommonException(ErrorCode.NOT_FOUND_RESOURCE);
        }
        return new CommonException(ErrorCode.API_ERROR);
    }

    private int getMinute(Long clusterId) {
        int m = LocalDateTime.now().minusSeconds(10).getMinute();
        int end = m - 10;
        while (m > end) {
            if (getUsage(List.of(
                "cluster-usage:" + clusterId + ":totalCpuUsage:" + ((60 + (m)) % 60))).get(0)
                == null) {
                m--;
            } else {
                break;
            }
        }
        return m;
    }
}
