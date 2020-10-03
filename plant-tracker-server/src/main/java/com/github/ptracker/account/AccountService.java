package com.github.ptracker.account;

import com.github.ptracker.entity.Account;
import com.github.ptracker.resource.CreateRequestOptionsImpl;
import com.github.ptracker.resource.DeleteRequestOptionsImpl;
import com.github.ptracker.resource.GetRequestOptionsImpl;
import com.github.ptracker.resource.Resource;
import com.github.ptracker.resource.ResourceResponse;
import com.github.ptracker.resource.ResponseStatus;
import com.github.ptracker.resource.UpdateRequestOptionsImpl;
import com.github.ptracker.service.AccountCreateRequest;
import com.github.ptracker.service.AccountCreateResponse;
import com.github.ptracker.service.AccountDeleteRequest;
import com.github.ptracker.service.AccountDeleteResponse;
import com.github.ptracker.service.AccountGetRequest;
import com.github.ptracker.service.AccountGetResponse;
import com.github.ptracker.service.AccountGrpc.AccountImplBase;
import com.github.ptracker.service.AccountUpdateRequest;
import com.github.ptracker.service.AccountUpdateResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

import static com.google.common.base.Preconditions.*;


public class AccountService extends AccountImplBase {
  private final Resource<String, Account> _accountResource;

  public AccountService(Resource<String, Account> accountResource) {
    _accountResource = checkNotNull(accountResource, "Account Resource cannot be null");
  }

  @Override
  public void get(AccountGetRequest request, StreamObserver<AccountGetResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Account ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Optional<Account>> response =
          _accountResource.get(request.getId(), new GetRequestOptionsImpl.Builder().build());
      if (response.getPayload().isPresent()) {
        responseObserver.onNext(AccountGetResponse.newBuilder().setAccount(response.getPayload().get()).build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(
            new StatusRuntimeException(Status.NOT_FOUND.augmentDescription("Did not find " + request.getId())));
      }
    }
  }

  @Override
  public void create(AccountCreateRequest request, StreamObserver<AccountCreateResponse> responseObserver) {
    if (!request.hasAccount()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Account is missing")));
    } else {
      ResourceResponse<Void> createResponse = _accountResource.create(request.getAccount(), new CreateRequestOptionsImpl());
      if (ResponseStatus.OK.equals(createResponse.getStatus())) {
        responseObserver.onNext(AccountCreateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(createResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void update(AccountUpdateRequest request, StreamObserver<AccountUpdateResponse> responseObserver) {
    if (!request.hasAccount()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Account is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> updateResponse = _accountResource.update(request.getAccount(),
          new UpdateRequestOptionsImpl.Builder().shouldUpsert(request.getShouldUpsert()).build());
      if (ResponseStatus.OK.equals(updateResponse.getStatus())) {
        responseObserver.onNext(AccountUpdateResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(updateResponse.getStatus().getGrpcStatus()));
      }
    }
  }

  @Override
  public void delete(AccountDeleteRequest request, StreamObserver<AccountDeleteResponse> responseObserver) {
    if (request.getId() == null || request.getId().isEmpty()) {
      responseObserver.onError(
          new StatusRuntimeException(Status.FAILED_PRECONDITION.augmentDescription("Account ID is missing")));
    } else {
      // TODO: add metadata
      ResourceResponse<Void> deleteResponse =
          _accountResource.delete(request.getId(), new DeleteRequestOptionsImpl.Builder().build());
      if (ResponseStatus.OK.equals(deleteResponse.getStatus())) {
        responseObserver.onNext(AccountDeleteResponse.newBuilder().build());
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(new StatusRuntimeException(deleteResponse.getStatus().getGrpcStatus()));
      }
    }
  }
}
