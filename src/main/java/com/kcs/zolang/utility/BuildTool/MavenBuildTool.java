package com.kcs.zolang.utility;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MavenBuildTool implements BuildTool {
    @Override
    public String setup(String repoDir) {
        return null;
    }

    @Override
    public String build(String repoDir) throws IOException, InterruptedException, ExecutionException {
        return "cd " + repoDir + " && ./mvnw clean package -DskipTests";
    }
}
