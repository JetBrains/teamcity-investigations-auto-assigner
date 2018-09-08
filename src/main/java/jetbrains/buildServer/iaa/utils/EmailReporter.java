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

import java.util.List;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.util.EmailSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmailReporter {

  @NotNull private final EmailSender myEmailSender;
  @Nullable private final String mySupervisorEmail;
  private final RelativeWebLinks myWebLinks = new RelativeWebLinks();

  public EmailReporter(@NotNull EmailSender emailSender) {
    myEmailSender = emailSender;
    mySupervisorEmail = CustomParameters.getEmailForEmailReporter();
  }

  public void sendResults(SBuild sBuild, List<Responsibility> responsibilities) {
    if (mySupervisorEmail != null && responsibilities.size() > 0) {
      String title = String.format("Investigation auto-assigner report for build #%s", sBuild.getBuildId());
      myEmailSender.send(mySupervisorEmail, generateReport(sBuild, responsibilities), title, null);
    }
  }

  @NotNull
  private String generateReport(final SBuild sBuild, final List<Responsibility> responsibilities) {
    StringBuilder sb = new StringBuilder(String.format("Report for %s#%s: found %s investigations.\n",
                                                       sBuild.getBuildTypeName(),
                                                       sBuild.getBuildId(),
                                                       responsibilities.size()));
    for (int i = 0; i < responsibilities.size(); i++) {
      Responsibility responsibility = responsibilities.get(i);
      sb.append(String.format("%s. Investigation was assigned to %s who %s.\n",
                              i,
                              responsibility.getUser().getUsername(),
                              responsibility.getDescription()));
    }

    sb.append("Link: ").append(myWebLinks.getViewResultsUrl(sBuild));
    return sb.toString();
  }
}
