package com.kcs.zolang.security.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {
    @Value("${encryption.key}")
    private String key;
    @Value("${encryption.algorithm}")
    private String algorithm;
    @Value("${encryption.key-obtention-iterations}")
    private String keyObtentionIterations;
    @Value("${encryption.pool-size}")
    private String poolSize;
    @Value("${encryption.provider-name}")
    private String providerName;
    @Value("${encryption.salt-generator-class-name}")
    private String saltGeneratorClassName;
    @Value("${encryption.string-output-type}")
    private String stringOutputType;
    @Bean
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(key);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations(keyObtentionIterations);
        config.setPoolSize(poolSize);
        config.setProviderName(providerName);
        config.setSaltGeneratorClassName(saltGeneratorClassName);
        config.setStringOutputType(stringOutputType);
        encryptor.setConfig(config);
        return encryptor;
    }
}

