package com.github.ptracker.gardener;

import com.github.ptracker.entity.Gardener;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.GardenerCreateRequest;
import com.github.ptracker.service.GardenerCreateResponse;
import com.github.ptracker.service.GardenerDeleteRequest;
import com.github.ptracker.service.GardenerDeleteResponse;
import com.github.ptracker.service.GardenerGetRequest;
import com.github.ptracker.service.GardenerGetResponse;
import com.github.ptracker.service.GardenerGrpc.GardenerImplBase;
import com.github.ptracker.service.GardenerUpdateRequest;
import com.github.ptracker.service.GardenerUpdateResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class GardenerService extends GardenerImplBase {
  private final Resource<String, Gardener> _gardenerResource;

  public GardenerService(Resource<String, Gardener> gardenerResource) {
    _gardenerResource = checkNotNull(gardenerResource, "Gardener Resource cannot be null");
  }

  @Override
  public void get(GardenerGetRequest request, StreamObserver<GardenerGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Gardener ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Optional<Gardener>> response =
          _gardenerResource.get(request.getId(), new GetRequestOptionsImpl.Builder().build());
      if (response.getPayload().isPresent()) {
        responseObserver.onNext(GardenerGetResponse.newBuilder().setGardener(response.getPayload().get()).build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new StatusRuntimeException(Status.NOT_FOUND.augmentDescription("Did not find " + request.getId())));
      }
    }
  }

  @Override
  public void create(GardenerCreateRequest request, StreamObserver<GardenerCreateResponse> responseObserver) {
    if (!request.hasGardener()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Gardener is missing")));
    } else {
      ResourceResponse<Void> createResponse = _gardenerResource.create(request.getGardener(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(GardenerCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(GardenerUpdateRequest request, StreamObserver<GardenerUpdateResponse> responseObserver) {
    if (!request.hasGardener()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Gardener is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _gardenerResource.update(request.getGardener(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(GardenerUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(GardenerDeleteRequest request, StreamObserver<GardenerDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Gardener ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _gardenerResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(GardenerDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
