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

import javax.management.*;

import static java.lang.String.format;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

public class FlakyTestDetector {
  /**
   * The JMX ObjectName's used by the Flaky Test Detector MXBean.
   */
  private static final String OBJECT_NAME = "com.jetbrains.teamcity:type=FlakyTestDetector";
  /**
   * Whether InstanceNotFoundException has been caught.
   */
  private boolean instanceNotFound = false;
  private final com.intellij.openapi.diagnostic.Logger LOGGER = com.intellij.openapi.diagnostic.Logger.getInstance(FlakyTestDetector.class.getName());

  /**
   * If Flaky Test Detector plug-in is not installed, returns false
   *
   * @param testNameId the unique name_id of the test.
   * @return whether the test specified by testNameId is flaky.
   */
  public boolean isFlaky(final long testNameId) {
    if (instanceNotFound) return false;

    final MBeanServer mBeanServer = getPlatformMBeanServer();
    try {
      return (Boolean)mBeanServer.invoke(new ObjectName(OBJECT_NAME),
                                         "isFlaky",
                                         new Long[]{testNameId},
                                         new String[]{"long"});
    } catch (final InstanceNotFoundException ignored) {
      instanceNotFound = true;
      LOGGER.warn(format("Flaky Test Detector is not available at %s", OBJECT_NAME));
    } catch (final MBeanException | ReflectionException | MalformedObjectNameException e) {
      LOGGER.warn(e);
    }
    return false;
  }
}
