package com.kcs.zolang.dto.response;

public record PodContainerDto(
    String name,
    String image,
    String status,
    PodContainerStateDto state) {

}
