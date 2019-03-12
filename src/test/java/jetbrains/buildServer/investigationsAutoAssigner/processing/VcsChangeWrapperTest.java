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

import java.util.Arrays;
import java.util.Collections;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.vcs.SVcsModification;
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

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    VcsChangeWrapperFactory vcsChangeWrapperFactory = new VcsChangeWrapperFactory();
    
    String firstUserUsername = "myFirstUser";
    myFirstUser = Mockito.mock(UserEx.class);
    myMod = Mockito.mock(SVcsModification.class);
    myWrappedVcsChange = vcsChangeWrapperFactory.wrap(myMod);

    when(myFirstUser.getUsername()).thenReturn(firstUserUsername);
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));

    String secondUserUsername = "mySecondUser";
    mySecondUser = Mockito.mock(UserEx.class);
    when(mySecondUser.getUsername()).thenReturn(secondUserUsername);

  }

  public void TestWithOneResponsible() {
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));
    User user = myWrappedVcsChange.getOnlyCommitter(Collections.emptyList());

    Assert.assertNotNull(user);
    Assert.assertEquals(user, myFirstUser);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void TestUnknownUser() {
    when(myMod.getCommitters()).thenReturn(Collections.emptyList());
    myWrappedVcsChange.getOnlyCommitter(Collections.emptyList());
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void TestWithManyResponsible() {
    when(myMod.getCommitters()).thenReturn(Arrays.asList(myFirstUser, mySecondUser));
    myWrappedVcsChange.getOnlyCommitter(Collections.emptyList());
  }

  public void TestUsersToIgnore() {
    when(myMod.getCommitters()).thenReturn(Collections.singletonList(myFirstUser));
    User user = myWrappedVcsChange.getOnlyCommitter(Collections.singletonList(myFirstUser.getUsername()));

    Assert.assertNull(user);
  }
}
