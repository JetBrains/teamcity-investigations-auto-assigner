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

import java.util.Arrays;
import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.EmailSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class EmailReporterTest extends BaseTestCase {

  private EmailReporter myEmailReporter;
  private EmailSenderMock myMockedEmailSender;
  private SBuild mySBuildMock;
  private Responsibility myResponsibility1;
  private Responsibility myResponsibility2;
  private CustomParameters myCustomParameters;
  private WebLinks myWebLinks;
  private String TEST_LINK_URL = "testLinkUrl.com";
  private Long BUILD_ID = 239L;
  private String BUILD_TYPE_NAME = "testBuildTypeName";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myMockedEmailSender = new EmailSenderMock();
    myWebLinks = Mockito.mock(WebLinks.class);
    mySBuildMock = Mockito.mock(SBuild.class);
    myCustomParameters = Mockito.mock(CustomParameters.class);
    final User user1 = Mockito.mock(User.class);
    final User user2 = Mockito.mock(User.class);
    myResponsibility1 = new Responsibility(user1, "testDescription");
    myResponsibility2 = new Responsibility(user2, "testDescription2");

    when(user1.getUsername()).thenReturn("testUser1");
    when(user2.getUsername()).thenReturn("testUser2");
    when(mySBuildMock.getBuildId()).thenReturn(BUILD_ID);
    when(mySBuildMock.getBuildTypeName()).thenReturn(BUILD_TYPE_NAME);
    String testEmail = "test.mail.com";
    when(myCustomParameters.getEmailForEmailReporter()).thenReturn(testEmail);
    when(myWebLinks.getViewResultsUrl(mySBuildMock)).thenReturn(TEST_LINK_URL);
    myEmailReporter = new EmailReporter(myMockedEmailSender, myWebLinks, myCustomParameters);
  }

  public void TestEmailAddressAbsents() {
    when(myCustomParameters.getEmailForEmailReporter()).thenReturn(null);

    EmailReporter emailReporter = new EmailReporter(myMockedEmailSender, myWebLinks, myCustomParameters);
    emailReporter.sendResults(mySBuildMock, Arrays.asList(myResponsibility1, myResponsibility2));

    assertFalse(myMockedEmailSender.called);
  }

  public void TestNoResponsibilities() {
    myEmailReporter.sendResults(mySBuildMock, Collections.emptyList());

    assertFalse(myMockedEmailSender.called);
  }

  public void TestEmailAddress() {
    String testEmail = "test.mail.com";
    when(myCustomParameters.getEmailForEmailReporter()).thenReturn(testEmail);

    EmailReporter emailReporter = new EmailReporter(myMockedEmailSender, myWebLinks, myCustomParameters);
    emailReporter.sendResults(mySBuildMock, Arrays.asList(myResponsibility1, myResponsibility2));

    assertTrue(myMockedEmailSender.called);
    assertEquals(testEmail, myMockedEmailSender.usedAddress);
  }

  public void TestTopicContainsBuildLink() {
    EmailReporter emailReporter = new EmailReporter(myMockedEmailSender, myWebLinks, myCustomParameters);
    emailReporter.sendResults(mySBuildMock, Arrays.asList(myResponsibility1, myResponsibility2));

    assertTrue(myMockedEmailSender.called);
    assertTrue(myMockedEmailSender.usedSubject.contains(mySBuildMock.getBuildId() + ""));
  }

  public void TestCompareWithGold() {
    EmailReporter emailReporter = new EmailReporter(myMockedEmailSender, myWebLinks, myCustomParameters);
    emailReporter.sendResults(mySBuildMock, Arrays.asList(myResponsibility1, myResponsibility2));

    assertTrue(myMockedEmailSender.called);
    assertEquals(getHtmlReportGold(), myMockedEmailSender.usedHtml);
  }

  private String getHtmlReportGold() {
    return String.format("<!DOCTYPE html>\n" +
                         "<html>\n" +
                         "<body>\n" +
                         "<h2>Report for <a href=\"%s\">%s#%s</a>. Found 2 investigations:</h2>\n" +
                         "<ol><li>Investigation was assigned to %s who %s.</li>\n" +
                         "<li>Investigation was assigned to %s who %s.</li>\n" +
                         "</ol>\n" +
                         "</body>\n" +
                         "</html>",
                         TEST_LINK_URL,
                         BUILD_TYPE_NAME,
                         BUILD_ID,
                         myResponsibility1.getUser().getUsername(),
                         myResponsibility1.getDescription(),
                         myResponsibility2.getUser().getUsername(),
                         myResponsibility2.getDescription());
  }
}

class EmailSenderMock implements EmailSender {

  boolean called = false;
  String usedAddress;
  String usedSubject;
  String usedHtml;

  @Override
  public void send(@NotNull final String address,
                   @NotNull final String subject,
                   @NotNull final String plainText,
                   @Nullable final String html) {
    called = true;
    usedAddress = address;
    usedSubject = subject;
    usedHtml = html;
  }
}

