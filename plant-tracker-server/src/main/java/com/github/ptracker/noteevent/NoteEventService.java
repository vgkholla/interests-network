package com.github.ptracker.noteevent;

import com.github.ptracker.StreamObserverConverter;
import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.QueryRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.NoteEventCreateRequest;
import com.github.ptracker.service.NoteEventCreateResponse;
import com.github.ptracker.service.NoteEventDeleteRequest;
import com.github.ptracker.service.NoteEventDeleteResponse;
import com.github.ptracker.service.NoteEventGetRequest;
import com.github.ptracker.service.NoteEventGetResponse;
import com.github.ptracker.service.NoteEventGrpc.NoteEventImplBase;
import com.github.ptracker.service.NoteEventQueryRequest;
import com.github.ptracker.service.NoteEventQueryResponse;
import com.github.ptracker.service.NoteEventUpdateRequest;
import com.github.ptracker.service.NoteEventUpdateResponse;
import com.google.common.collect.Iterables;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;

import static com.google.common.base.Preconditions.*;


public class NoteEventService extends NoteEventImplBase {
  private final Resource<String, NoteEvent> _noteEventResource;

  public NoteEventService(Resource<String, NoteEvent> noteEventResource) {
    _noteEventResource = checkNotNull(noteEventResource, "NoteEvent Resource cannot be null");
  }

  @Override
  public void get(NoteEventGetRequest request, StreamObserver<NoteEventGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("NoteEvent ID is missing")));
    } else {
      // TODO: add metadata
      NoteEventQueryRequest noteEventQueryRequest =
          NoteEventQueryRequest.newBuilder().setTemplate(NoteEvent.newBuilder().setId(request.getId())).build();
      query(noteEventQueryRequest, new StreamObserverConverter<>(responseObserver, noteEventQueryResponse -> {
        NoteEvent noteEvent = Iterables.getOnlyElement(noteEventQueryResponse.getNoteEventList());
        responseObserver.onNext(NoteEventGetResponse.newBuilder().setNoteEvent(noteEvent).build());
        }));
    }
  }

  @Override
  public void query(NoteEventQueryRequest request, StreamObserver<NoteEventQueryResponse> responseObserver) {
    if (request.getTemplate() == null) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Template is missing")));
    } else {
      // TODO: add metadata
      List<ResourceResponse<NoteEvent>> responses =
          _noteEventResource.query(request.getTemplate(), new QueryRequestOptionsImpl.Builder().build());
      NoteEventQueryResponse.Builder responseBuilder = NoteEventQueryResponse.newBuilder();
      if (!responses.isEmpty()) {
        responses.forEach(response -> {
          if (response.getStatus().equals(ResponseStatus.OK)) {
            responseBuilder.addNoteEvent(response.getPayload());
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
  public void create(NoteEventCreateRequest request, StreamObserver<NoteEventCreateResponse> responseObserver) {
    if (!request.hasNoteEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("NoteEvent is missing")));
    } else {
      ResourceResponse<Void> createResponse =
          _noteEventResource.create(request.getNoteEvent(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(NoteEventCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(NoteEventUpdateRequest request, StreamObserver<NoteEventUpdateResponse> responseObserver) {
    if (!request.hasNoteEvent()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("NoteEvent is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _noteEventResource.update(request.getNoteEvent(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(NoteEventUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(NoteEventDeleteRequest request, StreamObserver<NoteEventDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("NoteEvent ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _noteEventResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(NoteEventDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
