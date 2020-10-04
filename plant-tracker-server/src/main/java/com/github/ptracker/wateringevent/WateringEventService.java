package com.github.ptracker.wateringevent;

import com.github.ptracker.StreamObserverConverter;
import com.github.ptracker.entity.WateringEvent;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.QueryRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.WateringEventCreateRequest;
import com.github.ptracker.service.WateringEventCreateResponse;
import com.github.ptracker.service.WateringEventDeleteRequest;
import com.github.ptracker.service.WateringEventDeleteResponse;
import com.github.ptracker.service.WateringEventGetRequest;
import com.github.ptracker.service.WateringEventGetResponse;
import com.github.ptracker.service.WateringEventGrpc.WateringEventImplBase;
import com.github.ptracker.service.WateringEventQueryRequest;
import com.github.ptracker.service.WateringEventQueryResponse;
import com.github.ptracker.service.WateringEventUpdateRequest;
import com.github.ptracker.service.WateringEventUpdateResponse;
import com.google.common.collect.Iterables;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;

import static com.google.common.base.Preconditions.*;


public class WateringEventService extends WateringEventImplBase {
  private final Resource<String, WateringEvent> _wateringEventResource;

  public WateringEventService(Resource<String, WateringEvent> wateringEventResource) {
    _wateringEventResource = checkNotNull(wateringEventResource, "WateringEvent Resource cannot be null");
  }

  @Override
  public void get(WateringEventGetRequest request, StreamObserver<WateringEventGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("WateringEvent ID is missing")));
    } else {
      // TODO: add metadata
      WateringEventQueryRequest wateringEventQueryRequest =
          WateringEventQueryRequest.newBuilder().setTemplate(WateringEvent.newBuilder().setId(request.getId())).build();
      query(wateringEventQueryRequest, new StreamObserverConverter<>(responseObserver, wateringEventQueryResponse -> {
        WateringEvent wateringEvent = Iterables.getOnlyElement(wateringEventQueryResponse.getWateringEventList());
        responseObserver.onNext(WateringEventGetResponse.newBuilder().setWateringEvent(wateringEvent).build());
        }));
    }
  }

  @Override
  public void query(WateringEventQueryRequest request, StreamObserver<WateringEventQueryResponse> responseObserver) {
    if (request.getTemplate() == null) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Template is missing")));
    } else {
      // TODO: add metadata
      List<ResourceResponse<WateringEvent>> responses =
          _wateringEventResource.query(request.getTemplate(), new QueryRequestOptionsImpl.Builder().build());
      WateringEventQueryResponse.Builder responseBuilder = WateringEventQueryResponse.newBuilder();
      if (!responses.isEmpty()) {
        responses.forEach(response -> {
          if (response.getStatus().equals(ResponseStatus.OK)) {
            responseBuilder.addWateringEvent(response.getPayload());
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
  public void create(WateringEventCreateRequest request, StreamObserver<WateringEventCreateResponse> responseObserver) {
    if (!request.hasWateringEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("WateringEvent is missing")));
    } else {
      ResourceResponse<Void> createResponse =
          _wateringEventResource.create(request.getWateringEvent(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(WateringEventCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(WateringEventUpdateRequest request, StreamObserver<WateringEventUpdateResponse> responseObserver) {
    if (!request.hasWateringEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("WateringEvent is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _wateringEventResource.update(request.getWateringEvent(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(WateringEventUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(WateringEventDeleteRequest request, StreamObserver<WateringEventDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("WateringEvent ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _wateringEventResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(WateringEventDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
