package com.kcs.zolang.utility;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ClusterApiUtil {
    private final RestTemplate restTemplate;

    public boolean getClusterStatus(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .path("/api/v1/nodes");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                builder.build().encode().toUri(), HttpMethod.GET, entity, Map.class);

        if(response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            Map<String,Object> body = response.getBody();
            return checkNodesStatus(body);
        }
        return false;
    }

    private Boolean checkNodesStatus(Map<String,Object> nodesInfo) {
        if (nodesInfo != null && nodesInfo.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) nodesInfo.get("items");
            for (Map<String, Object> item : items) {
                Map<String, Object> status = (Map<String, Object>) item.get("status");
                Map<String, Object> conditions = ((List<Map<String, Object>>) status.get("conditions")).get(0);
                if (!"True".equals(conditions.get("status"))) {
                    return false; // 하나라도 비정상 상태면 false 반환
                }
            }
        }
        return true;
    }
}
