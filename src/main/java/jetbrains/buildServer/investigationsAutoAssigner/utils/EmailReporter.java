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
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
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
  private StatisticsReporter myStatisticsReporter;
  @NotNull private final WebLinks myWebLinks;

  public EmailReporter(@NotNull EmailSender emailSender,
                       @NotNull WebLinks webLinks,
                       @NotNull CustomParameters customParameters,
                       @NotNull StatisticsReporter statisticsReporter) {
    myEmailSender = emailSender;
    myWebLinks = webLinks;
    mySupervisorEmail = customParameters.getEmailForEmailReporter();
    myStatisticsReporter = statisticsReporter;
  }

  public void sendResults(FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();
    HeuristicResult heuristicsResult = failedBuildInfo.getHeuristicsResult();

    if (mySupervisorEmail != null && !heuristicsResult.isEmpty()) {
      trySendEmail(mySupervisorEmail, getTitle(failedBuildInfo), generateHtmlReport(sBuild, heuristicsResult));
    }
  }

  private String getTitle(final FailedBuildInfo failedBuildInfo) {
    SBuild sBuild = failedBuildInfo.getBuild();

    StringBuilder sb = new StringBuilder();
    sb.append("[IAA health report. ");
    if (failedBuildInfo.shouldDelayAssignments()) {
      sb.append("Delayed assignment");
    } else if (CustomParameters.isBuildFeatureEnabled(sBuild)) {
      sb.append("New assignments");
    } else {
      sb.append("New suggestions");
    }
    sb.append("]");

    @Nullable
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType != null) {
      sb.append(" Project '").append(sBuildType.getProject().describe(false)).append("'");
    }

    return sb.toString();
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

    String projectUrl = sBuild.getProjectId();
    String projectName = sBuild.getProjectId();
    SBuildType sBuildType = sBuild.getBuildType();
    if (sBuildType != null) {
      projectUrl = myWebLinks.getProjectPageUrl(sBuildType.getProject().getExternalId());
      projectName = sBuildType.getProject().getName();
    }

    return String.format("<!DOCTYPE html>\n" +
                         "<html>\n" +
                         "<body>\n" +
                         "<h3>Report for <a href=\"%s\">%s#%s</a></h3>\n" +
                         "<p>Found %s entries for project <a href=\"%s\">%s</a>:</p>\n" +
                         "<ol>\n%s%s</ol>\n" +
                         "%s\n" +
                         "</body>\n" +
                         "</html>",
                         buildRunResultsUrl,
                         sBuild.getBuildTypeName(),
                         sBuild.getBuildId(),
                         heuristicsResult.getAllResponsibilities().size(),
                         projectUrl,
                         projectName,
                         generateForFailedTests(sBuild, heuristicsResult),
                         generateForBuildProblems(sBuild, heuristicsResult),
                         generateStatisticsReport());
  }

  private String generateStatisticsReport() {
    return myStatisticsReporter.generateReport().replaceAll("\n", "<br>");
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

      htmlBuilder.append(String.format("<li><a href=\"%s\">Test entry</a> for %s. The user %s.</li>\n",
                                       buildRunResultsUrl + "#testNameId" + testRun.getTest().getTestNameId(),
                                       responsibility.getUser().getDescriptiveName(),
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

      htmlBuilder.append(String.format("Build problem entry for %s. The user %s.</li>\n",
                                       responsibility.getUser().getDescriptiveName(),
                                       responsibility.getDescription()));
    }

    return htmlBuilder.toString();
  }

}
