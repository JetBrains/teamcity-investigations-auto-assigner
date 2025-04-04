package jetbrains.buildServer.investigationsAutoAssigner.utils;

import java.util.*;
import jetbrains.buildServer.BuildProject;
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
    this.myResponsibilityFacade = responsibilityFacade;
  }

  /**
   * Checks if a problem is under investigation for a given project and build.
   *
   * @param project The project to check.
   * @param sBuild  The build to check.
   * @param problem The build problem to check.
   * @return true if the problem is under investigation, false otherwise.
   */
  public boolean checkUnderInvestigation(@NotNull final SProject project,
                                         @NotNull final SBuild sBuild,
                                         @NotNull final BuildProblem problem) {
    return problem.getAllResponsibilities().stream().anyMatch(
      entry -> isActiveOrAlreadyFixed(sBuild, entry) && belongsToSameProjectOrParent(entry.getProject(), project));
  }

  /**
   * Checks if a test is under investigation for a given project and build.
   *
   * @param project The project to check.
   * @param sBuild  The build to check.
   * @param test    The test to check.
   * @return true if the test is under investigation, false otherwise.
   */
  public boolean checkUnderInvestigation(@NotNull final SProject project,
                                         @NotNull final SBuild sBuild,
                                         @NotNull final STest test) {
    return getInvestigation(project, sBuild, test) != null;
  }

  /**
   * Retrieves the responsibility entry for a test investigation.
   *
   * @param project The project to check.
   * @param sBuild  The build to check.
   * @param test    The test to check.
   * @return The responsibility entry if the test is under investigation, null otherwise.
   */
  @Nullable
  public TestNameResponsibilityEntry getInvestigation(@NotNull final SProject project,
                                                      @NotNull final SBuild sBuild,
                                                      @NotNull final STest test) {
    return test.getAllResponsibilities().stream().filter(
                 entry -> isActiveOrAlreadyFixed(sBuild, entry) && belongsToSameProjectOrParent(entry.getProject(), project))
               .findFirst().orElse(null);
  }

  /**
   * Checks if a responsibility entry is either active or has been fixed before the build was queued.
   *
   * @param sBuild The build to check against.
   * @param entry  The responsibility entry to check.
   * @return true if the entry is active or fixed before the build, false otherwise.
   */
  private boolean isActiveOrAlreadyFixed(@NotNull final SBuild sBuild, @NotNull final ResponsibilityEntry entry) {
    final ResponsibilityEntry.State state = entry.getState();
    return state.isActive() || (state.isFixed() && createdBeforeBuildQueued(entry, sBuild));
  }

  /**
   * Checks if the responsibility entry was created before the build was queued.
   *
   * @param entry  The responsibility entry to check.
   * @param sBuild The build to compare against.
   * @return true if the entry was created before the build was queued, false otherwise.
   */
  private static boolean createdBeforeBuildQueued(final ResponsibilityEntry entry, final SBuild sBuild) {
    return sBuild.getQueuedDate().getTime() - entry.getTimestamp().getTime() <= 0;
  }

  /**
   * Checks if a project belongs to the same project or parent project.
   *
   * @param parent  The parent project.
   * @param project The project to check.
   * @return true if the project or its parent matches the given parent project.
   */
  private boolean belongsToSameProjectOrParent(@NotNull final BuildProject parent,
                                               @NotNull final BuildProject project) {
    List<String> projectIds = collectProjectHierarchyIds(project);
    return projectIds.contains(parent.getProjectId());
  }

  /**
   * Finds the previous responsible user for a build problem.
   *
   * @param project The project to check.
   * @param sBuild  The build to check.
   * @param problem The build problem.
   * @return The user who was previously responsible for the problem, or null if none found.
   */
  @Nullable
  public User findPreviousResponsible(@NotNull final SProject project,
                                      @NotNull final SBuild sBuild,
                                      @NotNull final BuildProblem problem) {
    User responsible = this.findAmongEntries(project, sBuild, problem.getAllResponsibilities());
    return responsible != null ? responsible : this.findInAudit(problem);
  }

  /**
   * Finds the responsible user for a build problem in the audit log.
   *
   * @param buildProblem The build problem to check.
   * @return The user who was responsible in the audit log, or null if none found.
   */
  @Nullable
  private User findInAudit(final BuildProblem buildProblem) {
    AuditLogBuilder builder = myAuditLogProvider.getBuilder();
    builder.setObjectId(BuildProblemAuditId.fromBuildProblem(buildProblem).asString());
    builder.setActionTypes(ActionType.BUILD_PROBLEM_MARK_AS_FIXED);
    builder.addFilter(new ObjectTypeFilter(ObjectType.BUILD_PROBLEM));
    AuditLogAction lastAction = builder.findLastAction();
    return lastAction == null ? null : extractUserFromAction(lastAction);
  }

  /**
   * Extracts the user from an audit log action.
   *
   * @param action The audit log action.
   * @return The user responsible, or null if none found.
   */
  @Nullable
  private User extractUserFromAction(AuditLogAction action) {
    for (ObjectWrapper obj : action.getObjects()) {
      if (obj.getObject() instanceof User) {
        return (User)obj.getObject();
      }
    }
    return null;
  }

  /**
   * Finds the previous responsible user for a test.
   *
   * @param project The project to check.
   * @param sBuild  The build to check.
   * @param sTest   The test to check.
   * @return The user responsible for the test, or null if none found.
   */
  @Nullable
  public User findPreviousResponsible(@NotNull final SProject project,
                                      @NotNull final SBuild sBuild,
                                      @NotNull final STest sTest) {
    return this.findAmongEntries(project, sBuild, sTest.getAllResponsibilities());
  }

  /**
   * Finds the responsible user from a list of responsibility entries.
   *
   * @param project               The project to check.
   * @param sBuild                The build to check.
   * @param responsibilityEntries The list of responsibility entries.
   * @return The user responsible, or null if none found.
   */
  @Nullable
  private User findAmongEntries(final SProject project,
                                final SBuild sBuild,
                                List<? extends ResponsibilityEntry> responsibilityEntries) {
    return responsibilityEntries.stream().filter(entry -> isValidEntryForResponsibility(project, sBuild, entry))
                                .map(ResponsibilityEntry::getResponsibleUser).findFirst().orElse(null);
  }

  /**
   * Checks if a responsibility entry is valid for assigning responsibility to a user.
   *
   * @param project The project to check.
   * @param sBuild  The build to check.
   * @param entry   The responsibility entry.
   * @return true if the entry is valid, false otherwise.
   */
  private boolean isValidEntryForResponsibility(@NotNull final SProject project,
                                                @NotNull final SBuild sBuild,
                                                ResponsibilityEntry entry) {
    BuildProject entryProject = myResponsibilityFacade.getProject(entry);
    final ResponsibilityEntry.State state = entry.getState();
    return state.isFixed() && !createdBeforeBuildQueued(entry, sBuild) && entryProject != null &&
           belongsToSameProjectOrParent(entryProject, project);
  }

  /**
   * Finds responsible users from audit log for a list of test runs.
   *
   * @param sTestRuns The test runs to check.
   * @param project   The project to check.
   * @return A map of test name IDs to responsible users.
   */
  @NotNull
  public HashMap<Long, User> findInAudit(@NotNull final Iterable<STestRun> sTestRuns, @NotNull SProject project) {
    AuditLogBuilder builder = myAuditLogProvider.getBuilder();
    builder.setActionTypes(ActionType.TEST_MARK_AS_FIXED, ActionType.TEST_INVESTIGATION_ASSIGN);
    Set<String> objectIds = collectTestIdsForRuns(sTestRuns, project);
    if (objectIds.isEmpty() && skipAuditLookupWithoutTests()) {
      return new HashMap<>();
    }
    builder.setObjectIds(objectIds);
    List<AuditLogAction> lastActions = builder.getLogActions(-1);
    return extractUsersFromAuditActions(lastActions);
  }

  /**
   * Collects test IDs from test runs for a given project.
   *
   * @param sTestRuns The test runs to check.
   * @param project   The project to check.
   * @return A set of object IDs.
   */
  private Set<String> collectTestIdsForRuns(@NotNull Iterable<STestRun> sTestRuns, @NotNull SProject project) {
    List<String> projectIds = collectProjectHierarchyIds(project);
    Set<String> objectIds = new HashSet<>();
    for (STestRun testRun : sTestRuns) {
      for (String projectId : projectIds) {
        objectIds.add(TestId.createOn(testRun.getTest().getTestNameId(), projectId).asString());
      }
    }
    return objectIds;
  }

  /**
   * Determines if the audit lookup should be skipped when no tests are found.
   *
   * @return true if audit lookup should be skipped, false otherwise.
   */
  private boolean skipAuditLookupWithoutTests() {
    return TeamCityProperties.getBooleanOrTrue("teamcity.autoAssigner.skipAuditLookupWithoutTests.enabled");
  }

  /**
   * Extracts users from audit log actions.
   *
   * @param lastActions The list of audit log actions.
   * @return A map of test name IDs to responsible users.
   */
  @NotNull
  private HashMap<Long, User> extractUsersFromAuditActions(List<AuditLogAction> lastActions) {
    HashMap<Long, User> result = new HashMap<>();
    for (AuditLogAction action : lastActions) {
      for (ObjectWrapper obj : action.getObjects()) {
        if (obj.getObject() instanceof User) {
          TestId testId = TestId.fromString(action.getObjectId());
          if (testId != null) {
            result.putIfAbsent(testId.getTestNameId(), (User)obj.getObject());
          }
        }
      }
    }
    return result;
  }

  /**
   * Collects project hierarchy IDs starting from a given project.
   *
   * @param project The starting project.
   * @return A list of project IDs representing the hierarchy.
   */
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
