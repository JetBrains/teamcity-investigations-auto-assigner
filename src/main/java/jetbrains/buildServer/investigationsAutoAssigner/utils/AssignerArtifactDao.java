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

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssignerArtifactDao {
  private final Gson myGson;
  private UserModelEx myUserModel;
  private static final Logger LOGGER = Logger.getInstance(AssignerArtifactDao.class.getName());

  public AssignerArtifactDao(@NotNull final UserModelEx userModel) {
    myUserModel = userModel;
    myGson = new Gson();
  }

  public void appendHeuristicsResult(@NotNull SBuild build,
                                     @NotNull List<STestRun> testRuns,
                                     @NotNull HeuristicResult heuristicResult) {
    try {
      List<ResponsibilityPersistentInfo> infoToAdd = new ArrayList<>(getPersistentInfoList(testRuns, heuristicResult));
      if (infoToAdd.isEmpty()) return;

      Path resultsFilePath = this.getAssignerResultFilePath(build);
      List<ResponsibilityPersistentInfo> previouslyAdded = readPreviouslyAdded(resultsFilePath);
      infoToAdd.addAll(previouslyAdded);
      LOGGER.debug(String.format("Build %s :: Read %s previously added investigations",
                                 build.getBuildId(), previouslyAdded.size()));

      try (BufferedWriter writer =
             Files.newBufferedWriter(resultsFilePath, StandardCharsets.UTF_8)) {
        myGson.toJson(infoToAdd, writer);
        LOGGER.debug(String.format("Build %s :: Wrote %s new found investigations",
                                   build.getBuildId(), infoToAdd.size() - previouslyAdded.size()));
      }
    } catch (IOException ex) {
      LOGGER.error(String.format("Build %s :: An error occurs during appending results", build.getBuildId()), ex);
      throw new RuntimeException("An error occurs during appending results");
    }
  }

  @NotNull
  private List<ResponsibilityPersistentInfo> getPersistentInfoList(@NotNull final List<STestRun> testRuns,
                                                                   @NotNull final HeuristicResult heuristicResult) {
    List<ResponsibilityPersistentInfo> result = new ArrayList<>();
    for (STestRun testRun : testRuns) {
      Responsibility responsibility = heuristicResult.getResponsibility(testRun);
      if (responsibility != null) {
        result.add(new ResponsibilityPersistentInfo(String.valueOf(testRun.getTest().getTestNameId()),
                                                    String.valueOf(responsibility.getUser().getId()),
                                                    responsibility.getDescription()));
      }
    }
    return result;
  }

  @NotNull
  private List<ResponsibilityPersistentInfo> readPreviouslyAdded(Path resultsFilePath) throws IOException {

    if (Files.exists(resultsFilePath) && Files.size(resultsFilePath) != 0) {
      try (BufferedReader reader = Files.newBufferedReader(resultsFilePath)) {
        return Arrays.asList(myGson.fromJson(reader, ResponsibilityPersistentInfo[].class));
      }
    }

    return Collections.emptyList();
  }

  @NotNull
  private Path getAssignerResultFilePath(@NotNull final SBuild build) throws IOException {
    Path resultPath = getAssignerResultFilePath(build, true);
    if (resultPath == null) {
      throw new IllegalStateException("The path for artifact supposed to be created");
    }

    return resultPath;
  }

  @Nullable
  private Path getAssignerResultFilePathIfExist(@NotNull final SBuild build) throws IOException {
    return getAssignerResultFilePath(build, false);
  }

  @Nullable
  private Path getAssignerResultFilePath(@NotNull final SBuild build, boolean createIfNotExist) throws IOException {
    Path artifactDirectoryPath = build.getArtifactsDirectory().toPath();
    Path teamcityDirectoryPath = artifactDirectoryPath.resolve(Constants.TEAMCITY_DIRECTORY);
    if (!Files.exists(teamcityDirectoryPath)) {
      throw new RuntimeException("TeamCity directory does not exist");
    }

    Path autoAssignerDirectoryPath = teamcityDirectoryPath.resolve(Constants.BUILD_FEATURE_TYPE);
    if (!Files.exists(autoAssignerDirectoryPath)) {
      if (createIfNotExist) {
        Files.createDirectory(autoAssignerDirectoryPath);
      } else {
        return null;
      }
    }

    Path resultsPath = autoAssignerDirectoryPath.resolve("results.json");
    if (!Files.exists(resultsPath)) {
      if (createIfNotExist) {
        Files.createFile(resultsPath);
      } else {
        return null;
      }
    }

    return resultsPath;
  }

  @Nullable
  public Responsibility get(@Nullable SBuild firstFailedBuild, @NotNull STestRun testRun) {
    ResponsibilityPersistentInfo[] persistentBuildInfo;
    try {
      Path resultsFilePath = firstFailedBuild != null ?
                             this.getAssignerResultFilePathIfExist(firstFailedBuild) :
                             this.getAssignerResultFilePathIfExist(testRun.getBuild());
      if (resultsFilePath == null) {
        return null;
      }

      try (BufferedReader reader = Files.newBufferedReader(resultsFilePath)) {
        persistentBuildInfo = myGson.fromJson(reader, ResponsibilityPersistentInfo[].class);
        LOGGER.debug(String.format("%s Read %s stored investigations",
                                   Utils.getLogPrefix(testRun), persistentBuildInfo.length));
      }
    } catch (IOException ex) {
      LOGGER.error(String.format("%s An error occurs during reading of file with results",
                                 Utils.getLogPrefix(testRun)), ex);
      throw new RuntimeException("An error occurs during reading of file with results");
    }

    for (ResponsibilityPersistentInfo persistentInfo : persistentBuildInfo) {
      if (persistentInfo.testNameId.equals(String.valueOf(testRun.getTest().getTestNameId()))) {
        LOGGER.debug(String.format("%s Investigation for testRun %s was found",
                                   Utils.getLogPrefix(testRun), testRun.getTestRunId()));
        User user = myUserModel.findUserById(Long.parseLong(persistentInfo.investigatorId));
        if (user == null) {
          LOGGER.warn(String.format("%s User with id %s was not found in our model.", Utils.getLogPrefix(testRun),
                                    persistentInfo.investigatorId));
        }
        return user != null ? new Responsibility(user, persistentInfo.description) : null;
      }
    }

    LOGGER.debug(String.format("%s Investigation for testRun %s wasn't found",
                               Utils.getLogPrefix(testRun), testRun.getTestRunId()));
    return null;
  }
}

