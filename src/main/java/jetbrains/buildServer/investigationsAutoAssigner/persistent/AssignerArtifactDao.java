/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.utils.Utils;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.STestRun;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserModelEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.investigationsAutoAssigner.common.Constants.SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION;

public class AssignerArtifactDao {
  private static final Logger LOGGER = Constants.LOGGER;
  private UserModelEx myUserModel;
  private SuggestionsDao mySuggestionsDao;
  private AssignerResultsFilePath myAssignerResultsFilePath;
  private StatisticsReporter myStatisticsReporter;

  public AssignerArtifactDao(@NotNull final UserModelEx userModel,
                             @NotNull final SuggestionsDao suggestionsDao,
                             @NotNull final AssignerResultsFilePath assignerResultsFilePath,
                             @NotNull final StatisticsReporter statisticsReporter) {
    myUserModel = userModel;
    mySuggestionsDao = suggestionsDao;
    myAssignerResultsFilePath = assignerResultsFilePath;
    myStatisticsReporter = statisticsReporter;
  }

  public void appendHeuristicsResult(@NotNull SBuild build,
                                     @NotNull List<STestRun> testRuns,
                                     @NotNull HeuristicResult heuristicResult) {
    doAppend(build, getPersistentInfoList(testRuns, heuristicResult));
  }

  private void doAppend(@NotNull final SBuild build,
                        @NotNull List<ResponsibilityPersistentInfo> infoToAdd) {
    if (infoToAdd.isEmpty()) return;

    try {
      myStatisticsReporter.reportSavedSuggestions(infoToAdd.size());
      Path resultsFilePath = myAssignerResultsFilePath.get(build);

      List<ResponsibilityPersistentInfo> previouslyAdded = mySuggestionsDao.read(resultsFilePath);

      if (previouslyAdded.isEmpty()) {
        //should be called only once per build
        myStatisticsReporter.reportBuildWithSuggestions();
      }

      infoToAdd.addAll(previouslyAdded);
      LOGGER.debug(String.format("Build id:%s :: Read %s previously added investigations",
                                 build.getBuildId(), previouslyAdded.size()));

      mySuggestionsDao.write(resultsFilePath, infoToAdd);
      LOGGER.debug(String.format("Build id:%s :: Wrote %s new found investigations",
                                 build.getBuildId(), infoToAdd.size() - previouslyAdded.size()));
    } catch (IOException ex) {
      LOGGER.warn(String.format("Build id:%s :: An error occurs during appending results", build.getBuildId()), ex);
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

  @Nullable
  public Responsibility get(@Nullable SBuild firstFailedBuild, @NotNull STestRun testRun) {
    List<ResponsibilityPersistentInfo> suggestions;
    try {
      Path resultsFilePath = firstFailedBuild != null ?
                             myAssignerResultsFilePath.getIfExist(firstFailedBuild, testRun) :
                             myAssignerResultsFilePath.getIfExist(testRun.getBuild(), testRun);

      suggestions = mySuggestionsDao.read(resultsFilePath);
    } catch (IOException ex) {
      LOGGER.warn(String.format("%s An error occurs during reading of file with results",
                                Utils.getLogPrefix(testRun)), ex);
      return null;
    }

    for (ResponsibilityPersistentInfo persistentInfo : suggestions) {
      if (persistentInfo.testNameId.equals(String.valueOf(testRun.getTest().getTestNameId()))) {
        if (persistentInfo.investigatorId.equals(Constants.ASSIGNEE_FILTERED_LITERAL)) {

          return TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION) ?
                 new Responsibility(myUserModel.getGuestUser(),
                                    Constants.ASSIGNEE_FILTERED_DESCRIPTION_PREFIX + persistentInfo.reason) :
                 null;
        }
        LOGGER.debug(String.format("%s Investigation for testRun %s was found",
                                   Utils.getLogPrefix(testRun), testRun.getTestRunId()));
        User user = myUserModel.findUserById(Long.parseLong(persistentInfo.investigatorId));
        if (user == null) {
          LOGGER.warn(String.format("%s User with id '%s' was not found in user model.", Utils.getLogPrefix(testRun),
                                    persistentInfo.investigatorId));
        }
        return user != null ? new Responsibility(user, persistentInfo.reason) : null;
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("%s Investigation for testRun '%s' wasn't found",
                                 Utils.getLogPrefix(testRun), testRun.getTestRunId()));
    }
    return null;
  }

  public void appendNotApplicableTestsDescription(@NotNull final SBuild build,
                                                  @NotNull final Map<Long, String> notApplicableTestsDescription) {
    doAppend(build, getPersistentInfoList(notApplicableTestsDescription));
  }


  @NotNull
  private List<ResponsibilityPersistentInfo> getPersistentInfoList(final Map<Long, String> notApplicableTestsDescription) {
    List<ResponsibilityPersistentInfo> result = new ArrayList<>();
    for (final Map.Entry<Long, String> longStringEntry : notApplicableTestsDescription.entrySet()) {
      result.add(new ResponsibilityPersistentInfo(longStringEntry.getKey().toString(),
                                                  Constants.ASSIGNEE_FILTERED_LITERAL,
                                                  longStringEntry.getValue()));
    }

    return result;
  }
}

