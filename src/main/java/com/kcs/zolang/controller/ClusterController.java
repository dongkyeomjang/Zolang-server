package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.ClusterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Tag(name = "Monitoring", description = "모니터링 관련 API")
public class ClusterController {
    private final ClusterService clusterService;

    public ResponseDto<?> getClusterList(
            @UserId Long userId
    ) {
        return ResponseDto.ok(clusterService.getClusterList(userId));
    }


}
