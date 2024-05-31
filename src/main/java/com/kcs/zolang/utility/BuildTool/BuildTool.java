package com.kcs.zolang.utility;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

 public interface BuildTool {
    String setup(String repoDir) throws IOException, InterruptedException, ExecutionException;
    String build(String repoDir) throws IOException, InterruptedException, ExecutionException;
}
