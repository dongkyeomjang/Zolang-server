package com.kcs.zolang.utility.BuildTool;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class YarnBuildTool implements BuildTool {
    @Override
    public String setup(String repoDir) {
        return null;
    }

    @Override
    public String build(String repoDir) throws IOException, InterruptedException, ExecutionException {
        return "cd " + repoDir + " && yarn install && yarn build";
    }
}
