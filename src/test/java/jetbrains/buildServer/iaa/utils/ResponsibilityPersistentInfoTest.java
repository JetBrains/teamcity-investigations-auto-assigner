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

import jetbrains.buildServer.BaseTestCase;
import org.testng.annotations.Test;

@Test
public class ResponsibilityPersistentInfoTest extends BaseTestCase {
  public void TestFields() {
    int testRunId = 239;
    long testInvestigatorId = 239L;
    String testDescription = "testDescription";
    ResponsibilityPersistentInfo rp = new ResponsibilityPersistentInfo(testRunId, testInvestigatorId, testDescription);
    assertEquals(testRunId, rp.testRunId);
    assertEquals(testInvestigatorId, rp.investigatorId);
    assertEquals(testDescription, rp.description);
  }
}