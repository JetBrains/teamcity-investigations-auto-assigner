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

package jetbrains.buildServer.iaa;

import java.util.HashMap;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.STest;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestProblemInfo extends ProblemInfo {
  @NotNull private final STest mySTest;
  private final HashMap<String, User> myTestId2Responsible;

  TestProblemInfo(@NotNull final STest test,
                  @NotNull final SBuild sBuild,
                  @NotNull final SProject project,
                  @Nullable final String problemText,
                  final HashMap<String, User> testId2Responsible) {
    super(sBuild, project, problemText);
    mySTest = test;
    myTestId2Responsible = testId2Responsible;
  }

  @NotNull
  public STest getSTest() {
    return mySTest;
  }

  public HashMap<String, User> getTestId2Responsible() {
    return myTestId2Responsible;
  }
}
