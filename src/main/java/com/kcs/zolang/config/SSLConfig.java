package com.kcs.zolang.config;

import com.kcs.zolang.exception.CommonException;
import com.kcs.zolang.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
@Component
public class SSLConfig {

    public String storeCertificate(InputStream certificateStream, String filename, Long userId, String clusterName) throws Exception {
        try{
            Path certPath = Path.of("/Users/kyeom/Development/resources/cert-zolang/", userId.toString()+clusterName+filename);
            Files.copy(certificateStream, certPath, StandardCopyOption.REPLACE_EXISTING);
            return certPath.toString();
        } catch (IOException e) {
            throw new CommonException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }
}
