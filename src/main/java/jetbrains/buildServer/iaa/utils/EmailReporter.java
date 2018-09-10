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

import com.intellij.openapi.diagnostic.Logger;
import java.util.List;
import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.util.EmailException;
import jetbrains.buildServer.util.EmailSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmailReporter {

  private static final Logger LOGGER = Logger.getInstance(EmailReporter.class.getName());
  @NotNull private final EmailSender myEmailSender;
  @Nullable private final String mySupervisorEmail;
  private final RelativeWebLinks myWebLinks = new RelativeWebLinks();
  @NotNull private final RootUrlHolder myRootUrlHolder;

  public EmailReporter(@NotNull EmailSender emailSender, @NotNull RootUrlHolder rootUrlHolder) {
    myEmailSender = emailSender;
    mySupervisorEmail = CustomParameters.getEmailForEmailReporter();
    myRootUrlHolder = rootUrlHolder;
  }

  public void sendResults(SBuild sBuild, List<Responsibility> responsibilities) {
    if (mySupervisorEmail != null && responsibilities.size() > 0) {
      String title = String.format("Investigation auto-assigner report for build #%s", sBuild.getBuildId());
      trySendEmail(mySupervisorEmail, title, generateHtmlReport(sBuild, responsibilities));
    }
  }

  private void trySendEmail(@NotNull String to, String title, String html) {
    try {
      myEmailSender.send(to, title, "", html);
    } catch (EmailException ex) {
      LOGGER.error(ex);
    }
  }

  @NotNull
  private String generateHtmlReport(final SBuild sBuild, final List<Responsibility> responsibilities) {
    StringBuilder htmlBuilder = new StringBuilder(
      String.format("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body>\n" +
                    "<h2>Report for <a href=\"%s\">%s#%s</a>. Found %s investigations:</h2>\n" +
                    "<ol>",
                    myRootUrlHolder.getRootUrl() + myWebLinks.getViewResultsUrl(sBuild),
                    sBuild.getBuildTypeName(),
                    sBuild.getBuildId(),
                    responsibilities.size()));
    for (Responsibility responsibility : responsibilities) {
      htmlBuilder.append(String.format("<li>Investigation was assigned to %s who %s.</li>\n",
                                       responsibility.getUser().getUsername(),
                                       responsibility.getDescription()));
    }
    htmlBuilder.append("</ol>\n" +
                       "</body>\n" +
                       "</html>");
    return htmlBuilder.toString();
  }
}
