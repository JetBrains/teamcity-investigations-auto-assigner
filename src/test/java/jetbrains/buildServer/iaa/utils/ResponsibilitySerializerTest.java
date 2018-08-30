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

import com.google.gson.JsonElement;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.iaa.common.Responsibility;
import jetbrains.buildServer.users.User;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

@Test
public class ResponsibilitySerializerTest extends BaseTestCase {
  private User myUser;
  private String testUserName = "testUserName";
  private String testDescription = "testDescription - 239";
  private final String EXPECTED_JSON =
    String.format("{\"investigator\":\"%s\",\"description\":\"%s\"}", testUserName, testDescription);

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myUser = Mockito.mock(User.class);
    when(myUser.getUsername()).thenReturn(testUserName);
  }

  public void TestSerialize() {
    Responsibility responsibility = new Responsibility(myUser, testDescription);
    JsonElement jsonElement =
      new ResponsibilitySerializer().serialize(responsibility, null, null);
    assertEquals(EXPECTED_JSON, jsonElement.toString());
  }
}
