package com.github.ptracker.gardener;

import com.github.ptracker.entity.Gardener;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.QueryRequestOptions;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.GardenerCreateRequest;
import com.github.ptracker.service.GardenerDeleteRequest;
import com.github.ptracker.service.GardenerGetRequest;
import com.github.ptracker.service.GardenerGrpc;
import com.github.ptracker.service.GardenerGrpc.GardenerBlockingStub;
import com.github.ptracker.service.GardenerQueryRequest;
import com.github.ptracker.service.GardenerUpdateRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;


public class GardenerClient implements GrpcResource.GrpcClient<String, Gardener> {
  private final GardenerBlockingStub _blockingStub;

  public GardenerClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public GardenerClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = GardenerGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public Gardener get(String key, GetRequestOptions options) {
    GardenerGetRequest request = GardenerGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getGardener();
  }

  @Override
  public List<Gardener> query(Gardener template, QueryRequestOptions options) {
    GardenerQueryRequest request = GardenerQueryRequest.newBuilder().setTemplate(template).build();
    return _blockingStub.query(request).getGardenerList();
  }

  @Override
  public void create(Gardener payload, CreateRequestOptions options) {
    GardenerCreateRequest request = GardenerCreateRequest.newBuilder().setGardener(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(Gardener payload, UpdateRequestOptions options) {
    GardenerUpdateRequest request =
        GardenerUpdateRequest.newBuilder().setGardener(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    GardenerDeleteRequest request = GardenerDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
