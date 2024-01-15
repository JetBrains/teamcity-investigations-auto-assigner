

package jetbrains.buildServer.investigationsAutoAssigner.common;

import jetbrains.buildServer.users.SUser;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test
public class DefaultUserResponsibilityTest {

  public void testDescription() {
    SUser user = Mockito.mock(SUser.class);
    Responsibility responsibility = new DefaultUserResponsibility(user);

    assertTrue(responsibility.getDescription().contains("default"));
  }
}