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
import jetbrains.buildServer.investigationsAutoAssigner.common.Constants;
import jetbrains.buildServer.serverSide.ServerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles the persistence of investigation suggestions by reading and writing them from/to a file.
 * This class utilizes Gson to serialize and deserialize the suggestions to/from JSON format.
 * It ensures that suggestions are associated with the correct server based on the server UUID.
 * <p>
 * The class implements the {@link PersistenceDao} interface for storing and retrieving a list of
 * {@link ResponsibilityPersistentInfo}.
 * </p>
 */
public class SuggestionsDao implements PersistenceDao<List<ResponsibilityPersistentInfo>> {
  private final Logger LOGGER = Constants.LOGGER;
  private final ServerSettings settings;
  private final Gson gson;

  /**
   * Constructs a new {@link SuggestionsDao} instance with the given server settings.
   *
   * @param settings The server settings used to retrieve the server UUID.
   */
  public SuggestionsDao(@NotNull final ServerSettings settings) {
    this.settings = settings;
    this.gson = new Gson();
  }

  /**
   * Writes the given list of {@link ResponsibilityPersistentInfo} to the specified file.
   * The data is serialized to JSON format and saved along with the server's UUID.
   *
   * @param resultsFilePath The path to the file where the suggestions should be written.
   * @param infoToAdd The list of suggestions to be written to the file.
   * @throws IOException If an I/O error occurs while writing the file.
   */
  public void write(Path resultsFilePath, List<ResponsibilityPersistentInfo> infoToAdd) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(resultsFilePath, StandardCharsets.UTF_8)) {
      ArtifactContent artifactContent = new ArtifactContent(this.settings.getServerUUID(), infoToAdd);
      this.gson.toJson(artifactContent, writer);
    }
  }

  /**
   * Reads the list of {@link ResponsibilityPersistentInfo} from the specified file.
   * The data is deserialized from JSON format and the server's UUID is validated.
   * If the file doesn't exist, is empty, or contains invalid data, an empty list is returned.
   *
   * @param resultsFilePath The path to the file from which the suggestions should be read.
   * @return A list of {@link ResponsibilityPersistentInfo} read from the file, or an empty list if the file is invalid.
   * @throws IOException If an I/O error occurs while reading the file.
   */
  @NotNull
  public List<ResponsibilityPersistentInfo> read(@Nullable Path resultsFilePath) throws IOException {
    if (resultsFilePath != null && Files.exists(resultsFilePath) && Files.size(resultsFilePath) != 0) {
      try (BufferedReader reader = Files.newBufferedReader(resultsFilePath)) {
        ArtifactContent artifactContent = this.gson.fromJson(reader, ArtifactContent.class);
        if (artifactContent == null || artifactContent.suggestions == null) {
          return Collections.emptyList();
        } else if (artifactContent.serverUUID == null ||
                   !artifactContent.serverUUID.equals(this.settings.getServerUUID())) {
          LOGGER.warn("%s: Server UUIDs don't match");
          return Collections.emptyList();
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Read %s stored investigations", artifactContent.suggestions.size()));
          }
          return artifactContent.suggestions;
        }
      }
    }
    return Collections.emptyList();
  }

  /**
   * Represents the structure of the artifact content that contains the server UUID and the list of suggestions.
   * This class is used for serializing and deserializing the suggestions to/from JSON format.
   */
  private static class ArtifactContent {
    String serverUUID;
    List<ResponsibilityPersistentInfo> suggestions;

    /**
     * Constructs an {@link ArtifactContent} instance with the given server UUID and list of suggestions.
     *
     * @param serverUUID The server UUID associated with the suggestions.
     * @param suggestions The list of suggestions to be serialized or deserialized.
     */
    private ArtifactContent(String serverUUID, List<ResponsibilityPersistentInfo> suggestions) {
      this.serverUUID = serverUUID;
      this.suggestions = suggestions;
    }
  }
}
