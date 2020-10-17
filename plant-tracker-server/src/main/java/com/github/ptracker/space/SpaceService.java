package com.github.ptracker.space;

import com.github.ptracker.StreamObserverConverter;
import com.github.ptracker.entity.Space;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.QueryRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.SpaceCreateRequest;
import com.github.ptracker.service.SpaceCreateResponse;
import com.github.ptracker.service.SpaceDeleteRequest;
import com.github.ptracker.service.SpaceDeleteResponse;
import com.github.ptracker.service.SpaceGetRequest;
import com.github.ptracker.service.SpaceGetResponse;
import com.github.ptracker.service.SpaceGrpc.SpaceImplBase;
import com.github.ptracker.service.SpaceQueryRequest;
import com.github.ptracker.service.SpaceQueryResponse;
import com.github.ptracker.service.SpaceUpdateRequest;
import com.github.ptracker.service.SpaceUpdateResponse;
import com.google.common.collect.Iterables;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class SpaceService extends SpaceImplBase {
  private final Resource<String, Space> _spaceResource;

  public SpaceService(Resource<String, Space> spaceResource) {
    _spaceResource = checkNotNull(spaceResource, "Space Resource cannot be null");
  }

  @Override
  public void get(SpaceGetRequest request, StreamObserver<SpaceGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Space ID is missing")));
    } else {
      // TODO: add metadata
      SpaceQueryRequest spaceQueryRequest =
          SpaceQueryRequest.newBuilder().setTemplate(Space.newBuilder().setId(request.getId())).build();
      query(spaceQueryRequest, new StreamObserverConverter<>(responseObserver, spaceQueryResponse -> {
        Space space = Iterables.getOnlyElement(spaceQueryResponse.getSpaceList());
        responseObserver.onNext(SpaceGetResponse.newBuilder().setSpace(space).build());
      }));
    }
  }

  @Override
  public void query(SpaceQueryRequest request, StreamObserver<SpaceQueryResponse> responseObserver) {
    if (request.getTemplate() == null) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Template is missing")));
    } else {
      // TODO: add metadata
      List<ResourceResponse<Space>> responses =
          _spaceResource.query(request.getTemplate(), new QueryRequestOptionsImpl.Builder().build());
      SpaceQueryResponse.Builder responseBuilder = SpaceQueryResponse.newBuilder();
      if (!responses.isEmpty()) {
        responses.forEach(response -> {
          if (response.getStatus().equals(ResponseStatus.OK)) {
            responseBuilder.addSpace(response.getPayload());
          }
        });
        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(
            Status.NOT_FOUND.augmentDescription("Did not find anything matching " + request.getTemplate())));
      }
    }
  }

  @Override
  public void create(SpaceCreateRequest request, StreamObserver<SpaceCreateResponse> responseObserver) {
    if (!request.hasSpace()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Space is missing")));
    } else {
      ResourceResponse<Void> createResponse =
          _spaceResource.create(request.getSpace(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(SpaceCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(SpaceUpdateRequest request, StreamObserver<SpaceUpdateResponse> responseObserver) {
    if (!request.hasSpace()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Space is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _spaceResource.update(request.getSpace(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(SpaceUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(SpaceDeleteRequest request, StreamObserver<SpaceDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Space ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _spaceResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(SpaceDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
