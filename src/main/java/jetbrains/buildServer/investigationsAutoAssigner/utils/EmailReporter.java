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

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.util.EmailException;
import jetbrains.buildServer.util.EmailSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmailReporter {

  private static final Logger LOGGER = Logger.getInstance(EmailReporter.class.getName());
  @NotNull private final EmailSender myEmailSender;
  @Nullable private final String mySupervisorEmail;
  @NotNull private final WebLinks myWebLinks;

  public EmailReporter(@NotNull EmailSender emailSender,
                       @NotNull WebLinks webLinks,
                       @NotNull CustomParameters customParameters) {
    myEmailSender = emailSender;
    myWebLinks = webLinks;
    mySupervisorEmail = customParameters.getEmailForEmailReporter();
  }

  public void sendResults(SBuild sBuild, HeuristicResult heuristicsResult) {
    if (mySupervisorEmail != null && !heuristicsResult.isEmpty()) {
      String title = String.format("Investigation auto-assigner report for build id:%s", sBuild.getBuildId());
      trySendEmail(mySupervisorEmail, title, generateHtmlReport(sBuild, heuristicsResult));
    }
  }

  private void trySendEmail(@NotNull String to, String title, String html) {
    try {
      myEmailSender.send(to, title, "", html);
    } catch (EmailException ex) {
      LOGGER.error(ex);
    }
  }

  @NotNull
  private String generateHtmlReport(final SBuild sBuild, final HeuristicResult heuristicsResult) {
    String buildRunResultsUrl = myWebLinks.getViewResultsUrl(sBuild);


    return String.format("<!DOCTYPE html>\n" +
                         "<html>\n" +
                         "<body>\n" +
                         "<h2>Report for <a href=\"%s\">%s#%s</a>. Found %s investigations:</h2>\n" +
                         "<ol>\n%s%s</ol>\n" +
                         "</body>\n" +
                         "</html>",
                         buildRunResultsUrl,
                         sBuild.getBuildTypeName(),
                         sBuild.getBuildId(),
                         heuristicsResult.getAllResponsibilities().size(),
                         generateForFailedTests(sBuild, heuristicsResult),
                         generateForBuildProblems(sBuild, heuristicsResult));
  }

  private String generateForFailedTests(SBuild sBuild, HeuristicResult heuristicsResult) {
    StringBuilder htmlBuilder = new StringBuilder();
    String buildRunResultsUrl = myWebLinks.getViewResultsUrl(sBuild);

    List<STestRun> testRuns = sBuild.getBuildStatistics(new BuildStatisticsOptions()).getFailedTests();

    for (STestRun testRun : testRuns) {
      Responsibility responsibility = heuristicsResult.getResponsibility(testRun);
      if (responsibility == null) {
        continue;
      }

      htmlBuilder.append(String.format("<li><a href=\"%s\">Investigation</a> was assigned to %s who %s.</li>\n",
                                       buildRunResultsUrl + "#testNameId" + testRun.getTest().getTestNameId(),
                                       responsibility.getUser().getUsername(),
                                       responsibility.getDescription()));
    }

    return htmlBuilder.toString();
  }

  private String generateForBuildProblems(final SBuild sBuild, final HeuristicResult heuristicsResult) {
    StringBuilder htmlBuilder = new StringBuilder();
    List<BuildProblem> allBuildProblems = ((BuildEx)sBuild).getBuildProblems();
    for (BuildProblem buildProblem : allBuildProblems) {
      Responsibility responsibility = heuristicsResult.getResponsibility(buildProblem);
      if (responsibility == null) {
        continue;
      }
      htmlBuilder.append(String.format("<li>Investigation for build problem was assigned to %s who %s.</li>\n",
                                       responsibility.getUser().getUsername(),
                                       responsibility.getDescription()));
    }

    return htmlBuilder.toString();
  }

}
