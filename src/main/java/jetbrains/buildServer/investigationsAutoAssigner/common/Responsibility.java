

package jetbrains.buildServer.investigationsAutoAssigner.common;

import java.util.Arrays;
import jetbrains.buildServer.users.User;
import org.jetbrains.annotations.NotNull;

public class Responsibility {
  private final User myUser;
  private final String myDescription;

  public Responsibility(@NotNull User user, @NotNull String description) {
    myUser = user;
    myDescription = description;
  }

  @NotNull
  public User getUser() {
    return myUser;
  }

  @NotNull
  public String getDescription() {
    return myDescription;
  }

  public String getAssignDescription(String linkToBuild) {
    return String.format("%s %s who %s (initial build: %s).",
                         Constants.ASSIGN_DESCRIPTION_PREFIX,
                         myUser.getDescriptiveName(),
                         myDescription,
                         linkToBuild);
  }

  @Override
  public boolean equals(final Object another) {
    if (!(another instanceof Responsibility)) {
      return false;
    }

    Responsibility anotherResponsibility = (Responsibility)another;
    return myUser.getId() == anotherResponsibility.getUser().getId() &&
           myDescription.equals(anotherResponsibility.getDescription());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[]{
      myUser.getId(),
      myDescription
    });
  }
}