package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.PodDto;
import com.kcs.zolang.dto.response.UserUrlTokenDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.repository.ClusterRepository;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final ClusterRepository clusterRepository;

    public void getWorkload(Long userId) {
            UserUrlTokenDto userUrlTokenDto = UserUrlTokenDto.fromEntity(
                clusterRepository.findByUserId(userId).get(0));
            ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
                userUrlTokenDto.token(), false);
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            api.listNode();
            //return podOverviewDto;
    }

    public List<PodDto> getPodList(Long userId) {
        try {
            UserUrlTokenDto userUrlTokenDto = clusterRepository.findByUserId(userId) != null? UserUrlTokenDto.fromEntity(clusterRepository.findByUserId(userId).get(0)):null;
            if(userUrlTokenDto == null){
                return null;
            }
            ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
                userUrlTokenDto.token(), false);
            Configuration.setDefaultApiClient(client);
            CoreV1Api api = new CoreV1Api();
            return api.listPodForAllNamespaces().execute().getItems().stream().map(PodDto::fromEntity).toList();
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }

    public UserUrlTokenDto getOriginPodList(Long userId){
        return UserUrlTokenDto.fromEntity(clusterRepository.findByUserId(userId).get(0));
    }
}
