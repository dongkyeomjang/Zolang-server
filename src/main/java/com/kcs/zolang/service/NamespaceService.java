package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.NamespaceCategoryDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NamespaceService {
    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);
    private final MonitoringUtil monitoringUtil;

    public NamespaceService(MonitoringUtil monitoringUtil) {
        this.monitoringUtil = monitoringUtil;
    }

    public List<NamespaceCategoryDto> getNamespaceCategory(Long userId, Long clusterId) {
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);
        try {
            V1NamespaceList namespaceList = coreV1Api.listNamespace().execute();
            return namespaceList.getItems().stream()
                    .map(NamespaceCategoryDto::fromEntity)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }
}
