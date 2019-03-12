/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class VcsChangeWrapperTest extends BaseTestCase {

  private UserEx myFirstUser;
  private UserEx mySecondUser;
  private SVcsModification myMod;
  private VcsChangeWrapperFactory.VcsChangeWrapper myWrappedVcsChange;
  private String myFilePath =  "./path1/path1/path1/filename";

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    String firstUserUsername = "myFirstUser";
    myFirstUser = Mockito.mock(UserEx.class);
    when(myFirstUser.getUsername()).thenReturn(firstUserUsername);

    String secondUserUsername = "mySecondUser";
    mySecondUser = Mockito.mock(UserEx.class);
    when(mySecondUser.getUsername()).thenReturn(secondUserUsername);

    VcsFileModification changeMod = Mockito.mock(VcsFileModification.class);
    when(changeMod.getRelativeFileName()).thenReturn(myFilePath);
    myMod = Mockito.mock(SVcsModification.class);
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));
    when(myMod.getChanges()).thenReturn(Collections.singletonList(changeMod));

    VcsChangeWrapperFactory vcsChangeWrapperFactory = new VcsChangeWrapperFactory();
    myWrappedVcsChange = vcsChangeWrapperFactory.wrap(myMod);
  }

  public void TestGetOnlyCommitter_OneResponsible() {
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));
    User user = myWrappedVcsChange.getOnlyCommitter(Collections.emptyList());

    Assert.assertNotNull(user);
    Assert.assertEquals(user, myFirstUser);
  }

  public void TestBrokenFile_CorrectCase() {
    String problematicText = "I contain " + myFilePath;
    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptyList());
    Assert.assertNotNull(result);
    Assert.assertEquals(result.first, myFirstUser);
    Assert.assertEquals(result.second, myFilePath);
  }

  public void TestBrokenFile_GitIgnoreCase() {
    String problematicText = "I contain ./no/file/here";
    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn(".gitignore");

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptyList());

    Assert.assertNull(result);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void TestBrokenFile_ManyCommitters() {
    String problematicText = "I contain " + myFilePath;

    when(myMod.getCommitters()).thenReturn(Arrays.asList(myFirstUser, mySecondUser));

    myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptyList());
  }

  public void TestBrokenFile_UsersToIgnore() {
    String problematicText = "I contain " + myFilePath;

    when(myMod.getCommitters()).thenReturn(Arrays.asList(myFirstUser, mySecondUser));

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.singletonList(mySecondUser.getUsername()));

    Assert.assertNotNull(result);
    Assert.assertEquals(result.first, myFirstUser);
    Assert.assertEquals(result.second, "./path1/path1/path1/filename");
  }

  public void TestBrokenFile_SmallFilePaths() {
    String problematicText = "I contain any/other/build/path";

    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("build.gradle");

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptyList());
    Assert.assertNull(result);
  }

  public void TestBrokenFile_SmallFilePaths2() {
    String problematicText = "I contain ./any/hmbrm/build.gradle";

    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("build.gradle");

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptyList());
    Assert.assertNotNull(result);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void TestGetOnlyCommitter_UnknownVcsUsername() {
    when(myMod.getCommitters()).thenReturn(Collections.emptyList());
    myWrappedVcsChange.getOnlyCommitter(Collections.emptyList());
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void TestBrokenFile_UnknownVcsUsername() {
    when(myMod.getCommitters()).thenReturn(Collections.emptyList());
    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("file.path");
    myWrappedVcsChange.findProblematicFile("any file.path", Collections.emptyList());
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void TestGetOnlyCommitter_ManyResponsible() {
    when(myMod.getCommitters()).thenReturn(Arrays.asList(myFirstUser, mySecondUser));
    myWrappedVcsChange.getOnlyCommitter(Collections.emptyList());
  }

  public void TestGetOnlyCommitter_UsersToIgnore() {
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));
    User user = myWrappedVcsChange.getOnlyCommitter(Collections.singletonList(myFirstUser.getUsername()));

    Assert.assertNull(user);
  }
}
