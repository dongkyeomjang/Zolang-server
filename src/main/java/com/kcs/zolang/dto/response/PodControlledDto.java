package com.kcs.zolang.dto.response;

public record PodControlledDto(
    String name,
    String kind,
    int replicas,
    int readyReplicas,
    String age,
    List<Map<String, String>> label,
    List<Map<String, String>> image
) {

    public static PodControlledDto fromEntity(V1OwnerReference ownerReference) {
        return PodControlledDto.builder()
            .name(ownerReference.getName())
            .kind(ownerReference.getKind())
            .replicas(ownerReference)
            .readyReplicas(ownerReference)
            .age(ownerReference.getAge())
            .label(ownerReference.getLabel())
            .image(ownerReference.getImage())
            .build();
    }
}
