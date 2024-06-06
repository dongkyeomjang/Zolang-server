package com.kcs.zolang.utility.BuildTool;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class NpmBuildTool implements BuildTool {
    @Override
    public String setup(String repoDir) {
        return null;
    }

    @Override
    public String build(String repoDir) throws IOException, InterruptedException, ExecutionException {
        return "cd " + repoDir + " && npm install && npm run build";
    }
}
