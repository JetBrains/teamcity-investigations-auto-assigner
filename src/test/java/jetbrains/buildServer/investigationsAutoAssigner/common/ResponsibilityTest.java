/*
 * Copyright 2000-2025 JetBrains s.r.o.
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

package jetbrains.buildServer.investigationsAutoAssigner.common;

import jetbrains.buildServer.users.User;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

@Test
public class ResponsibilityTest {

  private User myUser;
  private final String myDescription = "myDescriptionTest";
  private Responsibility myResponsibility;

  @BeforeMethod
  public void setUp() {
    myUser = Mockito.mock(User.class);
    when(myUser.getDescriptiveName()).thenReturn("myUserDescriptiveName");
    when(myUser.getId()).thenReturn((long)1);
    myResponsibility = new Responsibility(myUser, myDescription);
  }

  @Test
  public void testFieldGetters() {
    assertEquals(myResponsibility.getUser(), myUser);
    assertEquals(myResponsibility.getDescription(), myDescription);
  }

  @Test
  public void testGetAssignDescription() {
    String linkToBuild = "test/link/to/build";
    String expectedResult = String.format("%s %s who %s (initial build: %s).",
                                          Constants.ASSIGN_DESCRIPTION_PREFIX,
                                          myUser.getDescriptiveName(),
                                          myDescription,
                                          linkToBuild);
    assertEquals(myResponsibility.getAssignDescription(linkToBuild), expectedResult);
  }

  @Test
  public void testEquals() {
    Responsibility newResponsibility = new Responsibility(myUser, myDescription);
    assertTrue(newResponsibility.equals(myResponsibility));
  }

  @Test
  public void testHashCode() {
    Responsibility newResponsibility = new Responsibility(myUser, myDescription);
    assertEquals(newResponsibility.hashCode(), myResponsibility.hashCode());

    User newUser = Mockito.mock(User.class);
    when(newUser.getDescriptiveName()).thenReturn("newUserDescriptiveName");
    when(newUser.getId()).thenReturn((long)2);
    Responsibility differentResponsibility = new Responsibility(newUser, myDescription);

    assertNotEquals(newResponsibility.hashCode(), differentResponsibility.hashCode());
    assertNotEquals(myResponsibility.hashCode(), differentResponsibility.hashCode());
  }
}
