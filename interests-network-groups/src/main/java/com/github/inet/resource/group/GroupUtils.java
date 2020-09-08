package com.github.inet.resource.group;

import com.github.inet.entities.GroupProtos;


public class GroupUtils {

  public static void verifyGroup(GroupProtos.Group group) {
    if (group.getId() == null || group.getId().isEmpty()) {
      throw new IllegalArgumentException("Group does not have an ID");
    }
    if (group.getName() == null || group.getName().isEmpty()) {
      throw new IllegalArgumentException("Group does not have a name");
    }
  }

  private GroupUtils() {
  }

}
