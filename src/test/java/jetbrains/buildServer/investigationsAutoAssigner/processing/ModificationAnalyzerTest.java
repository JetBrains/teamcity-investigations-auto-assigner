

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import com.intellij.openapi.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicNotApplicableException;
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
public class ModificationAnalyzerTest extends BaseTestCase {

  private UserEx myFirstUser;
  private UserEx mySecondUser;
  private SVcsModification myMod;
  private ModificationAnalyzerFactory.ModificationAnalyzer myWrappedVcsChange;
  private final String myFilePath =  "./path1/path1/path1/filename";

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

    ModificationAnalyzerFactory modificationAnalyzerFactory = new ModificationAnalyzerFactory();
    myWrappedVcsChange = modificationAnalyzerFactory.getInstance(myMod);
  }

  public void TestGetOnlyCommitter_OneResponsible() {
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));
    User user = myWrappedVcsChange.getOnlyCommitter(Collections.emptySet());

    Assert.assertNotNull(user);
    Assert.assertEquals(user, myFirstUser);
  }

  public void TestBrokenFile_CorrectCase() {
    String problematicText = "I contain " + myFilePath;
    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptySet());
    Assert.assertNotNull(result);
    Assert.assertEquals(result.first, myFirstUser);
    Assert.assertEquals(result.second, myFilePath);
  }

  public void TestBrokenFile_GitIgnoreCase() {
    String problematicText = "I contain ./no/file/here";
    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn(".gitignore");

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptySet());

    Assert.assertNull(result);
  }

  @Test(expectedExceptions = HeuristicNotApplicableException.class)
  public void TestBrokenFile_ManyCommitters() {
    String problematicText = "I contain " + myFilePath;

    when(myMod.getCommitters()).thenReturn(Arrays.asList(myFirstUser, mySecondUser));

    myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptySet());
  }

  public void TestBrokenFile_UsersToIgnore() {
    String problematicText = "I contain " + myFilePath;

    when(myMod.getCommitters()).thenReturn(Arrays.asList(myFirstUser, mySecondUser));

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.singleton(mySecondUser.getUsername()));

    Assert.assertNotNull(result);
    Assert.assertEquals(result.first, myFirstUser);
    Assert.assertEquals(result.second, "./path1/path1/path1/filename");
  }

  public void TestBrokenFile_SmallFilePaths() {
    String problematicText = "I contain any/other/build/path";

    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("build.gradle");

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptySet());
    Assert.assertNull(result);
  }

  public void TestBrokenFile_SmallFilePaths2() {
    String problematicText = "I contain ./any/hmbrm/build.gradle";

    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("build.gradle");

    Pair<User, String> result = myWrappedVcsChange.findProblematicFile(problematicText, Collections.emptySet());
    Assert.assertNotNull(result);
  }

  @Test(expectedExceptions = HeuristicNotApplicableException.class)
  public void TestGetOnlyCommitter_UnknownVcsUsername() {
    when(myMod.getCommitters()).thenReturn(Collections.emptyList());
    myWrappedVcsChange.getOnlyCommitter(Collections.emptySet());
  }

  @Test(expectedExceptions = HeuristicNotApplicableException.class)
  public void TestBrokenFile_UnknownVcsUsername() {
    when(myMod.getCommitters()).thenReturn(Collections.emptyList());
    VcsFileModification mod = Mockito.mock(VcsFileModification.class);
    when(myMod.getChanges()).thenReturn(Collections.singletonList(mod));
    when(mod.getRelativeFileName()).thenReturn("file.path");
    myWrappedVcsChange.findProblematicFile("any file.path", Collections.emptySet());
  }

  @Test(expectedExceptions = HeuristicNotApplicableException.class)
  public void TestGetOnlyCommitter_ManyResponsible() {
    when(myMod.getCommitters()).thenReturn(Arrays.asList(myFirstUser, mySecondUser));
    myWrappedVcsChange.getOnlyCommitter(Collections.emptySet());
  }

  public void TestGetOnlyCommitter_UsersToIgnore() {
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));
    User user = myWrappedVcsChange.getOnlyCommitter(Collections.singleton(myFirstUser.getUsername()));

    Assert.assertNull(user);
  }
}