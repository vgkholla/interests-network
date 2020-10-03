package com.github.ptracker.account;

import com.github.ptracker.entity.Account;
import com.github.ptracker.resource.CreateRequestOptions;
import com.github.ptracker.resource.DeleteRequestOptions;
import com.github.ptracker.resource.GetRequestOptions;
import com.github.ptracker.resource.GrpcResource;
import com.github.ptracker.resource.UpdateRequestOptions;
import com.github.ptracker.service.AccountCreateRequest;
import com.github.ptracker.service.AccountDeleteRequest;
import com.github.ptracker.service.AccountGetRequest;
import com.github.ptracker.service.AccountGrpc;
import com.github.ptracker.service.AccountGrpc.AccountBlockingStub;
import com.github.ptracker.service.AccountUpdateRequest;
import io.grpc.ManagedChannelBuilder;


public class AccountClient implements GrpcResource.GrpcClient<String, Account> {
  private final AccountBlockingStub _blockingStub;

  public AccountClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  public AccountClient(ManagedChannelBuilder<?> channelBuilder) {
    _blockingStub = AccountGrpc.newBlockingStub(channelBuilder.build());
  }

  @Override
  public Account get(String key, GetRequestOptions options) {
    AccountGetRequest request = AccountGetRequest.newBuilder().setId(key).build();
    return _blockingStub.get(request).getAccount();
  }

  @Override
  public void create(Account payload, CreateRequestOptions options) {
    AccountCreateRequest request = AccountCreateRequest.newBuilder().setAccount(payload).build();
    _blockingStub.create(request);
  }

  @Override
  public void update(Account payload, UpdateRequestOptions options) {
    AccountUpdateRequest request =
        AccountUpdateRequest.newBuilder().setAccount(payload).setShouldUpsert(options.shouldUpsert()).build();
    _blockingStub.update(request);
  }

  @Override
  public void delete(String key, DeleteRequestOptions options) {
    AccountDeleteRequest request = AccountDeleteRequest.newBuilder().setId(key).build();
    _blockingStub.delete(request);
  }
}
