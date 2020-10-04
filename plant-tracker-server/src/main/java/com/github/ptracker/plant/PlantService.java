package com.github.ptracker.plant;

import com.github.ptracker.StreamObserverConverter;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.entity.Plant;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.QueryRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantGetResponse;
import com.github.ptracker.service.PlantQueryRequest;
import com.github.ptracker.service.PlantQueryResponse;
import com.github.ptracker.service.PlantCreateRequest;
import com.github.ptracker.service.PlantCreateResponse;
import com.github.ptracker.service.PlantDeleteRequest;
import com.github.ptracker.service.PlantDeleteResponse;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantGetResponse;
import com.github.ptracker.service.PlantGrpc.PlantImplBase;
import com.github.ptracker.service.PlantUpdateRequest;
import com.github.ptracker.service.PlantUpdateResponse;
import com.google.common.collect.Iterables;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class PlantService extends PlantImplBase {
  private final Resource<String, Plant> _plantResource;

  public PlantService(Resource<String, Plant> plantResource) {
    _plantResource = checkNotNull(plantResource, "Plant Resource cannot be null");
  }

  @Override
  public void get(PlantGetRequest request, StreamObserver<PlantGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Plant ID is missing")));
    } else {
      // TODO: add metadata
      PlantQueryRequest plantQueryRequest =
          PlantQueryRequest.newBuilder().setTemplate(Plant.newBuilder().setId(request.getId())).build();
      query(plantQueryRequest, new StreamObserverConverter<>(responseObserver, plantQueryResponse -> {
        Plant plant = Iterables.getOnlyElement(plantQueryResponse.getPlantList());
        responseObserver.onNext(PlantGetResponse.newBuilder().setPlant(plant).build());
      }));
    }
  }

  @Override
  public void query(PlantQueryRequest request, StreamObserver<PlantQueryResponse> responseObserver) {
    if (request.getTemplate() == null) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Template is missing")));
    } else {
      // TODO: add metadata
      List<ResourceResponse<Plant>> responses =
          _plantResource.query(request.getTemplate(), new QueryRequestOptionsImpl.Builder().build());
      PlantQueryResponse.Builder responseBuilder = PlantQueryResponse.newBuilder();
      if (!responses.isEmpty()) {
        responses.forEach(response -> {
          if (response.getStatus().equals(ResponseStatus.OK)) {
            responseBuilder.addPlant(response.getPayload());
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
  public void create(PlantCreateRequest request, StreamObserver<PlantCreateResponse> responseObserver) {
    if (!request.hasPlant()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Plant is missing")));
    } else {
      ResourceResponse<Void> createResponse = _plantResource.create(request.getPlant(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(PlantCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(PlantUpdateRequest request, StreamObserver<PlantUpdateResponse> responseObserver) {
    if (!request.hasPlant()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Plant is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _plantResource.update(request.getPlant(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(PlantUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(PlantDeleteRequest request, StreamObserver<PlantDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Plant ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _plantResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(PlantDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
