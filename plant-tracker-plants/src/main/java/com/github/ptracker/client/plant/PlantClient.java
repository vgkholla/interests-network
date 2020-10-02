package com.github.ptracker.client.plant;

import com.github.ptracker.entity.Plant;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResourceResponseImpl;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.PlantCreateRequest;
import com.github.ptracker.service.PlantDeleteRequest;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantUpdateRequest;
import com.github.ptracker.service.PlantGrpc;
import com.github.ptracker.service.PlantGrpc.PlantBlockingStub;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class PlantClient implements Resource<String, Plant> {
  private final PlantBlockingStub _blockingStub;

  public PlantClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public PlantClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = PlantGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public ResourceResponse<Optional<Plant>> get(String key, GetRequestOptions options) {
    checkArgument(key != null && !key.isEmpty(), "id cannot be null or empty");
    PlantGetRequest request = PlantGetRequest.newBuilder().setId(key).build();
    Optional<Plant> plant = Optional.empty();
    ResponseStatus status = ResponseStatus.OK;
    try {
      plant = Optional.of(_blockingStub.get(request).getPlant());
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Optional<Plant>>().status(status).payload(plant).build();
  }

  @Override
  public ResourceResponse<Void> create(Plant payload, CreateRequestOptions options) {
    checkNotNull(payload, "Create payload cannot be null");
    PlantCreateRequest request = PlantCreateRequest.newBuilder().setPlant(payload).build();
    ResponseStatus status = ResponseStatus.OK;
    try {
      _blockingStub.create(request);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }

  @Override
  public ResourceResponse<Void> update(Plant payload, UpdateRequestOptions options) {
    checkNotNull(payload, "Update payload cannot be null");
    PlantUpdateRequest request =
        PlantUpdateRequest.newBuilder().setPlant(payload).setShouldUpsert(options.shouldUpsert()).build();
    ResponseStatus status = ResponseStatus.OK;
    try {
      _blockingStub.update(request);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }

  @Override
  public ResourceResponse<Void> delete(String key, DeleteRequestOptions options) {
    checkArgument(key != null && !key.isEmpty(), "id cannot be null or empty");
    PlantDeleteRequest request = PlantDeleteRequest.newBuilder().setId(key).build();
    ResponseStatus status = ResponseStatus.OK;
    try {
      _blockingStub.delete(request);
    } catch (StatusRuntimeException e) {
      status = ResponseStatus.fromGrpcStatus(e.getStatus());
    }
    return new ResourceResponseImpl.Builder<Void>().status(status).build();
  }
}
