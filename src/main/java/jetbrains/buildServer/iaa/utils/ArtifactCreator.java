/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.iaa.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.common.FailedBuildInfo;
import jetbrains.buildServer.iaa.common.HeuristicResult;
import jetbrains.buildServer.iaa.common.Responsibility;

public class ArtifactCreator {

  private final static String TEAMCITY_DIRECTORY = ".teamcity";
  public void create(final FailedBuildInfo failedBuildInfo) {
    HeuristicResult heuristicResult = failedBuildInfo.getHeuristicsResult();
    List<Responsibility> allResponsibilities = heuristicResult.getAllResponsibilities();
    File artifactDirectory = failedBuildInfo.getBuild().getArtifactsDirectory();
    File teamcityDirectory = new File(artifactDirectory, TEAMCITY_DIRECTORY);
    if (!teamcityDirectory.exists()) {
      throw new RuntimeException("TeamCity directory does not exist");
    }
    File autoAssignerDirectory = new File(teamcityDirectory, Constants.BUILD_FEATURE_TYPE);
    boolean creationResult = autoAssignerDirectory.mkdir();
    if (!creationResult) {
      throw new RuntimeException("Creation of auto-assigner folder is failed");
    }

    final String fileName = "responsibilities.json";
    File resultsFile = new File(autoAssignerDirectory, fileName);

    Gson gson = new GsonBuilder().registerTypeAdapter(Responsibility.class, new ResponsibilitySerializer()).create();
    try {
      Files.write(resultsFile.toPath(), gson.toJson(allResponsibilities).getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      throw new RuntimeException("An error occurs during creation of file with results");
    }
  }
}

