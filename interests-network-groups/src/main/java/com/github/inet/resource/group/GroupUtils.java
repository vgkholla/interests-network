package com.github.inet.resource.group;

import com.github.inet.entities.GroupProtos;
import com.google.protobuf.util.JsonFormat;


public class GroupUtils {

  public static GroupProtos.Group convert(String json) {
    GroupProtos.Group.Builder builder = GroupProtos.Group.newBuilder();
    try {
      JsonFormat.parser().ignoringUnknownFields().merge(json,builder);
      return builder.build();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

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
