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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssignerArtifactDao {
  private UserModelEx myUserModel;
  private static final Logger LOGGER = Logger.getInstance(AssignerArtifactDao.class.getName());

  public AssignerArtifactDao(@NotNull final UserModelEx userModel) {
    myUserModel = userModel;
  }

  public void put(STestRun testRun, Responsibility responsibility) {
    File resultsFile = this.getAssignerResultFile(testRun);

    Gson gson = new GsonBuilder().registerTypeAdapter(Responsibility.class, new ResponsibilitySerializer()).create();
    try {
      Files.write(resultsFile.toPath(), gson.toJson(responsibility).getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      throw new RuntimeException("An error occurs during creation of file with results");
    }
  }

  @Nullable
  public Responsibility get(STestRun testRun) {
    File resultsFile = this.getAssignerResultFile(testRun);
    if (!resultsFile.exists()) {
      return null;
    }

    Gson gson = new GsonBuilder().registerTypeAdapter(Responsibility.class, new ResponsibilitySerializer()).create();
    Responsibility result;
    try {
      List<String> responsibilityJson = Files.readAllLines(resultsFile.toPath(), StandardCharsets.UTF_8);
      ResponsibilityPair pair = gson.fromJson(String.join("\n", responsibilityJson), ResponsibilityPair.class);
      if (pair.investigator == null) {
        throw new RuntimeException("Investigator is not specified!");
      }
      User user = myUserModel.findUserAccount(null, pair.investigator);
      if (user != null) {
        result = new Responsibility(user, pair.description);
      } else {
        LOGGER.warn(String.format("User %s was not found in our model.", pair.investigator));
      }
    } catch (IOException ex) {
      throw new RuntimeException("An error occurs during creation of file with results");
    }
    return result;
  }

  private File getAssignerResultFile(STestRun testRun) {
    File artifactDirectory = testRun.getBuild().getArtifactsDirectory();
    File teamcityDirectory = new File(artifactDirectory, Constants.TEAMCITY_DIRECTORY);
    if (!teamcityDirectory.exists()) {
      throw new RuntimeException("TeamCity directory does not exist");
    }

    File autoAssignerDirectory = new File(teamcityDirectory, Constants.BUILD_FEATURE_TYPE);
    if (!autoAssignerDirectory.exists()) {
      boolean creationResult = autoAssignerDirectory.mkdir();
      if (!creationResult) {
        throw new RuntimeException("Creation of auto-assigner folder is failed");
      }
    }

    final String fileName = this.getFileName(testRun);
    return new File(autoAssignerDirectory, fileName);
  }

  private String getFileName(final STestRun testRun) {
    return String.format("r-%s.json", testRun.getTestRunId());
  }
}

