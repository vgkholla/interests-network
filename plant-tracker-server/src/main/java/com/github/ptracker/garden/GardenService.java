package com.github.ptracker.garden;

import com.github.ptracker.entity.Garden;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.GardenCreateRequest;
import com.github.ptracker.service.GardenCreateResponse;
import com.github.ptracker.service.GardenDeleteRequest;
import com.github.ptracker.service.GardenDeleteResponse;
import com.github.ptracker.service.GardenGetRequest;
import com.github.ptracker.service.GardenGetResponse;
import com.github.ptracker.service.GardenGrpc.GardenImplBase;
import com.github.ptracker.service.GardenUpdateRequest;
import com.github.ptracker.service.GardenUpdateResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class GardenService extends GardenImplBase {
  private final Resource<String, Garden> _gardenResource;

  public GardenService(Resource<String, Garden> gardenResource) {
    _gardenResource = checkNotNull(gardenResource, "Garden Resource cannot be null");
  }

  @Override
  public void get(GardenGetRequest request, StreamObserver<GardenGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Garden ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Optional<Garden>> response =
          _gardenResource.get(request.getId(), new GetRequestOptionsImpl.Builder().build());
      if (response.getPayload().isPresent()) {
        responseObserver.onNext(GardenGetResponse.newBuilder().setGarden(response.getPayload().get()).build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new StatusRuntimeException(Status.NOT_FOUND.augmentDescription("Did not find " + request.getId())));
      }
    }
  }

  @Override
  public void create(GardenCreateRequest request, StreamObserver<GardenCreateResponse> responseObserver) {
    if (!request.hasGarden()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Garden is missing")));
    } else {
      ResourceResponse<Void> createResponse = _gardenResource.create(request.getGarden(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(GardenCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(GardenUpdateRequest request, StreamObserver<GardenUpdateResponse> responseObserver) {
    if (!request.hasGarden()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Garden is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _gardenResource.update(request.getGarden(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(GardenUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(GardenDeleteRequest request, StreamObserver<GardenDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Garden ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _gardenResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(GardenDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
