package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.dto.response.PodListDto;
import com.kcs.zolang.dto.response.UserUrlTokenDto;
import com.kcs.zolang.service.WorkloadService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workloads")
public class WorkloadController {
    private final WorkloadService podService;

    @GetMapping
    public ResponseDto<PodListDto> getOverview(
        @UserId Long userId
    ) {
        return ResponseDto.ok(podService.getWorkload(userId));
    }

    @GetMapping("/pods")
    public ResponseDto<PodListDto> getPods(
        @UserId Long userId
    ) {
        return ResponseDto.ok(podService.getPodList(userId));
    }

    @GetMapping("/pods/original")
    public V1PodList getOriginalPods(
        @UserId Long userId
    ) throws ApiException {
        UserUrlTokenDto userUrlTokenDto=podService.getOriginPodList(userId);
        ApiClient client = Config.fromToken("https://" + userUrlTokenDto.url(),
            userUrlTokenDto.token(), false);
        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        V1PodList podList = api.listPodForAllNamespaces().execute();
        return podList;
    }
}
