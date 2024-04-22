package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.PodListDto;
import com.kcs.zolang.dto.response.UserUrlTokenDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final ClusterRepository clusterRepository;

    public PodListDto getWorkload(Long userId) {
        try {
            UserUrlTokenDto userUrlTokenDto = UserUrlTokenDto.fromEntity(
                clusterRepository.findByUserId(userId));
            ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
                userUrlTokenDto.token(), false);
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            PodListDto podListDto = PodListDto.of(api.listPodForAllNamespaces().execute());
            api.listNode();
            return PodListDto.of(api.listPodForAllNamespaces().execute());
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public PodListDto getPodList(Long userId) {
        try {
            UserUrlTokenDto userUrlTokenDto = UserUrlTokenDto.fromEntity(
                clusterRepository.findByUserId(userId));
            ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
                userUrlTokenDto.token(), false);
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            return PodListDto.of(api.listPodForAllNamespaces().execute());
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public UserUrlTokenDto getOriginPodList(Long userId) throws ApiException {
        return UserUrlTokenDto.fromEntity(
                clusterRepository.findByUserId(userId));
    }
}
