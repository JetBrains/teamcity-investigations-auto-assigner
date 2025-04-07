

package jetbrains.buildServer.investigationsAutoAssigner.persistent;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.ServerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SuggestionsDao {
  private final Logger logger = Constants.LOGGER;
  private final ServerSettings serverSettings;
  private final Gson gson;

  public SuggestionsDao(@NotNull final ServerSettings settings) {
    this.serverSettings = settings;
    this.gson = new Gson();
  }

  public void write(@NotNull Path resultsFilePath, @NotNull List<ResponsibilityPersistentInfo> suggestions) throws IOException {
    ArtifactContent content = new ArtifactContent(serverSettings.getServerUUID(), suggestions);
    try (BufferedWriter writer = Files.newBufferedWriter(resultsFilePath, StandardCharsets.UTF_8)) {
      gson.toJson(content, writer);
    }
  }

  @NotNull
  public List<ResponsibilityPersistentInfo> read(@Nullable Path resultsFilePath) throws IOException {
    if (resultsFilePath == null || !Files.exists(resultsFilePath) || Files.size(resultsFilePath) == 0) {
      return Collections.emptyList();
    }

    try (BufferedReader reader = Files.newBufferedReader(resultsFilePath)) {
      return Optional.ofNullable(gson.fromJson(reader, ArtifactContent.class))
                     .filter(this::isValidContent)
                     .map(content -> content.suggestions)
                     .orElse(Collections.emptyList());
    } catch (Exception e) {
      logger.warn("Failed to read suggestions from file: " + resultsFilePath, e);
      return Collections.emptyList();
    }
  }

  private boolean isValidContent(@NotNull ArtifactContent content) {
    if (content.suggestions == null || content.serverUUID == null) {
      return false;
    }

    if (!serverSettings.getServerUUID().equals(content.serverUUID)) {
      logger.warn("Server UUID mismatch: expected " + serverSettings.getServerUUID() + ", got " + content.serverUUID);
      return false;
    }

    return true;
  }

  private static class ArtifactContent {
    String serverUUID;
    List<ResponsibilityPersistentInfo> suggestions;

    ArtifactContent(String serverUUID, List<ResponsibilityPersistentInfo> suggestions) {
      this.serverUUID = serverUUID;
      this.suggestions = suggestions;
    }
  }
}
