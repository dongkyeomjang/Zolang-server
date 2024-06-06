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
            case "none":
                return new PythonBuildTool();
            case "npm":
                return new NpmBuildTool();
            case "yarn":
                return new YarnBuildTool();
            case "AUTO":
                if (new File(repoDir + "/gradlew").exists()) {
                    return new GradleBuildTool();
                } else if (new File(repoDir + "/mvnw").exists()) {
                    return new MavenBuildTool();
                } else if (new File(repoDir + "/requirements.txt").exists()) {
                    return new PythonBuildTool();
                } else if (new File(repoDir + "/package.json").exists()) {
                    if (new File(repoDir + "/yarn.lock").exists()) {
                        return new YarnBuildTool();
                    } else {
                        return new NpmBuildTool();
                    }
                } else {
                    throw new RuntimeException("No supported build tool found in the repository");
                }
            default:
                throw new RuntimeException("Unsupported build tool: " + buildTool);
        }

    }
}
