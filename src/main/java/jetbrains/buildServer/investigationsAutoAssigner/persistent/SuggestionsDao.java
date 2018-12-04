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
import jetbrains.buildServer.serverSide.ServerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SuggestionsDao {
  private final Logger LOGGER = Logger.getInstance(SuggestionsDao.class.getName());
  private final ServerSettings mySettings;
  private final Gson myGson;

  public SuggestionsDao(@NotNull final ServerSettings settings) {
    mySettings = settings;
    myGson = new Gson();
  }

  public void write(Path resultsFilePath, List<ResponsibilityPersistentInfo> infoToAdd) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(resultsFilePath, StandardCharsets.UTF_8)) {
      ArtifactContent artifactContent = new ArtifactContent(mySettings.getServerUUID(), infoToAdd);
      myGson.toJson(artifactContent, writer);
    }
  }

  @NotNull
  public List<ResponsibilityPersistentInfo> read(@Nullable Path resultsFilePath) throws IOException {

    if (resultsFilePath != null && Files.exists(resultsFilePath) && Files.size(resultsFilePath) != 0) {
      try (BufferedReader reader = Files.newBufferedReader(resultsFilePath)) {
        ArtifactContent artifactContent = myGson.fromJson(reader, ArtifactContent.class);
        if (artifactContent == null || artifactContent.suggestions == null) {
          return Collections.emptyList();
        } else if (artifactContent.serverUUID == null ||
                   !artifactContent.serverUUID.equals(mySettings.getServerUUID())) {
          LOGGER.warn("%s: Server UUIDs don't match");
          return Collections.emptyList();
        } else {
          LOGGER.debug(String.format("Read %s stored investigations", artifactContent.suggestions.size()));
          return artifactContent.suggestions;
        }
      }
    }

    return Collections.emptyList();
  }

  private class ArtifactContent {
    String serverUUID;
    List<ResponsibilityPersistentInfo> suggestions;

    private ArtifactContent(String serverUUID, List<ResponsibilityPersistentInfo> suggestions) {
      this.serverUUID = serverUUID;
      this.suggestions = suggestions;
    }
  }
}

