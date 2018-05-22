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

package jetbrains.buildServer.iaa.heuristics;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.FailedBuildContext;
import jetbrains.buildServer.iaa.HeuristicResult;
import jetbrains.buildServer.iaa.Responsibility;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.iaa.utils.ProblemTextExtractor;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.join;

public class BrokenFileHeuristic implements Heuristic {

  private static final Logger LOGGER = Logger.getInstance(BrokenFileHeuristic.class.getName());
  private final ProblemTextExtractor myProblemTextExtractor;

  BrokenFileHeuristic(ProblemTextExtractor problemTextExtractor) {
    myProblemTextExtractor = problemTextExtractor;
  }

  @Override
  @NotNull
  public String getName() {
    return "Detect Broken File Heuristic";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "Assign an investigation to a user if the user is the only committer " +
           "who changed the suspicious file. The suspicious file is the one that probably caused this failure.";
  }

  public HeuristicResult findResponsibleUser(@NotNull FailedBuildContext failedBuildContext) {
    HeuristicResult result = new HeuristicResult();
    SBuild sBuild = failedBuildContext.getSBuild();

    final BuildPromotion buildPromotion = sBuild.getBuildPromotion();
    if (!(buildPromotion instanceof BuildPromotionEx)) return result;

    SelectPrevBuildPolicy prevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    List<SVcsModification> vcsChanges = ((BuildPromotionEx)buildPromotion).getDetectedChanges(prevBuildPolicy, true)
                                                                          .stream()
                                                                          .map(ChangeDescriptor::getRelatedVcsChange)
                                                                          .filter(Objects::nonNull)
                                                                          .collect(Collectors.toList());
    for (STestRun sTestRun : failedBuildContext.getTestRuns()) {
      String problemText = myProblemTextExtractor.getBuildProblemText(sTestRun);
      Responsibility responsibility = findResponsibleUser(vcsChanges, sBuild, problemText);
      if (responsibility != null)
        result.addResponsibility(sTestRun, responsibility);
    }

    for (BuildProblem buildProblem : failedBuildContext.getBuildProblems()) {
      String problemText = myProblemTextExtractor.getBuildProblemText(buildProblem, sBuild);
      Responsibility responsibility = findResponsibleUser(vcsChanges, sBuild, problemText);
      if (responsibility != null)
        result.addResponsibility(buildProblem, responsibility);
    }

    return result;
  }

  @Nullable
  private Responsibility findResponsibleUser(List<SVcsModification> vcsChanges, SBuild sBuild, String problemText) {
    SUser responsibleUser = null;
    String brokenFile = null;
    for (SVcsModification vcsChange : vcsChanges) {
      final String foundBrokenFile = findBrokenFile(vcsChange, problemText);
      if (foundBrokenFile == null) continue;

      final Collection<SUser> changeCommitters = vcsChange.getCommitters();
      if (changeCommitters.size() != 1) return null;

      final SUser foundResponsibleUser = changeCommitters.iterator().next();
      if (responsibleUser != null && !responsibleUser.equals(foundResponsibleUser)) {
        LOGGER.debug("There are more then one committers since last build for failed build #" + sBuild.getBuildId());
        return null;
      }

      responsibleUser = foundResponsibleUser;
      brokenFile = foundBrokenFile;
    }

    if (responsibleUser == null) return null;

    return new Responsibility(responsibleUser, String.format("%s you changed the \"%s\" file, which probably caused" +
                                                             " this failure.", Constants.REASON_PREFIX, brokenFile));
  }

  @Nullable
  private static String findBrokenFile(@NotNull final SVcsModification vcsChange, @NotNull final String problemText) {
    for (VcsFileModification modification : vcsChange.getChanges()) {
      final String filePath = modification.getRelativeFileName();
      for (String pattern : getPatterns(filePath)) {
        if (problemText.contains(pattern)) {
          return filePath;
        }
      }
    }
    return null;
  }

  @NotNull
  private static List<String> getPatterns(@NotNull final String filePath) {
    final List<String> parts = new ArrayList<>();
    parts.add(FileUtil.getNameWithoutExtension(new File(filePath)));

    String path = getParentPath(filePath);
    if (path != null) {
      parts.add(0, new File(path).getName());
      path = getParentPath(path);
      if (path != null) {
        parts.add(0, new File(path).getName());
      }
    }

    return Arrays.asList(join(parts, "."), join(parts, "/"), join(parts, "\\"));
  }

  // we do not use File#getParentFile() instead because we must not take current
  // working directory into account, i.e. getParentPath("abc") must return null
  @Nullable
  private static String getParentPath(@NotNull final String path) {
    final int lastSlashPos = path.replace('\\', '/').lastIndexOf('/');
    return lastSlashPos == -1 ? null : path.substring(0, lastSlashPos);
  }
}

