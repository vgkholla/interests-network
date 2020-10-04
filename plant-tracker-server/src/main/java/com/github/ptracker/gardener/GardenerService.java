package com.github.ptracker.gardener;

import com.github.ptracker.StreamObserverConverter;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.entity.Gardener;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.QueryRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.GardenerGetRequest;
import com.github.ptracker.service.GardenerGetResponse;
import com.github.ptracker.service.GardenerQueryRequest;
import com.github.ptracker.service.GardenerQueryResponse;
import com.github.ptracker.service.GardenerCreateRequest;
import com.github.ptracker.service.GardenerCreateResponse;
import com.github.ptracker.service.GardenerDeleteRequest;
import com.github.ptracker.service.GardenerDeleteResponse;
import com.github.ptracker.service.GardenerGetRequest;
import com.github.ptracker.service.GardenerGetResponse;
import com.github.ptracker.service.GardenerGrpc.GardenerImplBase;
import com.github.ptracker.service.GardenerUpdateRequest;
import com.github.ptracker.service.GardenerUpdateResponse;
import com.google.common.collect.Iterables;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
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
      GardenerQueryRequest gardenerQueryRequest =
          GardenerQueryRequest.newBuilder().setTemplate(Gardener.newBuilder().setId(request.getId())).build();
      query(gardenerQueryRequest, new StreamObserverConverter<>(responseObserver, gardenerQueryResponse -> {
        Gardener gardener = Iterables.getOnlyElement(gardenerQueryResponse.getGardenerList());
        responseObserver.onNext(GardenerGetResponse.newBuilder().setGardener(gardener).build());
        }));
    }
  }

  @Override
  public void query(GardenerQueryRequest request, StreamObserver<GardenerQueryResponse> responseObserver) {
    if (request.getTemplate() == null) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Template is missing")));
    } else {
      // TODO: add metadata
      List<ResourceResponse<Gardener>> responses =
          _gardenerResource.query(request.getTemplate(), new QueryRequestOptionsImpl.Builder().build());
      GardenerQueryResponse.Builder responseBuilder = GardenerQueryResponse.newBuilder();
      if (!responses.isEmpty()) {
        responses.forEach(response -> {
          if (response.getStatus().equals(ResponseStatus.OK)) {
            responseBuilder.addGardener(response.getPayload());
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
