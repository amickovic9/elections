package rs.raf.pds.faulttolerance.z1;

import io.grpc.stub.StreamObserver;
import rs.raf.pds.faulttolerance.gRPC.AccountRequest;
import rs.raf.pds.faulttolerance.gRPC.AccountResponse;
import rs.raf.pds.faulttolerance.gRPC.AccountServiceGrpc.AccountServiceImplBase;
import rs.raf.pds.faulttolerance.z1.AccountAppServer.ExecutionStatus;
import rs.raf.pds.faulttolerance.z1.AccountAppServer.OperationStatus;
import rs.raf.pds.faulttolerance.gRPC.RequestStatus;


public class AccountServiceGRPCServer extends AccountServiceImplBase  {

	final AccountAppServer appReplicatedServer;
	
	protected AccountServiceGRPCServer(AccountAppServer server) {
		this.appReplicatedServer = server;
	}
	
	@Override
	public void addAmount(AccountRequest request, StreamObserver<AccountResponse> responseObserver) {
		AccountResponse response = null;
		
		OperationStatus opStatus = 
				appReplicatedServer.addAmount(request.getRequestId(), request.getAmount(), true);
		
		if (ExecutionStatus.UPDATE_REJECTED_NOT_LEADER == opStatus.status()) {
			response = AccountResponse.newBuilder().
						setRequestId(request.getRequestId()).
						setStatus(RequestStatus.UPDATE_REJECTED_NOT_LEADER).
						build();
		}else if (ExecutionStatus.STATUS_OK == opStatus.status()) {
			 
			response = AccountResponse.newBuilder().
						setRequestId(request.getRequestId()).
						setStatus(RequestStatus.STATUS_OK).
						setBalance(opStatus.finalAmount()).
						build();
		}else {
			response = AccountResponse.newBuilder().
						setRequestId(request.getRequestId()).
						setStatus(RequestStatus.UNRECOGNIZED).
						setBalance(opStatus.finalAmount()).
						build();
		}
		
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
	
	@Override
	public void withdrawAmount(AccountRequest request, StreamObserver<AccountResponse> responseObserver) {
		AccountResponse response = null;
		
		OperationStatus opStatus = appReplicatedServer
				.witdrawAmount(request.getRequestId(), request.getAmount(), true);
		
		if (ExecutionStatus.UPDATE_REJECTED_NOT_LEADER == opStatus.status()) {
			response = AccountResponse.newBuilder().
						setRequestId(request.getRequestId()).
						setStatus(RequestStatus.UPDATE_REJECTED_NOT_LEADER).
						build();
		}else if (ExecutionStatus.WITHDRAWAL_REJECT_NOT_SUFFICIENT_AMOUNT == opStatus.status()) {
			
			response = AccountResponse.newBuilder().
						setRequestId(request.getRequestId()).
						setStatus(RequestStatus.WITHDRAWAL_REJECT_NOT_SUFFICIENT_AMOUNT).
						build();
		}
		else if (ExecutionStatus.STATUS_OK == opStatus.status()) {
			response = AccountResponse.newBuilder().
						setRequestId(request.getRequestId()).
						setStatus(RequestStatus.STATUS_OK).
						setBalance(opStatus.finalAmount()).
						build();
		}else {
			response = AccountResponse.newBuilder().
						setRequestId(request.getRequestId()).
						setStatus(RequestStatus.UNRECOGNIZED).
						setBalance(opStatus.finalAmount()).
						build();
		}
		
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
	@Override
	public void getAmount(AccountRequest request, StreamObserver<AccountResponse> responseObserver) {
	    
		float currentAmount = appReplicatedServer.getAmount(request.getRequestId());
		
		AccountResponse response = AccountResponse.newBuilder().
				setRequestId(request.getRequestId()).
				setStatus(RequestStatus.STATUS_OK).
				setBalance(currentAmount).
				build();
	     
	     responseObserver.onNext(response);
		 responseObserver.onCompleted(); 
	}
	
}
