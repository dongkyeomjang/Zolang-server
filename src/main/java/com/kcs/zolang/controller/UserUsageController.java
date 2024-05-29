package com.kcs.zolang.controller;

import com.kcs.zolang.annotation.UserId;
import com.kcs.zolang.dto.global.ResponseDto;
import com.kcs.zolang.service.UserUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Validated
@Tag(name="User Usage API", description="사용자 사용량 API")
public class UserUsageController {
    private final UserUsageService userUsageService;

    @GetMapping("/dashboard/usage")
    @Operation(summary = "사용자 클러스터 사용량 조회", description = "사용자의 모든 클러스터 사용량 조회")
    public ResponseDto<?> getUserUsage(
            @UserId Long userId
    ) throws Exception {
        return ResponseDto.ok(userUsageService.getUserUsage(userId));
    }

//    @GetMapping("/dashboard/usage/period")
//    @Operation(summary = "사용자 클러스터 사용량 조회", description = "사용자의 모든 클러스터 사용량 조회")
//    public ResponseDto<?> getUserUsagePeriod(
//            @UserId Long userId,
//            @RequestParam(name="period") String period
//
//    ) throws Exception {
//        return ResponseDto.ok(userUsageService.getUserUsagePeriod(userId,period));
//    }
//}

}

