package com.github.ptracker.otherevent;

import com.github.ptracker.entity.OtherEvent;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.OtherEventCreateRequest;
import com.github.ptracker.service.OtherEventCreateResponse;
import com.github.ptracker.service.OtherEventDeleteRequest;
import com.github.ptracker.service.OtherEventDeleteResponse;
import com.github.ptracker.service.OtherEventGetRequest;
import com.github.ptracker.service.OtherEventGetResponse;
import com.github.ptracker.service.OtherEventGrpc.OtherEventImplBase;
import com.github.ptracker.service.OtherEventUpdateRequest;
import com.github.ptracker.service.OtherEventUpdateResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class OtherEventService extends OtherEventImplBase {
  private final Resource<String, OtherEvent> _otherEventResource;

  public OtherEventService(Resource<String, OtherEvent> otherEventResource) {
    _otherEventResource = checkNotNull(otherEventResource, "OtherEvent Resource cannot be null");
  }

  @Override
  public void get(OtherEventGetRequest request, StreamObserver<OtherEventGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("OtherEvent ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Optional<OtherEvent>> response =
          _otherEventResource.get(request.getId(), new GetRequestOptionsImpl.Builder().build());
      if (response.getPayload().isPresent()) {
        responseObserver.onNext(OtherEventGetResponse.newBuilder().setOtherEvent(response.getPayload().get()).build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new StatusRuntimeException(Status.NOT_FOUND.augmentDescription("Did not find " + request.getId())));
      }
    }
  }

  @Override
  public void create(OtherEventCreateRequest request, StreamObserver<OtherEventCreateResponse> responseObserver) {
    if (!request.hasOtherEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("OtherEvent is missing")));
    } else {
      ResourceResponse<Void> createResponse = _otherEventResource.create(request.getOtherEvent(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(OtherEventCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(OtherEventUpdateRequest request, StreamObserver<OtherEventUpdateResponse> responseObserver) {
    if (!request.hasOtherEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("OtherEvent is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _otherEventResource.update(request.getOtherEvent(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(OtherEventUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(OtherEventDeleteRequest request, StreamObserver<OtherEventDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("OtherEvent ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _otherEventResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(OtherEventDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
