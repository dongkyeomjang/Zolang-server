package com.kcs.zolang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.eks.EksClient;
import software.amazon.awssdk.services.sts.StsClient;

@Configuration
public class AWSConfig {
    @Value("${aws.region}")
    private String region;

    @Bean
    public EksClient eksClient() {
        return EksClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public StsClient stsClient() {
        return StsClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public Ec2Client ec2Client() {
        return Ec2Client.builder()
                .region(Region.of(region))
                .build();
    }

}
