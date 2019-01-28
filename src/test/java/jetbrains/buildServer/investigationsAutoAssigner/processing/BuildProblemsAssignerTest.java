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

package jetbrains.buildServer.investigationsAutoAssigner.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.investigationsAutoAssigner.common.DefaultUserResponsibility;
import jetbrains.buildServer.investigationsAutoAssigner.common.HeuristicResult;
import jetbrains.buildServer.investigationsAutoAssigner.common.Responsibility;
import jetbrains.buildServer.investigationsAutoAssigner.persistent.StatisticsReporter;
import jetbrains.buildServer.responsibility.BuildProblemResponsibilityFacade;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.impl.problems.BuildProblemImpl;
import jetbrains.buildServer.serverSide.problems.BuildProblem;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.UserSet;
import jetbrains.buildServer.users.impl.UserImpl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@Test
public class BuildProblemsAssignerTest extends BaseTestCase {

  private BuildProblemResponsibilityFacade myBuildProblemResponsibilityFacade;
  private BuildProblem myBuildProblem1;
  private HeuristicResult myHeuristicResult;
  private User myUser1;
  private SProject mySProject;
  private BuildProblemImpl myBuildProblem2;
  private User myUser2;
  private SBuild mySBuild;
  private BuildProblemsAssigner myBuildProblemsAssigner;

  @BeforeMethod
  @Override
  protected void setUp() {
    myBuildProblemResponsibilityFacade = Mockito.mock(BuildProblemResponsibilityFacade.class);
    myBuildProblem1 = Mockito.mock(BuildProblemImpl.class);
    when(myBuildProblem1.getId()).thenReturn(0);
    myBuildProblem2 = Mockito.mock(BuildProblemImpl.class);
    when(myBuildProblem2.getId()).thenReturn(1);

    mySProject = Mockito.mock(SProject.class);
    mySBuild = Mockito.mock(SBuild.class);
    myUser1 = Mockito.mock(UserImpl.class);
    when(myUser1.getUsername()).thenReturn("user1");
    when(myUser1.getId()).thenReturn(1L);
    myUser2 = Mockito.mock(UserImpl.class);
    when(myUser2.getUsername()).thenReturn("user2");
    when(myUser2.getId()).thenReturn(2L);
    myHeuristicResult = new HeuristicResult();


    UserSet userSetMock = Mockito.mock(UserSet.class);
    when(userSetMock.getUsers()).thenReturn(new HashSet<>(Arrays.asList(myUser1, myUser2)));
    when(mySBuild.getCommitters(any())).thenReturn(userSetMock);

    myBuildProblemsAssigner = new BuildProblemsAssigner(myBuildProblemResponsibilityFacade,
                                                        Mockito.mock(WebLinks.class),
                                                        Mockito.mock(StatisticsReporter.class));
  }

  public void Test_NoBuildProblems() {
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Collections.emptyList());
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.never()).setBuildProblemResponsibility(anyList(), any(), any());
  }

  public void Test_NoResponsibilitiesFound() {
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Collections.singletonList(myBuildProblem1));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.never()).setBuildProblemResponsibility(anyList(), any(), any());
  }

  public void Test_OneResponsibilityFound() {
    myHeuristicResult.addResponsibility(myBuildProblem1, new Responsibility(myUser1, "any description"));
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Collections.singletonList(myBuildProblem1));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.only()).setBuildProblemResponsibility(anyList(), any(), any());
  }

  public void Test_TwoSameResponsibilitiesFound() {
    myHeuristicResult.addResponsibility(myBuildProblem1, new Responsibility(myUser1, "any description"));
    myHeuristicResult.addResponsibility(myBuildProblem2, new Responsibility(myUser1, "any description"));
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Arrays.asList(myBuildProblem1, myBuildProblem2));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.only()).setBuildProblemResponsibility(anyList(), any(), any());
  }

  public void Test_TwoDifferentResponsibilitiesFound() {
    myHeuristicResult.addResponsibility(myBuildProblem1, new Responsibility(myUser1, "any description"));
    myHeuristicResult.addResponsibility(myBuildProblem2, new Responsibility(myUser2, "any description"));
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Arrays.asList(myBuildProblem1, myBuildProblem2));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.times(2)).setBuildProblemResponsibility(anyList(), any(), any());

    myHeuristicResult = new HeuristicResult();
    Mockito.clearInvocations(myBuildProblemResponsibilityFacade);
    myHeuristicResult.addResponsibility(myBuildProblem1, new Responsibility(myUser1, "any description"));
    myHeuristicResult.addResponsibility(myBuildProblem2, new Responsibility(myUser1, "any description 2"));
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Arrays.asList(myBuildProblem1, myBuildProblem2));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.times(2)).setBuildProblemResponsibility(anyList(), any(), any());

    myHeuristicResult = new HeuristicResult();
    Mockito.clearInvocations(myBuildProblemResponsibilityFacade);
    myHeuristicResult.addResponsibility(myBuildProblem1, new Responsibility(myUser1, "any description"));
    myHeuristicResult.addResponsibility(myBuildProblem2, new Responsibility(myUser2, "any description 2"));
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Arrays.asList(myBuildProblem1, myBuildProblem2));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.times(2)).setBuildProblemResponsibility(anyList(), any(), any());
  }

  public void Test_FoundNoCommitters() {
    UserSet userSetMock = Mockito.mock(UserSet.class);
    when(userSetMock.getUsers()).thenReturn(new HashSet<>(Collections.singletonList(myUser2)));
    when(mySBuild.getCommitters(any())).thenReturn(userSetMock);

    myHeuristicResult.addResponsibility(myBuildProblem1, new Responsibility(myUser1, "any description"));
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Collections.singletonList(myBuildProblem1));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.never()).setBuildProblemResponsibility(anyList(), any(), any());
  }


  public void Test_FoundNoCommittersOneDefaultResponsibility() {
    UserSet userSetMock = Mockito.mock(UserSet.class);
    when(userSetMock.getUsers()).thenReturn(new HashSet<>(Collections.singletonList(myUser2)));
    when(mySBuild.getCommitters(any())).thenReturn(userSetMock);

    myHeuristicResult.addResponsibility(myBuildProblem1, new DefaultUserResponsibility(myUser1));
    myBuildProblemsAssigner.assign(myHeuristicResult, mySProject, mySBuild, Collections.singletonList(myBuildProblem1));
    Mockito.verify(myBuildProblemResponsibilityFacade, Mockito.only()).setBuildProblemResponsibility(anyList(), any(), any());
  }
}
