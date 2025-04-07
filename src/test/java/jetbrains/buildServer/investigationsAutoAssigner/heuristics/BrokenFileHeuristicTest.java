

package jetbrains.buildServer.investigationsAutoAssigner.heuristics;

import com.intellij.openapi.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicNotApplicableException;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.processing.HeuristicContext;
import jetbrains.buildServer.investigationsAutoAssigner.processing.ModificationAnalyzerFactory;
import jetbrains.buildServer.investigationsAutoAssigner.utils.ProblemTextExtractor;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SelectPrevBuildPolicy;
import jetbrains.buildServer.vcs.VcsFileModification;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Test
public class BrokenFileHeuristicTest extends BaseTestCase {

  private BrokenFileHeuristic myHeuristic;
  private SUser myUser;
  private SUser mySecondUser;
  private BuildPromotionEx myBuildPromotion;
  private HeuristicContext myHeuristicContext;
  private STestRun mySTestRun;
  private ProblemTextExtractor myProblemTextExtractor;
  private ModificationAnalyzerFactory.ModificationAnalyzer myFirstVcsChangeWrapped;
  private ModificationAnalyzerFactory.ModificationAnalyzer mySecondVcsChangeWrapped;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myProblemTextExtractor = Mockito.mock(ProblemTextExtractor.class);
    ModificationAnalyzerFactory modificationAnalyzerFactory = Mockito.mock(ModificationAnalyzerFactory.class);
    myHeuristic = new BrokenFileHeuristic(myProblemTextExtractor, modificationAnalyzerFactory);

    SBuild build = Mockito.mock(SBuild.class);
    SProject project = Mockito.mock(SProject.class);
    myUser = Mockito.mock(SUser.class);
    when(myUser.getUsername()).thenReturn("myUser1");
    mySecondUser = Mockito.mock(SUser.class);
    when(mySecondUser.getUsername()).thenReturn("myUser2");

    mySTestRun = Mockito.mock(STestRun.class);
    myHeuristicContext = new HeuristicContext(build, project, Collections.emptyList(),
                                              Collections.singletonList(mySTestRun), Collections.emptySet());

    myBuildPromotion = Mockito.mock(BuildPromotionEx.class);
    when(build.getBuildPromotion()).thenReturn(myBuildPromotion);

    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn("problem includes ./path/to/file");

    setupMockedVcsChanges(modificationAnalyzerFactory);
  }

  private void setupMockedVcsChanges(ModificationAnalyzerFactory factory) {
    ChangeDescriptor descriptor1 = Mockito.mock(ChangeDescriptor.class);
    ChangeDescriptor descriptor2 = Mockito.mock(ChangeDescriptor.class);
    SVcsModification vcs1 = Mockito.mock(SVcsModification.class);
    SVcsModification vcs2 = Mockito.mock(SVcsModification.class);

    when(descriptor1.getRelatedVcsChange()).thenReturn(vcs1);
    when(descriptor2.getRelatedVcsChange()).thenReturn(vcs2);
    when(myBuildPromotion.getDetectedChanges(eq(SelectPrevBuildPolicy.SINCE_LAST_BUILD), anyBoolean()))
      .thenReturn(Arrays.asList(descriptor1, descriptor2));

    myFirstVcsChangeWrapped = Mockito.mock(ModificationAnalyzerFactory.ModificationAnalyzer.class);
    mySecondVcsChangeWrapped = Mockito.mock(ModificationAnalyzerFactory.ModificationAnalyzer.class);

    when(factory.getInstance(vcs1)).thenReturn(myFirstVcsChangeWrapped);
    when(factory.getInstance(vcs2)).thenReturn(mySecondVcsChangeWrapped);
  }

  public void testNoDetectedChangesReturnsEmpty() {
    when(myBuildPromotion.getDetectedChanges(any(), anyBoolean())).thenReturn(Collections.emptyList());
    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty());
  }

  public void testNoRelatedVcsChangeReturnsEmpty() {
    ChangeDescriptor descriptor = Mockito.mock(ChangeDescriptor.class);
    when(descriptor.getRelatedVcsChange()).thenReturn(null);
    when(myBuildPromotion.getDetectedChanges(any(), anyBoolean())).thenReturn(Collections.singletonList(descriptor));
    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty());
  }

  public void testHeuristicNotApplicableReturnsEmpty() {
    when(myFirstVcsChangeWrapped.findProblematicFile(any(), anySet()))
      .thenThrow(new HeuristicNotApplicableException("invalid"));

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty());
  }

  public void testSingleResponsibleUserAssignedCorrectly() {
    String filePath = "./path/to/file";
    String problemText = "problem includes " + filePath;

    Pair<User, String> match = Pair.create(myUser, filePath);
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn(problemText);
    when(myFirstVcsChangeWrapped.findProblematicFile(problemText, Collections.emptySet())).thenReturn(match);
    when(mySecondVcsChangeWrapped.findProblematicFile(problemText, Collections.emptySet())).thenReturn(null);

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Responsibility responsibility = result.getResponsibility(mySTestRun);

    Assert.assertNotNull(responsibility);
    Assert.assertEquals(responsibility.getUser(), myUser);
  }

  public void testMultipleDifferentUsersLeadsToEmptyResult() {
    String problemText = "problem includes multiple files";
    when(myProblemTextExtractor.getBuildProblemText(any())).thenReturn(problemText);

    Pair<User, String> result1 = Pair.create(myUser, "./fileA");
    Pair<User, String> result2 = Pair.create(mySecondUser, "./fileB");

    when(myFirstVcsChangeWrapped.findProblematicFile(problemText, Collections.emptySet())).thenReturn(result1);
    when(mySecondVcsChangeWrapped.findProblematicFile(problemText, Collections.emptySet())).thenReturn(result2);

    HeuristicResult result = myHeuristic.findResponsibleUser(myHeuristicContext);
    Assert.assertTrue(result.isEmpty());
  }
}
