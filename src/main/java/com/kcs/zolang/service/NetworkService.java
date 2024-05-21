package com.kcs.zolang.service;

import com.kcs.zolang.dto.response.ServiceDetailDto;
import com.kcs.zolang.dto.response.ServiceListDto;
import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import com.kcs.zolang.utility.MonitoringUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NetworkService {

    private static final Logger log = LoggerFactory.getLogger(NetworkService.class);
    private final MonitoringUtil monitoringUtil;

    public List<ServiceListDto> getServiceList(Long userId, Long clusterId){
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);
        try {
            List<V1Service> serviceList = coreV1Api.listServiceForAllNamespaces().execute().getItems();
            List<ServiceListDto> serviceListDtos = new ArrayList<>();
            for (V1Service service : serviceList) {
                serviceListDtos.add(ServiceListDto.fromEntity(service));
            }
            return serviceListDtos;
        } catch (ApiException e) {
            log.error("Error listing services: {}", e.getResponseBody(), e);
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }


    public List<ServiceDetailDto> getServiceDetail(Long userId, Long clusterId, String serviceName){
        ApiClient client = monitoringUtil.getV1Api(userId, clusterId);
        CoreV1Api coreV1Api = new CoreV1Api(client);
        try {
            List<V1Service> serviceList = coreV1Api.listServiceForAllNamespaces().execute().getItems();
            List<ServiceDetailDto> serviceDetailDtos = new ArrayList<>();
            for (V1Service service : serviceList) {
                if (service.getMetadata().getName().equals(serviceName)) {
                    serviceDetailDtos.add(ServiceDetailDto.fromEntity(service));
                }
            }
            if (serviceDetailDtos.isEmpty()) {
                throw new CommonException(ErrorCode.NOT_FOUND_NETWORK);
            }
            return serviceDetailDtos;
        } catch (ApiException e) {
            throw new CommonException(ErrorCode.API_ERROR);
        }
    }
}
