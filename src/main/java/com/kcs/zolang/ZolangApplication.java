package com.kcs.zolang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ZolangApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZolangApplication.class, args);
    }

}
