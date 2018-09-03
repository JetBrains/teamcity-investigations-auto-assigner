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
import com.intellij.openapi.diagnostic.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.common.Responsibility;
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
    myGson = new GsonBuilder().registerTypeAdapter(Responsibility.class, new ResponsibilitySerializer()).create();
  }

  public void put(STestRun testRun, Responsibility responsibility) {
    try (BufferedWriter writer =
           Files.newBufferedWriter(getAssignerResultFilePath((testRun)), StandardCharsets.UTF_8)) {
      myGson.toJson(responsibility, writer);
    } catch (IOException ex) {
      LOGGER.error(String.format("%s An error occurs during creation of file with results",
                                 Utils.getLogPrefix(testRun)), ex);
      throw new RuntimeException("An error occurs during creation of file with results");
    }
  }

  @Nullable
  public Responsibility get(STestRun testRun) {
    ResponsibilityPair pair;
    try {
      Path resultsFilePath = this.getAssignerResultFilePath(testRun);
      if (!Files.exists(resultsFilePath)) {
        return null;
      }

      try (BufferedReader reader = Files.newBufferedReader(resultsFilePath)) {
        pair = myGson.fromJson(reader, ResponsibilityPair.class);
      }
    } catch (IOException ex) {
      LOGGER.error(String.format("%s An error occurs during reading of file with results",
                                 Utils.getLogPrefix(testRun)), ex);
      throw new RuntimeException("An error occurs during reading of file with results");
    }

    if (pair.investigator == null) {
      throw new RuntimeException("Investigator is not specified!");
    }

    User user = myUserModel.findUserAccount(null, pair.investigator);
    if (user == null) {
      LOGGER.warn(String.format("%s User %s was not found in our model.", Utils.getLogPrefix(testRun),
                                pair.investigator));
    }
    return user != null ? new Responsibility(user, pair.description) : null;
  }

  private Path getAssignerResultFilePath(STestRun testRun) throws IOException {
    Path artifactDirectoryPath = testRun.getBuild().getArtifactsDirectory().toPath();
    Path teamcityDirectoryPath = artifactDirectoryPath.resolve(Constants.TEAMCITY_DIRECTORY);
    if (!Files.exists(teamcityDirectoryPath)) {
      throw new RuntimeException("TeamCity directory does not exist");
    }

    Path autoAssignerDirectoryPath = teamcityDirectoryPath.resolve(Constants.BUILD_FEATURE_TYPE);
    if (!Files.exists(autoAssignerDirectoryPath)) {
      Files.createDirectory(autoAssignerDirectoryPath);
    }

    final String fileName = this.getFileName(testRun);
    return autoAssignerDirectoryPath.resolve(fileName);
  }

  private String getFileName(final STestRun testRun) {
    return String.format("r-%s.json", testRun.getTestRunId());
  }
}

