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

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.investigationsAutoAssigner.utils.ProblemTextExtractor;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BrokenFileHeuristic implements Heuristic {

  private static final Logger LOGGER = Logger.getInstance(Constants.LOGGING_CATEGORY);
  private final ProblemTextExtractor myProblemTextExtractor;
  private ModificationAnalyzerFactory myModificationAnalyzerFactory;

  public BrokenFileHeuristic(ProblemTextExtractor problemTextExtractor,
                             ModificationAnalyzerFactory modificationAnalyzerFactory) {
    myProblemTextExtractor = problemTextExtractor;
    myModificationAnalyzerFactory = modificationAnalyzerFactory;
  }

  @Override
  @NotNull
  public String getId() {
    return "BrokenFile";
  }

  @NotNull
  public HeuristicResult findResponsibleUser(@NotNull HeuristicContext heuristicContext) {
    HeuristicResult result = new HeuristicResult();
    SBuild sBuild = heuristicContext.getBuild();

    final BuildPromotion buildPromotion = sBuild.getBuildPromotion();
    if (!(buildPromotion instanceof BuildPromotionEx)) return result;

    SelectPrevBuildPolicy prevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    List<SVcsModification> vcsChanges = ((BuildPromotionEx)buildPromotion).getDetectedChanges(prevBuildPolicy, false)
                                                                          .stream()
                                                                          .map(ChangeDescriptor::getRelatedVcsChange)
                                                                          .filter(Objects::nonNull)
                                                                          .collect(Collectors.toList());
    for (STestRun sTestRun : heuristicContext.getTestRuns()) {
      String problemText = myProblemTextExtractor.getBuildProblemText(sTestRun);
      Responsibility responsibility = findResponsibleUser(vcsChanges, problemText, heuristicContext);
      if (responsibility != null) {
        result.addResponsibility(sTestRun, responsibility);
      }
    }

    for (BuildProblem buildProblem : heuristicContext.getBuildProblems()) {
      String problemText = myProblemTextExtractor.getBuildProblemText(buildProblem, sBuild);
      Responsibility responsibility = findResponsibleUser(vcsChanges, problemText, heuristicContext);
      if (responsibility != null) {
        result.addResponsibility(buildProblem, responsibility);
      }
    }

    return result;
  }

  @Nullable
  private Responsibility findResponsibleUser(List<SVcsModification> vcsChanges,
                                             String problemText,
                                             HeuristicContext heuristicContext) {
    Pair<User, String> foundBrokenFile = null;
    for (SVcsModification vcsChange : vcsChanges) {
      try {
        ModificationAnalyzerFactory.ModificationAnalyzer vcsChangeWrapped = myModificationAnalyzerFactory.getInstance(vcsChange);
        Pair<User, String> brokenFile = vcsChangeWrapped.findProblematicFile(problemText, heuristicContext.getUsersToIgnore());
        if (brokenFile == null) continue;

        ensureSameUsers(foundBrokenFile, brokenFile);
        foundBrokenFile = brokenFile;
      } catch (IllegalStateException ex) {
        LOGGER.debug(ex.getMessage() + ". build: " + heuristicContext.getBuild().getBuildId() + " is incompatible for the Broken File heuristic.");
        return null;
      }
    }

    if (foundBrokenFile == null) return null;

    String description =
      String.format("changed the suspicious file \"%s\" which probably broke the build", foundBrokenFile.second);
    return new Responsibility(foundBrokenFile.first, description);
  }

  private void ensureSameUsers(@Nullable Pair<User, String> foundBrokenFile,
                               @Nullable final Pair<User, String> broken) {
    if (foundBrokenFile != null &&
        broken != null &&
        !foundBrokenFile.first.equals(broken.first)) {
      throw new IllegalStateException("There are at least one unknown for TeamCity user");
    }
  }
}

