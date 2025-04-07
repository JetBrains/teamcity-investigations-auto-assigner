

package jetbrains.buildServer.investigationsAutoAssigner.common;

package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.Objects;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;

public class Responsibility {
  private final User user;
  private final String description;

  public Responsibility(@NotNull User user, @NotNull String description) {
    this.user = user;
    this.description = description;
  }

  @NotNull
  public User getUser() {
    return user;
  }

  @NotNull
  public String getDescription() {
    return description;
  }

  public String getAssignDescription(String linkToBuild) {
    return String.format("%s %s who %s (initial build: %s).",
                         Constants.ASSIGN_DESCRIPTION_PREFIX,
                         user.getDescriptiveName(),
                         description,
                         linkToBuild);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Responsibility that = (Responsibility) o;
    return user.getId() == that.user.getId() && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user.getId(), description);
  }
}