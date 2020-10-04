package com.github.ptracker.gardenplant;

import com.github.ptracker.entity.GardenPlant;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.QueryRequestOptions;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.GardenPlantCreateRequest;
import com.github.ptracker.service.GardenPlantDeleteRequest;
import com.github.ptracker.service.GardenPlantGetRequest;
import com.github.ptracker.service.GardenPlantGrpc;
import com.github.ptracker.service.GardenPlantGrpc.GardenPlantBlockingStub;
import com.github.ptracker.service.GardenPlantQueryRequest;
import com.github.ptracker.service.GardenPlantUpdateRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;


public class GardenPlantClient implements GrpcResource.GrpcClient<String, GardenPlant> {
  private final GardenPlantBlockingStub _blockingStub;

  public GardenPlantClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public GardenPlantClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = GardenPlantGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public GardenPlant get(String key, GetRequestOptions options) {
    GardenPlantGetRequest request = GardenPlantGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getGardenPlant();
  }

  @Override
  public List<GardenPlant> query(GardenPlant template, QueryRequestOptions options) {
    GardenPlantQueryRequest request = GardenPlantQueryRequest.newBuilder().setTemplate(template).build();
    return _blockingStub.query(request).getGardenPlantList();
  }

  @Override
  public void create(GardenPlant payload, CreateRequestOptions options) {
    GardenPlantCreateRequest request = GardenPlantCreateRequest.newBuilder().setGardenPlant(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(GardenPlant payload, UpdateRequestOptions options) {
    GardenPlantUpdateRequest request =
        GardenPlantUpdateRequest.newBuilder().setGardenPlant(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    GardenPlantDeleteRequest request = GardenPlantDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
