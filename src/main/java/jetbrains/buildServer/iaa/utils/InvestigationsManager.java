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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.BuildProject;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.serverSide.audit.*;
import jetbrains.buildServer.serverSide.impl.audit.filters.ActionTypesFilter;
import jetbrains.buildServer.serverSide.impl.audit.filters.BuildProblemAuditId;
import jetbrains.buildServer.serverSide.impl.audit.filters.ObjectTypeFilter;
import jetbrains.buildServer.serverSide.impl.audit.filters.TestId;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InvestigationsManager {

  @NotNull private final AuditLogProvider myAuditLogProvider;

  InvestigationsManager(@NotNull final AuditLogProvider auditLogProvider) {
    this.myAuditLogProvider = auditLogProvider;
  }

  public boolean checkUnderInvestigation(@NotNull final SProject project,
                                         @NotNull final SBuild sBuild,
                                         @NotNull final BuildProblem problem) {
    for (BuildProblemResponsibilityEntry entry : problem.getAllResponsibilities()) {
      if (isActiveOrAlreadyFixed(sBuild, entry) && belongSameProjectOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  public boolean checkUnderInvestigation(@NotNull final SProject project,
                                         @NotNull final SBuild sBuild,
                                         @NotNull final STest test) {
    for (TestNameResponsibilityEntry entry : test.getAllResponsibilities()) {
      if (isActiveOrAlreadyFixed(sBuild, entry) && belongSameProjectOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  private boolean isActiveOrAlreadyFixed(@NotNull final SBuild sBuild, @NotNull final ResponsibilityEntry entry) {
    final ResponsibilityEntry.State state = entry.getState();
    return state.isActive() || (state.isFixed() && createdBeforeBuildQueued(entry, sBuild));
  }

  private static boolean createdBeforeBuildQueued(final ResponsibilityEntry entry, final SBuild sBuild) {
    return sBuild.getQueuedDate().getTime() - entry.getTimestamp().getTime() <= 0;
  }

  private boolean belongSameProjectOrParent(@NotNull final BuildProject parent, @NotNull final BuildProject project) {
    if (parent.getProjectId().equals(project.getProjectId())) return true;
    final BuildProject parentProject = project.getParentProject();
    return parentProject != null && belongSameProjectOrParent(parent, parentProject);
  }

  @Nullable
  public User findPreviousResponsible(@NotNull final SProject project,
                                      @NotNull final SBuild sBuild,
                                      @NotNull final BuildProblem problem) {
    User responsible = this.findInCache(project, sBuild, problem);
    if (responsible == null) {
      responsible = this.findInAudit(problem);
    }
    return responsible;
  }

  @Nullable
  private User findInCache(final SProject project, final SBuild sBuild, final BuildProblem problem) {
    for (BuildProblemResponsibilityEntry entry : problem.getAllResponsibilities()) {
      final ResponsibilityEntry.State state = entry.getState();
      if (state.isFixed() &&
          !createdBeforeBuildQueued(entry, sBuild) &&
          belongSameProjectOrParent(entry.getProject(), project)) {
        return entry.getResponsibleUser();
      }
    }
    return null;
  }

  @Nullable
  private User findInAudit(final BuildProblem buildProblem) {
    AuditLogBuilder builder = myAuditLogProvider.getBuilder();
    builder.setObjectId(BuildProblemAuditId.fromBuildProblem(buildProblem).asString());
    builder.addFilter(new ActionTypesFilter(ActionType.BUILD_PROBLEM_MARK_AS_FIXED));
    builder.addFilter(new ObjectTypeFilter(ObjectType.BUILD_PROBLEM));
    AuditLogAction lastAction = builder.findLastAction();
    if (lastAction == null) {
      return null;
    }

    for (ObjectWrapper obj : lastAction.getObjects()) {
      Object user = obj.getObject();
      if (user instanceof User) {
        return (User)user;
      }
    }
    return null;
  }

  @Nullable
  public User findPreviousResponsible(@NotNull final SProject project,
                                      @NotNull final SBuild sBuild,
                                      @NotNull final STest test) {
    User responsible = this.findInCache(project, sBuild, test);
    if (responsible == null) {
      responsible = this.findInAudit(test);
    }
    return responsible;
  }

  private User findInCache(final SProject project, final SBuild sBuild, final STest test) {
    for (TestNameResponsibilityEntry entry : test.getAllResponsibilities()) {
      final ResponsibilityEntry.State state = entry.getState();
      if (state.isFixed() &&
          !createdBeforeBuildQueued(entry, sBuild) &&
          belongSameProjectOrParent(entry.getProject(), project)) {
        return entry.getResponsibleUser();
      }
    }
    return null;
  }

  @Nullable
  private User findInAudit(final STest sTest) {
    AuditLogBuilder builder = myAuditLogProvider.getBuilder();
    builder.setObjectId(TestId.createOn(sTest).asString());
    builder.addFilter(new ActionTypesFilter(ActionType.TEST_MARK_AS_FIXED));
    builder.addFilter(new ObjectTypeFilter(ObjectType.TEST));
    AuditLogAction lastAction = builder.findLastAction();
    if (lastAction == null) {
      return null;
    }

    for (ObjectWrapper obj : lastAction.getObjects()) {
      Object user = obj.getObject();
      if (user instanceof User) {
        return (User)user;
      }
    }
    return null;
  }

  @Nullable
  public HashMap<String, AuditLogAction> findInAudit(@NotNull final Collection<STest> sTests) {
    AuditLogBuilder builder = myAuditLogProvider.getBuilder();
    builder.setActionTypes(ActionType.TEST_MARK_AS_FIXED);
    builder.addFilter(new ObjectIdsFilter(sTests.stream()
                                                .map(test -> TestId.createOn(test).asString())
                                                .collect(Collectors.toSet())));
    List<AuditLogAction> lastActions = builder.getLogActions(100);
    HashMap<String, AuditLogAction> result = new HashMap<>(100);
    for (AuditLogAction action : lastActions) {
      if (!result.containsKey(action.getObjectId())) {
        result.put(action.getObjectId(), action);
      }
    }
    return result;
  }
}
