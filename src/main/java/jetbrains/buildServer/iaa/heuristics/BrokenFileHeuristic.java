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

import com.intellij.openapi.util.Pair;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.iaa.ProblemInfo;
import jetbrains.buildServer.iaa.common.Constants;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildPromotionEx;
import jetbrains.buildServer.serverSide.ChangeDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.join;

public class BrokenFileHeuristic implements Heuristic {
  @Override
  public long getOrder() {
    return 1;
  }

  @Override
  @NotNull
  public String getName() {
    return "Detect Broken File Heuristic";
  }

  @Override
  @NotNull
  public String getDescription() {
    return "Assign an investigation to a user, if there are no other committers but him that changed a \"broken\"" +
           " file. The \"broken\" file is which could probably cause this failure.";
  }

  @Override
  @Nullable
  public Pair<SUser, String> findResponsibleUser(@NotNull ProblemInfo problemInfo) {
    if (problemInfo.myProblemText == null) return null;

    final BuildPromotion buildPromotion = problemInfo.mySBuild.getBuildPromotion();
    if (!(buildPromotion instanceof BuildPromotionEx)) return null;

    SelectPrevBuildPolicy prevBuildPolicy = SelectPrevBuildPolicy.SINCE_LAST_BUILD;
    List<SVcsModification> vcsChanges = ((BuildPromotionEx)buildPromotion).getDetectedChanges(prevBuildPolicy, true)
                                                                          .stream()
                                                                          .map(ChangeDescriptor::getRelatedVcsChange)
                                                                          .filter(Objects::nonNull)
                                                                          .collect(Collectors.toList());
    SUser responsibleUser = null;
    String brokenFile = null;
    for (SVcsModification vcsChange : vcsChanges) {
      final String foundBrokenFile = findBrokenFile(vcsChange, problemInfo.myProblemText);
      if (foundBrokenFile == null) continue;

      final Collection<SUser> changeCommitters = vcsChange.getCommitters();
      if (changeCommitters.size() != 1) return null;

      final SUser foundResponsibleUser = changeCommitters.iterator().next();
      if (responsibleUser != null && !responsibleUser.equals(foundResponsibleUser)) return null;

      responsibleUser = foundResponsibleUser;
      brokenFile = foundBrokenFile;
    }

    if (responsibleUser == null) return null;
    return Pair.create(responsibleUser, String.format("%s you changed the \"%s\" file, which could probably cause" +
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
