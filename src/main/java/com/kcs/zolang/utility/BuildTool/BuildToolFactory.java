package com.kcs.zolang.utility.BuildTool;

import java.io.File;
import java.io.IOException;

public class BuildToolFactory {
    public static BuildTool detectBuildTool(String repoDir, String buildTool) throws IOException {
        switch (buildTool) {
            case "gradle":
                return new GradleBuildTool();
            case "maven":
                return new MavenBuildTool();
            case "python":
                return new PythonBuildTool();
            case "nodejs":
                return new NodeJsBuildTool();
            case "AUTO":
                if (new File(repoDir + "/gradlew").exists()) {
                    return new GradleBuildTool();
                } else if (new File(repoDir + "/mvnw").exists()) {
                    return new MavenBuildTool();
                } else if (new File(repoDir + "/requirements.txt").exists()) {
                    return new PythonBuildTool();
                } else if (new File(repoDir + "/package.json").exists()) {
                    return new NodeJsBuildTool();
                } else {
                    throw new RuntimeException("No supported build tool found in the repository");
                }
            default:
                throw new RuntimeException("Unsupported build tool: " + buildTool);
        }

    }
}
