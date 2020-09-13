package com.github.inet.client.group;

import com.github.inet.entity.Group;
import com.github.inet.resource.CreateRequestOptions;
import com.github.inet.resource.DeleteRequestOptions;
import com.github.inet.resource.GetRequestOptions;
import com.github.inet.resource.Resource;
import com.github.inet.resource.ResourceResponse;
import com.github.inet.resource.ResourceResponseImpl;
import com.github.inet.resource.ResponseStatus;
import com.github.inet.resource.UpdateRequestOptions;
import com.github.inet.service.GroupCreateRequest;
import com.github.inet.service.GroupDeleteRequest;
import com.github.inet.service.GroupGetRequest;
import com.github.inet.service.GroupUpdateRequest;
import com.github.inet.service.GroupGrpc;
import com.github.inet.service.GroupGrpc.GroupBlockingStub;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class GroupClient implements Resource<String, Group> {
  private final GroupBlockingStub _blockingStub;

  public GroupClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public GroupClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = GroupGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public ResourceResponse<Optional<Group>> get(String key, GetRequestOptions options) {
    checkArgument(key != null && !key.isEmpty(), "id cannot be null or empty");
    GroupGetRequest request = GroupGetRequest.newBuilder().setId(key).build();
    Optional<Group> group = Optional.empty();
    ResponseStatus status = ResponseStatus.OK;
    try {
      group = Optional.of(_blockingStub.get(request).getGroup());
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Optional<Group>>().status(status).payload(group).build();
  }

  @Override
  public ResourceResponse<Void> create(Group payload, CreateRequestOptions options) {
    checkNotNull(payload, "Create payload cannot be null");
    GroupCreateRequest request = GroupCreateRequest.newBuilder().setGroup(payload).build();
    ResponseStatus status = ResponseStatus.OK;
    try {
      _blockingStub.create(request);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }

  @Override
  public ResourceResponse<Void> update(Group payload, UpdateRequestOptions options) {
    checkNotNull(payload, "Update payload cannot be null");
    GroupUpdateRequest request =
        GroupUpdateRequest.newBuilder().setGroup(payload).setShouldUpsert(options.shouldUpsert()).build();
    ResponseStatus status = ResponseStatus.OK;
    try {
      _blockingStub.update(request);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }

  @Override
  public ResourceResponse<Void> delete(String key, DeleteRequestOptions options) {
    checkArgument(key != null && !key.isEmpty(), "id cannot be null or empty");
    GroupDeleteRequest request = GroupDeleteRequest.newBuilder().setId(key).build();
    ResponseStatus status = ResponseStatus.OK;
    try {
      _blockingStub.delete(request);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }
}
