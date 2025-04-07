

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

// Optimized AssignerArtifactDao.java
package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

public class AssignerArtifactDao {
  private static final Logger LOGGER = Constants.LOGGER;
  private final UserModelEx userModel;
  private final SuggestionsDao suggestionsDao;
  private final AssignerResultsFilePath resultsFilePathResolver;
  private final StatisticsReporter statisticsReporter;

  public AssignerArtifactDao(@NotNull UserModelEx userModel,
                             @NotNull SuggestionsDao suggestionsDao,
                             @NotNull AssignerResultsFilePath resultsFilePath,
                             @NotNull StatisticsReporter statisticsReporter) {
    this.userModel = userModel;
    this.suggestionsDao = suggestionsDao;
    this.resultsFilePathResolver = resultsFilePath;
    this.statisticsReporter = statisticsReporter;
  }

  public void appendHeuristicsResult(@NotNull SBuild build,
                                     @NotNull List<STestRun> testRuns,
                                     @NotNull HeuristicResult heuristicResult) {
    List<ResponsibilityPersistentInfo> entries = testRuns.stream()
                                                         .map(testRun -> Optional.ofNullable(heuristicResult.getResponsibility(testRun))
                                                                                 .map(responsibility -> new ResponsibilityPersistentInfo(
                                                                                   String.valueOf(testRun.getTest().getTestNameId()),
                                                                                   String.valueOf(responsibility.getUser().getId()),
                                                                                   responsibility.getDescription()))
                                                                                 .orElse(null))
                                                         .filter(info -> info != null)
                                                         .collect(Collectors.toList());

    persistResponsibilities(build, entries);
  }

  public void appendNotApplicableTestsDescription(@NotNull SBuild build,
                                                  @NotNull Map<Long, String> notApplicableDescriptions) {
    List<ResponsibilityPersistentInfo> entries = notApplicableDescriptions.entrySet().stream()
                                                                          .map(e -> new ResponsibilityPersistentInfo(
                                                                            String.valueOf(e.getKey()),
                                                                            Constants.ASSIGNEE_FILTERED_LITERAL,
                                                                            e.getValue()))
                                                                          .collect(Collectors.toList());

    persistResponsibilities(build, entries);
  }

  @Nullable
  public Responsibility get(@Nullable SBuild firstFailedBuild, @NotNull STestRun testRun) {
    try {
      Path path = firstFailedBuild != null
                  ? resultsFilePathResolver.getIfExist(firstFailedBuild, testRun)
                  : resultsFilePathResolver.getIfExist(testRun.getBuild(), testRun);

      List<ResponsibilityPersistentInfo> suggestions = suggestionsDao.read(path);
      return suggestions.stream()
                        .filter(info -> info.testNameId.equals(String.valueOf(testRun.getTest().getTestNameId())))
                        .map(info -> resolveResponsibility(testRun, info))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst()
                        .orElse(null);
    } catch (IOException e) {
      LOGGER.warn(Utils.getLogPrefix(testRun) + " Error reading suggestions file", e);
      return null;
    }
  }

  private void persistResponsibilities(@NotNull SBuild build, @NotNull List<ResponsibilityPersistentInfo> entries) {
    if (entries.isEmpty()) return;

    try {
      statisticsReporter.reportSavedSuggestions(entries.size());
      Path path = resultsFilePathResolver.get(build);

      List<ResponsibilityPersistentInfo> existing = suggestionsDao.read(path);
      if (existing.isEmpty()) statisticsReporter.reportBuildWithSuggestions();

      entries.addAll(existing);
      LOGGER.debug(String.format("Build id:%s :: Merged %d responsibilities", build.getBuildId(), entries.size()));
      suggestionsDao.write(path, entries);
    } catch (IOException ex) {
      LOGGER.warn(String.format("Build id:%s :: Error persisting suggestions", build.getBuildId()), ex);
    }
  }

  private Optional<Responsibility> resolveResponsibility(STestRun testRun, ResponsibilityPersistentInfo info) {
    if (Constants.ASSIGNEE_FILTERED_LITERAL.equals(info.investigatorId)) {
      if (TeamCityProperties.getBoolean(SHOULD_PERSIST_FILTERED_TESTS_DESCRIPTION)) {
        return Optional.of(new Responsibility(userModel.getGuestUser(),
                                              Constants.ASSIGNEE_FILTERED_DESCRIPTION_PREFIX + info.reason));
      }
      return Optional.empty();
    }

    try {
      long userId = Long.parseLong(info.investigatorId);
      User user = userModel.findUserById(userId);
      if (user == null) {
        LOGGER.warn(Utils.getLogPrefix(testRun) + " User with ID not found: " + info.investigatorId);
        return Optional.empty();
      }
      return Optional.of(new Responsibility(user, info.reason));
    } catch (NumberFormatException e) {
      LOGGER.warn(Utils.getLogPrefix(testRun) + " Invalid user ID format: " + info.investigatorId);
      return Optional.empty();
    }
  }
}
