package com.github.ptracker.fertilizationevent;

import com.github.ptracker.entity.FertilizationEvent;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.QueryRequestOptions;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.FertilizationEventCreateRequest;
import com.github.ptracker.service.FertilizationEventDeleteRequest;
import com.github.ptracker.service.FertilizationEventGetRequest;
import com.github.ptracker.service.FertilizationEventGrpc;
import com.github.ptracker.service.FertilizationEventGrpc.FertilizationEventBlockingStub;
import com.github.ptracker.service.FertilizationEventQueryRequest;
import com.github.ptracker.service.FertilizationEventUpdateRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;


public class FertilizationEventClient implements GrpcResource.GrpcClient<String, FertilizationEvent> {
  private final FertilizationEventBlockingStub _blockingStub;

  public FertilizationEventClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public FertilizationEventClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = FertilizationEventGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public FertilizationEvent get(String key, GetRequestOptions options) {
    FertilizationEventGetRequest request = FertilizationEventGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getFertilizationEvent();
  }

  @Override
  public List<FertilizationEvent> query(FertilizationEvent template, QueryRequestOptions options) {
    FertilizationEventQueryRequest request = FertilizationEventQueryRequest.newBuilder().setTemplate(template).build();
    return _blockingStub.query(request).getFertilizationEventList();
  }

  @Override
  public void create(FertilizationEvent payload, CreateRequestOptions options) {
    FertilizationEventCreateRequest request =
        FertilizationEventCreateRequest.newBuilder().setFertilizationEvent(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(FertilizationEvent payload, UpdateRequestOptions options) {
    FertilizationEventUpdateRequest request = FertilizationEventUpdateRequest.newBuilder()
        .setFertilizationEvent(payload)
        .setShouldUpsert(options.shouldUpsert())
        .build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    FertilizationEventDeleteRequest request = FertilizationEventDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
