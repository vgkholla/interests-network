package com.github.ptracker.garden;

import com.github.ptracker.entity.Garden;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.QueryRequestOptions;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.GardenCreateRequest;
import com.github.ptracker.service.GardenDeleteRequest;
import com.github.ptracker.service.GardenGetRequest;
import com.github.ptracker.service.GardenGrpc;
import com.github.ptracker.service.GardenGrpc.GardenBlockingStub;
import com.github.ptracker.service.GardenQueryRequest;
import com.github.ptracker.service.GardenUpdateRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;


public class GardenClient implements GrpcResource.GrpcClient<String, Garden> {
  private final GardenBlockingStub _blockingStub;

  public GardenClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public GardenClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = GardenGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public Garden get(String key, GetRequestOptions options) {
    GardenGetRequest request = GardenGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getGarden();
  }

  @Override
  public List<Garden> query(Garden template, QueryRequestOptions options) {
    GardenQueryRequest request = GardenQueryRequest.newBuilder().setTemplate(template).build();
    return _blockingStub.query(request).getGardenList();
  }

  @Override
  public void create(Garden payload, CreateRequestOptions options) {
    GardenCreateRequest request = GardenCreateRequest.newBuilder().setGarden(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(Garden payload, UpdateRequestOptions options) {
    GardenUpdateRequest request =
        GardenUpdateRequest.newBuilder().setGarden(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    GardenDeleteRequest request = GardenDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
