

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import com.intellij.openapi.diagnostic.Logger;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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
  private static final Logger LOGGER = Logger.getInstance(FlakyTestDetector.class.getName());

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