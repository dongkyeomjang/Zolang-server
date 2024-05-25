package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.NamespaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/namespace")
@Validated
@Tag(name="Monitoring-namespace Category API", description="네임스페이스 카테고리 API")
public class NamespaceController {
    private final NamespaceService namespaceService;

    @GetMapping("/{cluster_id}")
    @Operation(summary="Namespace-category 조회", description = "특정 클러스터안의 네임스페이스 카테고리 출력")
    public ResponseDto<?> getNamespaceCategory(
            @UserId Long userId,
            @PathVariable(name="cluster_id") Long clusterId
    ){
        return ResponseDto.ok(namespaceService.getNamespaceCategory(userId, clusterId));
    }
}
