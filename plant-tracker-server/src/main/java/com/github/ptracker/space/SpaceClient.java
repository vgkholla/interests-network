package com.github.ptracker.space;

import com.github.ptracker.entity.Space;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.QueryRequestOptions;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.SpaceCreateRequest;
import com.github.ptracker.service.SpaceDeleteRequest;
import com.github.ptracker.service.SpaceGetRequest;
import com.github.ptracker.service.SpaceGrpc;
import com.github.ptracker.service.SpaceGrpc.SpaceBlockingStub;
import com.github.ptracker.service.SpaceQueryRequest;
import com.github.ptracker.service.SpaceUpdateRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;


public class SpaceClient implements GrpcResource.GrpcClient<String, Space> {
  private final SpaceBlockingStub _blockingStub;

  public SpaceClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public SpaceClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = SpaceGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public Space get(String key, GetRequestOptions options) {
    SpaceGetRequest request = SpaceGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getSpace();
  }

  @Override
  public List<Space> query(Space template, QueryRequestOptions options) {
    SpaceQueryRequest request = SpaceQueryRequest.newBuilder().setTemplate(template).build();
    return _blockingStub.query(request).getSpaceList();
  }

  @Override
  public void create(Space payload, CreateRequestOptions options) {
    SpaceCreateRequest request = SpaceCreateRequest.newBuilder().setSpace(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(Space payload, UpdateRequestOptions options) {
    SpaceUpdateRequest request =
        SpaceUpdateRequest.newBuilder().setSpace(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    SpaceDeleteRequest request = SpaceDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
