/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the interface for persistence operations related to reading and writing data
 * to a file. Implementations of this interface are responsible for handling the
 * persistence of data of type {@code T} to and from the specified file path.
 *
 * @param <T> The type of data to be read or written.
 */
public interface PersistenceDao<T> {

  /**
   * Reads data of type {@code T} from the specified file path.
   *
   * @param path The path to the file from which the data is to be read.
   * @return The data read from the file.
   * @throws IOException If an I/O error occurs while reading the file.
   */
  @NotNull
  T read(@NotNull Path path) throws IOException;

  /**
   * Writes data of type {@code T} to the specified file path.
   *
   * @param path The path to the file where the data is to be written.
   * @param data The data to be written to the file.
   * @throws IOException If an I/O error occurs while writing to the file.
   */
  void write(@NotNull Path path, @NotNull T data) throws IOException;
}
