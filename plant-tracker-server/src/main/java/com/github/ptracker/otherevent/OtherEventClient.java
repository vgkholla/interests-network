package com.github.ptracker.otherevent;

import com.github.ptracker.entity.OtherEvent;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.OtherEventCreateRequest;
import com.github.ptracker.service.OtherEventDeleteRequest;
import com.github.ptracker.service.OtherEventGetRequest;
import com.github.ptracker.service.OtherEventGrpc;
import com.github.ptracker.service.OtherEventGrpc.OtherEventBlockingStub;
import com.github.ptracker.service.OtherEventUpdateRequest;
import io.grpc.ManagedChannelBuilder;


public class OtherEventClient implements GrpcResource.GrpcClient<String, OtherEvent> {
  private final OtherEventBlockingStub _blockingStub;

  public OtherEventClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public OtherEventClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = OtherEventGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public OtherEvent get(String key, GetRequestOptions options) {
    OtherEventGetRequest request = OtherEventGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getOtherEvent();
  }

  @Override
  public void create(OtherEvent payload, CreateRequestOptions options) {
    OtherEventCreateRequest request = OtherEventCreateRequest.newBuilder().setOtherEvent(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(OtherEvent payload, UpdateRequestOptions options) {
    OtherEventUpdateRequest request =
        OtherEventUpdateRequest.newBuilder().setOtherEvent(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    OtherEventDeleteRequest request = OtherEventDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
