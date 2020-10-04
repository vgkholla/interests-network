package com.github.ptracker.gardenplant;

import com.github.ptracker.StreamObserverConverter;
import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.QueryRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.GardenPlantCreateRequest;
import com.github.ptracker.service.GardenPlantCreateResponse;
import com.github.ptracker.service.GardenPlantDeleteRequest;
import com.github.ptracker.service.GardenPlantDeleteResponse;
import com.github.ptracker.service.GardenPlantGetRequest;
import com.github.ptracker.service.GardenPlantGetResponse;
import com.github.ptracker.service.GardenPlantGrpc.GardenPlantImplBase;
import com.github.ptracker.service.GardenPlantQueryRequest;
import com.github.ptracker.service.GardenPlantQueryResponse;
import com.github.ptracker.service.GardenPlantUpdateRequest;
import com.github.ptracker.service.GardenPlantUpdateResponse;
import com.google.common.collect.Iterables;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;

import static com.google.common.base.Preconditions.*;


public class GardenPlantService extends GardenPlantImplBase {
  private final Resource<String, GardenPlant> _gardenPlantResource;

  public GardenPlantService(Resource<String, GardenPlant> gardenPlantResource) {
    _gardenPlantResource = checkNotNull(gardenPlantResource, "GardenPlant Resource cannot be null");
  }

  @Override
  public void get(GardenPlantGetRequest request, StreamObserver<GardenPlantGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("GardenPlant ID is missing")));
    } else {
      // TODO: add metadata
      GardenPlantQueryRequest gardenPlantQueryRequest =
          GardenPlantQueryRequest.newBuilder().setTemplate(GardenPlant.newBuilder().setId(request.getId())).build();
      query(gardenPlantQueryRequest, new StreamObserverConverter<>(responseObserver, gardenPlantQueryResponse -> {
        GardenPlant gardenPlant = Iterables.getOnlyElement(gardenPlantQueryResponse.getGardenPlantList());
        responseObserver.onNext(GardenPlantGetResponse.newBuilder().setGardenPlant(gardenPlant).build());
        }));
    }
  }

  @Override
  public void query(GardenPlantQueryRequest request, StreamObserver<GardenPlantQueryResponse> responseObserver) {
    if (request.getTemplate() == null) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Template is missing")));
    } else {
      // TODO: add metadata
      List<ResourceResponse<GardenPlant>> responses =
          _gardenPlantResource.query(request.getTemplate(), new QueryRequestOptionsImpl.Builder().build());
      GardenPlantQueryResponse.Builder responseBuilder = GardenPlantQueryResponse.newBuilder();
      if (!responses.isEmpty()) {
        responses.forEach(response -> {
          if (response.getStatus().equals(ResponseStatus.OK)) {
            responseBuilder.addGardenPlant(response.getPayload());
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
  public void create(GardenPlantCreateRequest request, StreamObserver<GardenPlantCreateResponse> responseObserver) {
    if (!request.hasGardenPlant()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("GardenPlant is missing")));
    } else {
      ResourceResponse<Void> createResponse =
          _gardenPlantResource.create(request.getGardenPlant(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(GardenPlantCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(GardenPlantUpdateRequest request, StreamObserver<GardenPlantUpdateResponse> responseObserver) {
    if (!request.hasGardenPlant()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("GardenPlant is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _gardenPlantResource.update(request.getGardenPlant(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(GardenPlantUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(GardenPlantDeleteRequest request, StreamObserver<GardenPlantDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("GardenPlant ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _gardenPlantResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(GardenPlantDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
