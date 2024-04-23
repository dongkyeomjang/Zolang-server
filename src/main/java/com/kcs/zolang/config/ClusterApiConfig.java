package com.kcs.zolang.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ClusterApiConfig {
    @Bean
    public RestTemplate clusterApiRestTemplate() {
        return new RestTemplate();
    }
}
