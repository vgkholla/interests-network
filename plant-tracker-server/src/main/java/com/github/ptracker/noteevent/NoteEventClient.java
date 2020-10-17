package com.github.ptracker.noteevent;

import com.github.ptracker.entity.NoteEvent;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.QueryRequestOptions;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.NoteEventCreateRequest;
import com.github.ptracker.service.NoteEventDeleteRequest;
import com.github.ptracker.service.NoteEventGetRequest;
import com.github.ptracker.service.NoteEventGrpc;
import com.github.ptracker.service.NoteEventGrpc.NoteEventBlockingStub;
import com.github.ptracker.service.NoteEventQueryRequest;
import com.github.ptracker.service.NoteEventUpdateRequest;
import io.grpc.ManagedChannelBuilder;
import java.util.List;


public class NoteEventClient implements GrpcResource.GrpcClient<String, NoteEvent> {
  private final NoteEventBlockingStub _blockingStub;

  public NoteEventClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public NoteEventClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = NoteEventGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public NoteEvent get(String key, GetRequestOptions options) {
    NoteEventGetRequest request = NoteEventGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getNoteEvent();
  }

  @Override
  public List<NoteEvent> query(NoteEvent template, QueryRequestOptions options) {
    NoteEventQueryRequest request = NoteEventQueryRequest.newBuilder().setTemplate(template).build();
    return _blockingStub.query(request).getNoteEventList();
  }

  @Override
  public void create(NoteEvent payload, CreateRequestOptions options) {
    NoteEventCreateRequest request = NoteEventCreateRequest.newBuilder().setNoteEvent(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(NoteEvent payload, UpdateRequestOptions options) {
    NoteEventUpdateRequest request =
        NoteEventUpdateRequest.newBuilder().setNoteEvent(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    NoteEventDeleteRequest request = NoteEventDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
