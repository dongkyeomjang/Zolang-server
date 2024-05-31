package com.kcs.zolang.utility;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class PythonBuildTool implements BuildTool {
    @Override
    public String setup(String repoDir) throws IOException, InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public String build(String repoDir) throws IOException, InterruptedException, ExecutionException {
        return "cd " + repoDir + " && pip install -r requirements.txt";
    }
}
