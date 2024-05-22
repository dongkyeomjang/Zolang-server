package com.kcs.zolang.dto.response;

public record PodContainerStateDto(
    boolean waiting,
    boolean running,
    String startDate
) {

}
