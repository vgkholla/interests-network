package com.github.ptracker.fertilizationevent;

import com.github.ptracker.StreamObserverConverter;
import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.QueryRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.FertilizationEventCreateRequest;
import com.github.ptracker.service.FertilizationEventCreateResponse;
import com.github.ptracker.service.FertilizationEventDeleteRequest;
import com.github.ptracker.service.FertilizationEventDeleteResponse;
import com.github.ptracker.service.FertilizationEventGetRequest;
import com.github.ptracker.service.FertilizationEventGetResponse;
import com.github.ptracker.service.FertilizationEventGrpc.FertilizationEventImplBase;
import com.github.ptracker.service.FertilizationEventQueryRequest;
import com.github.ptracker.service.FertilizationEventQueryResponse;
import com.github.ptracker.service.FertilizationEventUpdateRequest;
import com.github.ptracker.service.FertilizationEventUpdateResponse;
import com.google.common.collect.Iterables;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;

import static com.google.common.base.Preconditions.*;


public class FertilizationEventService extends FertilizationEventImplBase {
  private final Resource<String, FertilizationEvent> _fertilizationEventResource;

  public FertilizationEventService(Resource<String, FertilizationEvent> fertilizationEventResource) {
    _fertilizationEventResource =
        checkNotNull(fertilizationEventResource, "FertilizationEvent Resource cannot be null");
  }

  @Override
  public void get(FertilizationEventGetRequest request,
      StreamObserver<FertilizationEventGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(new StatusRuntimeException(
          Status.FAILED_PRECONDITION.augmentDescription("FertilizationEvent ID is missing")));
    } else {
      // TODO: add metadata
      FertilizationEventQueryRequest fertilizationEventQueryRequest = FertilizationEventQueryRequest.newBuilder()
          .setTemplate(FertilizationEvent.newBuilder().setId(request.getId()))
          .build();
      query(fertilizationEventQueryRequest,
          new StreamObserverConverter<>(responseObserver, fertilizationEventQueryResponse -> {
            FertilizationEvent fertilizationEvent =
                Iterables.getOnlyElement(fertilizationEventQueryResponse.getFertilizationEventList());
            responseObserver.onNext(
                FertilizationEventGetResponse.newBuilder().setFertilizationEvent(fertilizationEvent).build());
            }));
    }
  }

  @Override
  public void query(FertilizationEventQueryRequest request,
      StreamObserver<FertilizationEventQueryResponse> responseObserver) {
    if (request.getTemplate() == null) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Template is missing")));
    } else {
      // TODO: add metadata
      List<ResourceResponse<FertilizationEvent>> responses =
          _fertilizationEventResource.query(request.getTemplate(), new QueryRequestOptionsImpl.Builder().build());
      FertilizationEventQueryResponse.Builder responseBuilder = FertilizationEventQueryResponse.newBuilder();
      if (!responses.isEmpty()) {
        responses.forEach(response -> {
          if (response.getStatus().equals(ResponseStatus.OK)) {
            responseBuilder.addFertilizationEvent(response.getPayload());
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
  public void create(FertilizationEventCreateRequest request,
      StreamObserver<FertilizationEventCreateResponse> responseObserver) {
    if (!request.hasFertilizationEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("FertilizationEvent is missing")));
    } else {
      ResourceResponse<Void> createResponse =
          _fertilizationEventResource.create(request.getFertilizationEvent(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(FertilizationEventCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(FertilizationEventUpdateRequest request,
      StreamObserver<FertilizationEventUpdateResponse> responseObserver) {
    if (!request.hasFertilizationEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("FertilizationEvent is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _fertilizationEventResource.update(request.getFertilizationEvent(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(FertilizationEventUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(FertilizationEventDeleteRequest request,
      StreamObserver<FertilizationEventDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(new StatusRuntimeException(
          Status.FAILED_PRECONDITION.augmentDescription("FertilizationEvent ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _fertilizationEventResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(FertilizationEventDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
