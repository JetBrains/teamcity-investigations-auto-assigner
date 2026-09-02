

package jetbrains.buildServer.investigationsAutoAssigner.utils;

import jetbrains.buildServer.web.functions.flakyTestDetector.FlakyTestDetectorFunctions;

public class FlakyTestDetector {

  /**
   * If Flaky Test Detector plug-in is not installed, returns false
   *
   * @param testNameId the unique name_id of the test.
   * @return whether the test specified by testNameId is flaky.
   */
  public boolean isFlaky(final long testNameId) {
    return FlakyTestDetectorFunctions.isFlaky(testNameId);
  }
}