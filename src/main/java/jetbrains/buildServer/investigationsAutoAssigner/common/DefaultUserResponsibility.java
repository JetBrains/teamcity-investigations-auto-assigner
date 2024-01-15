

package jetbrains.buildServer.investigationsAutoAssigner.common;

import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;

public class DefaultUserResponsibility extends Responsibility {
  private static final String DEFAULT_DESCRIPTION = "was the default responsible for the builds";

  public DefaultUserResponsibility(@NotNull final User user) {
    super(user, DEFAULT_DESCRIPTION);
  }
}