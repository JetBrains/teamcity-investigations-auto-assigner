

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.*;
import jetbrains.buildServer.BuildProject;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityEntry;
import jetbrains.buildServer.responsibility.ResponsibilityFacadeEx;
import jetbrains.buildServer.responsibility.TestNameResponsibilityEntry;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.audit.*;
import jetbrains.buildServer.serverSide.impl.audit.filters.BuildProblemAuditId;
import jetbrains.buildServer.serverSide.impl.audit.filters.ObjectTypeFilter;
import jetbrains.buildServer.serverSide.impl.audit.filters.TestId;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InvestigationsManager {

  @NotNull private final AuditLogProvider myAuditLogProvider;
  @NotNull private final ResponsibilityFacadeEx myResponsibilityFacade;

  public InvestigationsManager(@NotNull final AuditLogProvider auditLogProvider,
                               @NotNull final ResponsibilityFacadeEx responsibilityFacade) {
    this.myAuditLogProvider = auditLogProvider;
    myResponsibilityFacade = responsibilityFacade;
  }

  public boolean checkUnderInvestigation(@NotNull final SProject project,
                                         @NotNull final SBuild sBuild,
                                         @NotNull final BuildProblem problem) {
    for (BuildProblemResponsibilityEntry entry : problem.getAllResponsibilities()) {
      if (isActiveOrAlreadyFixed(sBuild, entry) && belongsToSameProjectOrParent(entry.getProject(), project)) return true;
    }
    return false;
  }

  public boolean checkUnderInvestigation(@NotNull final SProject project,
                                         @NotNull final SBuild sBuild,
                                         @NotNull final STest test) {
    return getInvestigation(project, sBuild, test) != null;
  }

  @Nullable
  public TestNameResponsibilityEntry getInvestigation(@NotNull final SProject project,
                                                      @NotNull final SBuild sBuild,
                                                      @NotNull final STest test) {
    for (TestNameResponsibilityEntry entry : test.getAllResponsibilities()) {
      if (isActiveOrAlreadyFixed(sBuild, entry) && belongsToSameProjectOrParent(entry.getProject(), project)) return entry;
    }
    return null;
  }

  private boolean isActiveOrAlreadyFixed(@NotNull final SBuild sBuild, @NotNull final ResponsibilityEntry entry) {
    final ResponsibilityEntry.State state = entry.getState();
    return state.isActive() || (state.isFixed() && createdBeforeBuildQueued(entry, sBuild));
  }

  private static boolean createdBeforeBuildQueued(final ResponsibilityEntry entry, final SBuild sBuild) {
    return sBuild.getQueuedDate().getTime() - entry.getTimestamp().getTime() <= 0;
  }

  private boolean belongsToSameProjectOrParent(@NotNull final BuildProject parent, @NotNull final BuildProject project) {
    List<String> projectIds = collectProjectHierarchyIds(project);
    return projectIds.contains(parent.getProjectId());
  }

  @Nullable
  public User findPreviousResponsible(@NotNull final SProject project,
                                      @NotNull final SBuild sBuild,
                                      @NotNull final BuildProblem problem) {
    User responsible = this.findAmongEntries(project, sBuild, problem.getAllResponsibilities());
    if (responsible == null) {
      responsible = this.findInAudit(problem);
    }
    return responsible;
  }

  @Nullable
  private User findInAudit(final BuildProblem buildProblem) {
    AuditLogBuilder builder = myAuditLogProvider.getBuilder();
    builder.setObjectId(BuildProblemAuditId.fromBuildProblem(buildProblem).asString());
    builder.setActionTypes(ActionType.BUILD_PROBLEM_MARK_AS_FIXED);
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
  public User findPreviousResponsible(@NotNull final SProject sProject,
                                      @NotNull final SBuild sBuild,
                                      @NotNull final STest sTest) {
    return this.findAmongEntries(sProject, sBuild, sTest.getAllResponsibilities());
  }

  @Nullable
  private User findAmongEntries(final SProject project,
                                final SBuild sBuild,
                                List<? extends ResponsibilityEntry> responsibilityEntries) {
    for (ResponsibilityEntry entry : responsibilityEntries) {
      BuildProject entryProject = myResponsibilityFacade.getProject(entry);
      final ResponsibilityEntry.State state = entry.getState();
      if (state.isFixed() &&
          !createdBeforeBuildQueued(entry, sBuild) &&
          entryProject != null &&
          belongsToSameProjectOrParent(entryProject, project)) {
        return entry.getResponsibleUser();
      }
    }
    return null;
  }

  @NotNull
  public Map<Long, User> findInAudit(@NotNull final Iterable<STestRun> sTestRuns, @NotNull SProject project) {
    AuditLogBuilder builder = myAuditLogProvider.getBuilder();
    builder.setActionTypes(ActionType.TEST_MARK_AS_FIXED, ActionType.TEST_INVESTIGATION_ASSIGN);
    List<String> projectIds = collectProjectHierarchyIds(project);
    Set<String> objectIds = new HashSet<>();
    for (STestRun testRun : sTestRuns) {
      for (String projectId : projectIds) {
        objectIds.add(TestId.createOn(testRun.getTest().getTestNameId(), projectId).asString());
      }
    }
    if (objectIds.isEmpty() && TeamCityProperties.getBooleanOrTrue("teamcity.autoAssigner.skipAuditLookupWithoutTests.enabled")) {
      return new HashMap<>();
    }
    builder.setObjectIds(objectIds);
    List<AuditLogAction> lastActions = builder.getLogActions(-1);
    Map<Long, User> result = new HashMap<>();
    for (AuditLogAction action : lastActions) {
      for (ObjectWrapper obj : action.getObjects()) {
        Object user = obj.getObject();
        if (!(user instanceof User)) {
          continue;
        }

        TestId testId = TestId.fromString(action.getObjectId());
        if (testId != null) {
          result.putIfAbsent(testId.getTestNameId(), (User)user);
          break;
        }
      }
    }
    return result;
  }

  @NotNull
  private List<String> collectProjectHierarchyIds(@NotNull BuildProject project) {
    List<String> result = new ArrayList<>();
    do {
      result.add(project.getProjectId());
      project = project.getParentProject();
    } while (project != null);
    return result;
  }
}
