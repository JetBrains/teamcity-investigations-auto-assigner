/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner;

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.FailedBuildInfo;
import jetbrains.buildServer.investigationsAutoAssigner.processing.DelayedAssignmentsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.processing.FailedTestAndBuildProblemsProcessor;
import jetbrains.buildServer.investigationsAutoAssigner.utils.CustomParameters;
import jetbrains.buildServer.investigationsAutoAssigner.utils.EmailReporter;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.util.NamedThreadFactory;
import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FailedTestAndBuildProblemsDispatcher {

  private static final Logger LOGGER = Logger.getInstance(FailedTestAndBuildProblemsDispatcher.class.getName());

  @NotNull
  private final FailedTestAndBuildProblemsProcessor myProcessor;
  private final DelayedAssignmentsProcessor myDelayedAssignmentsProcessor;
  @NotNull private final EmailReporter myEmailReporter;
  @NotNull
  private final ConcurrentHashMap<Long, FailedBuildInfo> myFailedBuilds;
  @NotNull
  private final ConcurrentHashMap<String, FailedBuildInfo> myDelayedAssignments;
  @NotNull
  private final ScheduledExecutorService myDaemon;

  public FailedTestAndBuildProblemsDispatcher(@NotNull final BuildServerListenerEventDispatcher buildServerListenerEventDispatcher,
                                              @NotNull final FailedTestAndBuildProblemsProcessor processor,
                                              @NotNull final DelayedAssignmentsProcessor delayedAssignmentsProcessor,
                                              @NotNull final EmailReporter emailReporter) {
    myProcessor = processor;
    myDelayedAssignmentsProcessor = delayedAssignmentsProcessor;
    myEmailReporter = emailReporter;
    myFailedBuilds = new ConcurrentHashMap<>();
    myDelayedAssignments = new ConcurrentHashMap<>();
    myDaemon = ExecutorsFactory.newFixedScheduledDaemonExecutor(Constants.BUILD_FEATURE_TYPE, 1);
    myDaemon.scheduleWithFixedDelay(this::processBrokenBuildsOneThread,
            CustomParameters.getProcessingDelayInSeconds(),
            CustomParameters.getProcessingDelayInSeconds(),
            TimeUnit.SECONDS);
    FailedTestAndBuildProblemsDispatcher instance = this;
    buildServerListenerEventDispatcher.addListener(new BuildServerAdapter() {
      @Override
      public void buildProblemsChanged(@NotNull SBuild sBuild,
                                       @NotNull List<BuildProblemData> before,
                                       @NotNull List<BuildProblemData> after) {
        if (myFailedBuilds.containsKey(sBuild.getBuildId()) || shouldIgnore(sBuild) || !(sBuild instanceof BuildEx)) {
          return;
        }

        myFailedBuilds.put(sBuild.getBuildId(), new FailedBuildInfo(sBuild));
      }

      @Override
      public void buildFinished(@NotNull SRunningBuild build) {
        if (shouldIgnore(build)) {
          return;
        }
        processDelayedAssignmentsForPrevious(build);

        @Nullable
        FailedBuildInfo failedBuildInfo = myFailedBuilds.remove(build.getBuildId());
        if (failedBuildInfo != null) {
          myDaemon.execute(() -> instance.processFinishedBuild(failedBuildInfo));
          LOGGER.debug("Build #" + build.getBuildId() + " removed from processing.");
        }
      }

      private void processDelayedAssignmentsForPrevious(SBuild nextBuild) {
        @Nullable
        SBuildType sBuildType = nextBuild.getBuildType();
        if (sBuildType != null) {
          @Nullable
          FailedBuildInfo delayedAssignmentsBuildInfo = myDelayedAssignments.remove(sBuildType.getInternalId());
          if (delayedAssignmentsBuildInfo != null) {
            myDaemon.execute(() -> instance.processDelayedAssignments(delayedAssignmentsBuildInfo, nextBuild));
          }
        }
      }

      @Override
      public void serverShutdown() {
        ThreadUtil.shutdownGracefully(myDaemon, "Investigator-Auto-Assigner Daemon");
      }
    });
  }

  private void processBrokenBuildsOneThread() {
    String description = String.format("Investigations auto-assigner: processing %s builds in background",
                                       myFailedBuilds.size());
    NamedThreadFactory.executeWithNewThreadName(description, this::processBrokenBuilds);
  }

  private void processDelayedAssignments(final FailedBuildInfo delayedAssignmentsBuildInfo, SBuild nextBuild) {
    String description = String.format("Investigations auto-assigner: processing delayed assignments for build %s" +
                                       " in background", delayedAssignmentsBuildInfo.getBuild().getBuildId());
    NamedThreadFactory.executeWithNewThreadName(
      description, () -> myDelayedAssignmentsProcessor.processBuild(delayedAssignmentsBuildInfo, nextBuild));
  }

  private void processFinishedBuild(@NotNull final FailedBuildInfo failedBuildInfo) {
    String description = String.format("Investigations auto-assigner: processing finished build %s in background",
                                       failedBuildInfo.getBuild().getBuildId());
    NamedThreadFactory.executeWithNewThreadName(description, () -> this.processBrokenBuild(failedBuildInfo));

    if (failedBuildInfo.shouldDelayAssignments && !failedBuildInfo.getHeuristicsResult().isEmpty()) {
      putIntoDelayAssignments(failedBuildInfo);
    }
  }

  private void putIntoDelayAssignments(final FailedBuildInfo failedBuildInfo) {
    @Nullable
    SBuildType sBuildType = failedBuildInfo.getBuild().getBuildType();
    if (sBuildType != null) {
      myDelayedAssignments.put(sBuildType.getInternalId(), failedBuildInfo);
    }
  }

  private void processBrokenBuilds() {
    for (FailedBuildInfo failedBuildInfo : myFailedBuilds.values()) {
      processBrokenBuild(failedBuildInfo);
    }
  }

  private synchronized void processBrokenBuild(final FailedBuildInfo failedBuildInfo) {
    myProcessor.processBuild(failedBuildInfo);
    myEmailReporter.sendResults(failedBuildInfo.getBuild(), failedBuildInfo.getHeuristicsResult());
  }

  /*
    We should ignore personal builds, builds for feature branches (by default),
    and handle the case when investigation suggestions are disabled.
   */
  private static boolean shouldIgnore(@NotNull SBuild build) {
    @Nullable
    Branch branch = build.getBranch();
    boolean isDefaultBranch = branch == null || branch.isDefaultBranch();

    if (build.isPersonal() || !(isDefaultBranch || CustomParameters.shouldRunForFeatureBranches(build))) {
      return true;
    }

    return !(CustomParameters.isBuildFeatureEnabled(build) || CustomParameters.isDefaultSilentModeEnabled(build));
  }
}
