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

/**
 * Handles storing and retrieving heuristic-based test failure responsibility assignments.
 * The results are persisted in an artifact file associated with a specific build.
 */
public class AssignerArtifactDao {
  private static final Logger LOGGER = Constants.LOGGER;
  private final UserModelEx userModel;
  private final SuggestionsDao suggestionsDao;
  private final AssignerResultsFilePath assignerResultsFilePath;
  private final StatisticsReporter statisticsReporter;

  /**
   * Constructs a new AssignerArtifactDao.
   *
   * @param userModel               The user model for resolving user information.
   * @param suggestionsDao          DAO for reading and writing suggested responsibilities.
   * @param assignerResultsFilePath Handles file paths for persisting assignments.
   * @param statisticsReporter      Reports statistics about saved suggestions.
   */
  public AssignerArtifactDao(@NotNull final UserModelEx userModel,
                             @NotNull final SuggestionsDao suggestionsDao,
                             @NotNull final AssignerResultsFilePath assignerResultsFilePath,
                             @NotNull final StatisticsReporter statisticsReporter) {
    this.userModel = userModel;
    this.suggestionsDao = suggestionsDao;
    this.assignerResultsFilePath = assignerResultsFilePath;
    this.statisticsReporter = statisticsReporter;
  }

  /**
   * Appends heuristic results to the persisted file for a given build.
   *
   * @param build           The build associated with the test runs.
   * @param testRuns        The test runs being analyzed.
   * @param heuristicResult The heuristic results mapping tests to responsible users.
   */
  public void appendHeuristicsResult(@NotNull SBuild build,
                                     @NotNull List<STestRun> testRuns,
                                     @NotNull HeuristicResult heuristicResult) {
    doAppend(build, getPersistentInfoList(testRuns, heuristicResult));
  }

  /**
   * Appends results to the file, ensuring previous results are preserved.
   *
   * @param build     The build associated with the test runs.
   * @param infoToAdd The list of results to append.
   */
  private void doAppend(@NotNull final SBuild build, @NotNull List<ResponsibilityPersistentInfo> infoToAdd) {
    if (infoToAdd.isEmpty()) return;

    try {
      this.statisticsReporter.reportSavedSuggestions(infoToAdd.size());
      Path resultsFilePath = this.assignerResultsFilePath.get(build);

      List<ResponsibilityPersistentInfo> previouslyAdded = this.suggestionsDao.read(resultsFilePath);

      if (previouslyAdded.isEmpty()) {
        this.statisticsReporter.reportBuildWithSuggestions();
      }

      infoToAdd.addAll(previouslyAdded);
      logAppendResults(build, previouslyAdded.size());

      this.suggestionsDao.write(resultsFilePath, infoToAdd);
      logWriteResults(build, previouslyAdded.size(), infoToAdd.size());

    } catch (IOException ex) {
      LOGGER.warn(String.format("Build id:%s :: Error during appending results", build.getBuildId()), ex);
    }
  }

  /**
   * Logs the number of results that were appended to the file.
   *
   * @param build               The build associated with the test runs.
   * @param previouslyAddedSize The number of results previously in the file.
   */
  private void logAppendResults(@NotNull SBuild build, int previouslyAddedSize) {
    LOGGER.debug(
      String.format("Build id:%s :: Read %d previously added investigations", build.getBuildId(), previouslyAddedSize));
  }

  /**
   * Logs the number of new results that were written to the file.
   *
   * @param build               The build associated with the test runs.
   * @param previouslyAddedSize The number of results previously in the file.
   * @param newResultsSize      The total number of results written.
   */
  private void logWriteResults(@NotNull SBuild build, int previouslyAddedSize, int newResultsSize) {
    LOGGER.debug(String.format("Build id:%s :: Wrote %d new found investigations", build.getBuildId(),
                               newResultsSize - previouslyAddedSize));
  }

  /**
   * Converts test run responsibility assignments into a persistent format.
   *
   * @param testRuns        The test runs being analyzed.
   * @param heuristicResult The heuristic-based responsibilities.
   * @return A list of persistent responsibility records.
   */
  @NotNull
  private List<ResponsibilityPersistentInfo> getPersistentInfoList(@NotNull final List<STestRun> testRuns,
                                                                   @NotNull final HeuristicResult heuristicResult) {
    List<ResponsibilityPersistentInfo> result = new ArrayList<>();
    for (STestRun testRun : testRuns) {
      Responsibility responsibility = heuristicResult.getResponsibility(testRun);
      if (responsibility != null) {
        result.add(createPersistentInfo(testRun, responsibility));
      }
    }
    return result;
  }

  /**
   * Creates a persistent responsibility record from a test run and its associated responsibility.
   *
   * @param testRun        The test run being analyzed.
   * @param responsibility The responsibility assignment for the test run.
   * @return A persistent responsibility record.
   */
  @NotNull
  private ResponsibilityPersistentInfo createPersistentInfo(@NotNull STestRun testRun,
                                                            @NotNull Responsibility responsibility) {
    return new ResponsibilityPersistentInfo(String.valueOf(testRun.getTest().getTestNameId()),
                                            String.valueOf(responsibility.getUser().getId()),
                                            responsibility.getDescription());
  }

  /**
   * Retrieves the responsibility assignment for a given test run, if available.
   *
   * @param firstFailedBuild The first failed build associated with the test run.
   * @param testRun          The test run in question.
   * @return The assigned responsibility or null if none exists.
   */
  @Nullable
  public Responsibility get(@Nullable SBuild firstFailedBuild, @NotNull STestRun testRun) {
    List<ResponsibilityPersistentInfo> suggestions = readSuggestions(firstFailedBuild, testRun);
    if (suggestions == null) return null;

    return findResponsibility(testRun, suggestions);
  }

  /**
   * Reads the suggestions from the persisted file for a given test run.
   *
   * @param firstFailedBuild The first failed build associated with the test run.
   * @param testRun          The test run being analyzed.
   * @return A list of responsibility records or null if an error occurs.
   */
  @Nullable
  private List<ResponsibilityPersistentInfo> readSuggestions(@Nullable SBuild firstFailedBuild,
                                                             @NotNull STestRun testRun) {
    try {
      Path resultsFilePath = getResultsFilePath(firstFailedBuild, testRun);
      return this.suggestionsDao.read(resultsFilePath);
    } catch (IOException ex) {
      LOGGER.warn(String.format("%s An error occurred while reading the results file", Utils.getLogPrefix(testRun)),
                  ex);
      return null;
    }
  }

  /**
   * Appends descriptions for tests that are not applicable to any responsibility assignment.
   *
   * @param build                         The build associated with the test runs.
   * @param notApplicableTestsDescription A map of test IDs and their descriptions.
   */
  public void appendNotApplicableTestsDescription(@NotNull final SBuild build,
                                                  @NotNull final Map<Long, String> notApplicableTestsDescription) {
    doAppend(build, getPersistentInfoList(notApplicableTestsDescription));
  }

  /**
   * Converts a map of not applicable test descriptions into a list of persistent responsibility records.
   *
   * @param notApplicableTestsDescription A map of test IDs and their descriptions.
   * @return A list of persistent responsibility records for not applicable tests.
   */
  @NotNull
  private List<ResponsibilityPersistentInfo> getPersistentInfoList(final Map<Long, String> notApplicableTestsDescription) {
    List<ResponsibilityPersistentInfo> result = new ArrayList<>();
    for (final Map.Entry<Long, String> longStringEntry : notApplicableTestsDescription.entrySet()) {
      result.add(
        new ResponsibilityPersistentInfo(longStringEntry.getKey().toString(), Constants.ASSIGNEE_FILTERED_LITERAL,
                                         longStringEntry.getValue()));
    }

    return result;
  }

  /**
   * Retrieves the file path for storing results, if it exists.
   *
   * @param firstFailedBuild The first failed build associated with the test run.
   * @param testRun          The test run being analyzed.
   * @return The file path or null if not found.
   * @throws IOException If an error occurs while retrieving the file path.
   */
  @Nullable
  private Path getResultsFilePath(@Nullable SBuild firstFailedBuild, @NotNull STestRun testRun) throws IOException {
    return firstFailedBuild != null
           ? this.assignerResultsFilePath.getIfExist(firstFailedBuild, testRun)
           : this.assignerResultsFilePath.getIfExist(testRun.getBuild(), testRun);
  }

  /**
   * Finds the responsibility for a test run from a list of suggestions.
   *
   * @param testRun     The test run being analyzed.
   * @param suggestions The list of responsibility records.
   * @return The found responsibility or null if none exists.
   */
  @Nullable
  private Responsibility findResponsibility(@NotNull STestRun testRun,
                                            @NotNull List<ResponsibilityPersistentInfo> suggestions) {
    for (ResponsibilityPersistentInfo persistentInfo : suggestions) {
      if (persistentInfo.testNameId.equals(String.valueOf(testRun.getTest().getTestNameId()))) {
        return createResponsibility(persistentInfo);
      }
    }
    logNotFound(testRun);
    return null;
  }

  /**
   * Creates a responsibility object from a persistent responsibility record.
   *
   * @param persistentInfo The persistent responsibility record.
   * @return A responsibility object or null if not found.
   */
  @Nullable
  private Responsibility createResponsibility(@NotNull ResponsibilityPersistentInfo persistentInfo) {
    if (persistentInfo.investigatorId.equals(Constants.ASSIGNEE_FILTERED_LITERAL)) {
      return getFilteredResponsibility(persistentInfo);
    }

    LOGGER.debug(String.format("Investigation found for testRun %s", persistentInfo.testNameId));
    return getUserResponsibility(persistentInfo);
  }

  /**
   * Creates a responsibility for a filtered test.
   *
   * @param persistentInfo The persistent responsibility record for a filtered test.
   * @return A filtered responsibility or null if filtering is not enabled.
   */
  @Nullable
  private Responsibility getFilteredResponsibility(@NotNull ResponsibilityPersistentInfo persistentInfo) {
    return TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION) ? new Responsibility(
      this.userModel.getGuestUser(), Constants.ASSIGNEE_FILTERED_DESCRIPTION_PREFIX + persistentInfo.reason) : null;
  }

  /**
   * Creates a responsibility for a user-assigned test.
   *
   * @param persistentInfo The persistent responsibility record for a user-assigned test.
   * @return A user responsibility or null if the user is not found.
   */
  @Nullable
  private Responsibility getUserResponsibility(@NotNull ResponsibilityPersistentInfo persistentInfo) {
    User user = this.userModel.findUserById(Long.parseLong(persistentInfo.investigatorId));
    if (user == null) {
      LOGGER.warn(String.format("User with id '%s' not found.", persistentInfo.investigatorId));
      return null;
    }
    return new Responsibility(user, persistentInfo.reason);
  }

  /**
   * Logs when a responsibility is not found for a test run.
   *
   * @param testRun The test run being analyzed.
   */
  private void logNotFound(@NotNull STestRun testRun) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("%s Investigation for testRun '%s' not found", Utils.getLogPrefix(testRun),
                                 testRun.getTestRunId()));
    }
  }
}
