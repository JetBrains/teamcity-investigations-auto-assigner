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

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.serverSide.DataItem;
import jetbrains.buildServer.serverSide.ProjectDataFetcher;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.UserModel;
import jetbrains.buildServer.users.UserModelEx;
import jetbrains.buildServer.util.browser.Browser;
import org.jetbrains.annotations.NotNull;

public class UserListFetcher implements ProjectDataFetcher {

  private final UserModel myUserModel;

  public UserListFetcher(final UserModelEx userModel) {
    myUserModel = userModel;
  }

  @NotNull
  @Override
  public List<DataItem> retrieveData(@NotNull final Browser fsBrowser, @NotNull final String projectFilePath) {
    List<DataItem> dataItems = new ArrayList<>();
    for (SUser user : myUserModel.getAllUsers().getUsers()) {
      dataItems.add(new DataItem(String.valueOf(user.getUsername()), user.getName()));
    }

    return dataItems;
  }

  @NotNull
  @Override
  public String getType() {
    return "UserList";
  }
}
