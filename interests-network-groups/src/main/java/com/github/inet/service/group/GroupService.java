package com.github.inet.service.group;

import com.github.inet.entity.Group;
import com.github.inet.resource.CreateRequestOptionsImpl;
import com.github.inet.resource.DeleteRequestOptionsImpl;
import com.github.inet.resource.GetRequestOptionsImpl;
import com.github.inet.resource.Resource;
import com.github.inet.resource.ResourceResponse;
import com.github.inet.resource.ResponseStatus;
import com.github.inet.resource.UpdateRequestOptionsImpl;
import com.github.inet.service.GroupCreateRequest;
import com.github.inet.service.GroupCreateResponse;
import com.github.inet.service.GroupDeleteRequest;
import com.github.inet.service.GroupDeleteResponse;
import com.github.inet.service.GroupGetRequest;
import com.github.inet.service.GroupGetResponse;
import com.github.inet.service.GroupUpdateRequest;
import com.github.inet.service.GroupUpdateResponse;
import com.github.inet.service.GroupGrpc.GroupImplBase;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class GroupService extends GroupImplBase {
  private final Resource<String, Group> _groupResource;

  public GroupService(Resource<String, Group> groupResource) {
    _groupResource = checkNotNull(groupResource, "Group Resource cannot be null");
  }

  @Override
  public void get(GroupGetRequest request, StreamObserver<GroupGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Group ID is missing")));
    } else {
      // todo: add metadata
      ResourceResponse<Optional<Group>> response =
          _groupResource.get(request.getId(), new GetRequestOptionsImpl.Builder().build());
      if (response.getPayload().isPresent()) {
        responseObserver.onNext(GroupGetResponse.newBuilder().setGroup(response.getPayload().get()).build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new StatusRuntimeException(Status.NOT_FOUND.augmentDescription("Did not find " + request.getId())));
      }
    }
  }

  @Override
  public void create(GroupCreateRequest request, StreamObserver<GroupCreateResponse> responseObserver) {
    if (!request.hasGroup()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Group is missing")));
    } else {
      ResourceResponse<Void> createResponse = _groupResource.create(request.getGroup(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(GroupCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(GroupUpdateRequest request, StreamObserver<GroupUpdateResponse> responseObserver) {
    if (!request.hasGroup()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Group is missing")));
    } else {
      // todo: add metadata
      ResourceResponse<Void> updateResponse = _groupResource.update(request.getGroup(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(GroupUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(GroupDeleteRequest request, StreamObserver<GroupDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Group ID is missing")));
    } else {
      // todo: add metadata
      ResourceResponse<Void> deleteResponse =
          _groupResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(GroupDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
