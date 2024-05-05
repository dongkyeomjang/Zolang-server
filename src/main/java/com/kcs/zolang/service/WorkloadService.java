package com.kcs.zolang.service;

import com.kcs.zolang.domain.Cluster;
import com.kcs.zolang.dto.response.PodSimpleDto;
import com.kcs.zolang.dto.response.UserUrlTokenDto;
import com.kcs.zolang.dto.response.WorkloadOverviewDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
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
import io.kubernetes.client.util.Config;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final ClusterRepository clusterRepository;

    public static String getAge(LocalDateTime created) {
        LocalDateTime now = LocalDateTime.now();
        String extension;
        int age;
        int passedHour = now.getMinute() > created.getMinute() ? 0 : 1;
        int passedDay = now.getHour() - passedHour > created.getHour() ? 0 : 1;
        int passedYear = now.getDayOfYear() - passedDay > created.getDayOfYear() ? 0 : 1;
        age = now.getYear() - created.getYear() - passedYear;
        if (age < 1) {
            age = (365 + (now.getDayOfYear() - created.getDayOfYear() - passedDay)) % 365;
            if (age < 1) {
                age = now.getHour() - created.getHour() - passedHour;
                if (age < 1) {
                    age = now.getMinute() - created.getMinute();
                    extension = "m";
                } else {
                    extension = "h";
                }
            } else {
                extension = "d";
            }
        } else {
            extension = "y";
        }
        return age + extension;
    }

    public WorkloadOverviewDto getOverview(Long userId) {
        getV1Api(userId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            CoreV1Api coreV1api = new CoreV1Api();
            BatchV1Api batchV1Api = new BatchV1Api();
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
        getV1Api(userId);
        try {
            AppsV1Api appsV1Api = new AppsV1Api();
            CoreV1Api coreV1api = new CoreV1Api();
            BatchV1Api batchV1Api = new BatchV1Api();
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
        getV1Api(userId);
        try {
            CoreV1Api api = new CoreV1Api();
            return api.listPodForAllNamespaces().execute().getItems().stream().map(
                PodSimpleDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public List<PodSimpleDto> getPodListByNamespace(Long userId, String namespace) {
        getV1Api(userId);
        try {
            CoreV1Api api = new CoreV1Api();
            return api.listNamespacedPod(namespace).execute().getItems().stream().map(
                PodSimpleDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    private void getV1Api(Long userId) {
        List<Cluster> clusters = clusterRepository.findByUserId(userId);
        if (clusters.isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_CLUSTER);
        }
        UserUrlTokenDto userUrlTokenDto = UserUrlTokenDto.fromEntity(clusters.get(0));
        ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
            userUrlTokenDto.token(), false);
        //TODO: SSL 인증서 추가
        /*InputStream caCertInputStream = new ByteArrayInputStream(userUrlTokenDto.caCert().getBytes(StandardCharsets.UTF_8));
        client.setSslCaCert(caCertInputStream);*/
        Configuration.setDefaultApiClient(client);
    }

    private WorkloadOverviewDto getCountWorkloadOverview(List<V1Deployment> deployment,
        List<V1DaemonSet> daemonSet, List<V1ReplicaSet> replicaSet, List<V1StatefulSet> statefulSet,
        List<V1CronJob> cronJobList, List<V1Job> jobList, List<V1Pod> podList) {
        int[] deploymentCount = {deployment.size(), 0};
        for (V1Deployment d : deployment) {
            if (d.getStatus().getAvailableReplicas() != null) {
                deploymentCount[1]++;
            }
        }
        int[] daemonSetCount = {daemonSet.size(), 0};
        for (V1DaemonSet d : daemonSet) {
            if (d.getStatus().getNumberAvailable() != 0) {
                daemonSetCount[1]++;
            }
        }
        int[] replicaSetCount = {replicaSet.size(), 0};
        for (V1ReplicaSet r : replicaSet) {
            if (r.getStatus().getReplicas() != null) {
                replicaSetCount[1]++;
            }
        }
        int[] statefulSetCount = {statefulSet.size(), 0};
        for (V1StatefulSet s : statefulSet) {
            if (s.getStatus().getAvailableReplicas() != null) {
                statefulSetCount[1]++;
            }
        }
        int[] cronJobCount = {cronJobList.size(), 0};
        for (V1CronJob c : cronJobList) {
            if (c.getStatus().getActive() != null) {
                cronJobCount[1]++;
            }
        }
        int[] jobCount = {jobList.size(), 0};
        for (V1Job j : jobList) {
            if (j.getStatus().getActive() != null) {
                jobCount[1]++;
            }
        }
        int[] podCount = {podList.size(), 0};
        for (V1Pod p : podList) {
            if (p.getStatus().getPhase().equals("Running")) {
                podCount[1]++;
            }
        }
        return WorkloadOverviewDto.of(deploymentCount, daemonSetCount, replicaSetCount,
            statefulSetCount, cronJobCount, jobCount, podCount);
    }
}
