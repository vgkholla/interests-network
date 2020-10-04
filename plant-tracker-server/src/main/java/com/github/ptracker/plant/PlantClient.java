package com.github.ptracker.plant;

import com.github.ptracker.entity.Plant;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.QueryRequestOptions;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.PlantCreateRequest;
import com.github.ptracker.service.PlantDeleteRequest;
import com.github.ptracker.service.PlantGetRequest;
import com.github.ptracker.service.PlantGrpc;
import com.github.ptracker.service.PlantGrpc.PlantBlockingStub;
import com.github.ptracker.service.PlantQueryRequest;
import com.github.ptracker.service.PlantUpdateRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;


public class PlantClient implements GrpcResource.GrpcClient<String, Plant> {
  private final PlantBlockingStub _blockingStub;

  public PlantClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public PlantClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = PlantGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public Plant get(String key, GetRequestOptions options) {
    PlantGetRequest request = PlantGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getPlant();
  }

  @Override
  public List<Plant> query(Plant template, QueryRequestOptions options) {
    PlantQueryRequest request = PlantQueryRequest.newBuilder().setTemplate(template).build();
    return _blockingStub.query(request).getPlantList();
  }

  @Override
  public void create(Plant payload, CreateRequestOptions options) {
    PlantCreateRequest request = PlantCreateRequest.newBuilder().setPlant(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(Plant payload, UpdateRequestOptions options) {
    PlantUpdateRequest request =
        PlantUpdateRequest.newBuilder().setPlant(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    PlantDeleteRequest request = PlantDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
