package com.github.ptracker.wateringevent;

import com.github.ptracker.entity.WateringEvent;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.WateringEventCreateRequest;
import com.github.ptracker.service.WateringEventDeleteRequest;
import com.github.ptracker.service.WateringEventGetRequest;
import com.github.ptracker.service.WateringEventGrpc;
import com.github.ptracker.service.WateringEventGrpc.WateringEventBlockingStub;
import com.github.ptracker.service.WateringEventUpdateRequest;
import io.grpc.ManagedChannelBuilder;


public class WateringEventClient implements GrpcResource.GrpcClient<String, WateringEvent> {
  private final WateringEventBlockingStub _blockingStub;

  public WateringEventClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public WateringEventClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = WateringEventGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public WateringEvent get(String key, GetRequestOptions options) {
    WateringEventGetRequest request = WateringEventGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getWateringEvent();
  }

  @Override
  public void create(WateringEvent payload, CreateRequestOptions options) {
    WateringEventCreateRequest request = WateringEventCreateRequest.newBuilder().setWateringEvent(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(WateringEvent payload, UpdateRequestOptions options) {
    WateringEventUpdateRequest request =
        WateringEventUpdateRequest.newBuilder().setWateringEvent(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    WateringEventDeleteRequest request = WateringEventDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
