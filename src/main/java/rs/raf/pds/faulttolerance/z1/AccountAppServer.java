package rs.raf.pds.faulttolerance.z1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Scanner;

import org.apache.zookeeper.KeeperException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import rs.raf.pds.faulttolerance.core.ReplicaNode;
import rs.raf.pds.faulttolerance.core.ReplicatedLog;
import rs.raf.pds.faulttolerance.gRPC.AccountResponse;
import rs.raf.pds.faulttolerance.gRPC.LogResponse;
import rs.raf.pds.faulttolerance.gRPC.LogStatus;
import rs.raf.pds.faulttolerance.gRPC.RequestStatus;
import rs.raf.pds.faulttolerance.z1.commands.AddValueCommand;
import rs.raf.pds.faulttolerance.z1.commands.Command;
import rs.raf.pds.faulttolerance.z1.commands.SubValueCommand;

public class AccountAppServer implements ReplicaNode.LogCommandExecutor{ 
	
	public static final String APP_ROOT_NODE ="/account";
	
	public enum ExecutionStatus {STATUS_OK, INTERNAL_ERROR, LOG_ERROR, UPDATE_REJECTED_NOT_LEADER, WITHDRAWAL_REJECT_NOT_SUFFICIENT_AMOUNT};
	public record OperationStatus(int requestId, ExecutionStatus status, float finalAmount) implements Serializable {};
	
	ReplicaNode myReplicaNode = null;
	AccountServiceImpl accountService;
	
	public AccountAppServer(AccountServiceImpl accountService, String zkAddress, String zkRoot, String myGRPCAddress, String logFileName) throws FileNotFoundException{
		this.accountService = accountService;
		this.myReplicaNode = new ReplicaNode(zkAddress, zkRoot, myGRPCAddress, logFileName, this);
		
	}
	
	protected ReplicaNode getReplicaNode() {
		return myReplicaNode;
	}
	/**
	 * Implementacija interfejsa ReplicaNode.LogCommandExecutor.
	 * Izvrsava se od strane pratioca-replike kada primi komandu od lidera, a nakon sto je upise u log!	
	 */
	@Override	
	public void executeReplicatedLogCommand(byte[] commandBytes) {
		
		//DataInputStream ds = new DataInputStream(new ByteArrayInputStream(data));
		Scanner sc = new Scanner(new String(commandBytes));
		
		//int commandType = sc.nextInt();
		String commandType = sc.next();
		
		if (Command.CommandType.ADD.equals(commandType)) {
			//AddValueCommand command = AddValueCommand.deserialize(ds);
			AddValueCommand command = new AddValueCommand(sc.nextFloat());
			addAmount(-1, command.getValue(), false);	// false - znaci da ga izvrsava pratioc replika i da ne upisuje u log i vrsi replikaciju!
			
		}else if (Command.CommandType.SUB.equals(commandType)) {
			//SubValueCommand command = SubValueCommand.deserialize(ds);
			SubValueCommand command = new SubValueCommand(sc.nextFloat());
			witdrawAmount(-1, command.getValue(), false); // false - znaci da ga izvrsava pratioc replika i da ne upisuje u log i vrsi replikaciju!
		}
	
	}
	
	public OperationStatus addAmount(int requestId, float amount, boolean asLeaderToExecute) {
		
		if (asLeaderToExecute && !myReplicaNode.isLeader()) {
			return new OperationStatus(requestId, ExecutionStatus.UPDATE_REJECTED_NOT_LEADER, -1);
		}
		if (asLeaderToExecute) {
			// Lider treba da upise u log i replikuje komandu ka pratiocima
			
			AddValueCommand command = new AddValueCommand(amount);
			try {
				myReplicaNode.getReplicatedLog().appendAndReplicate(command.writeToString().getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new OperationStatus(requestId, ExecutionStatus.LOG_ERROR, -1);
			}
		}
		
		float resultAmount = accountService.addAmount(amount);
		
		return new OperationStatus(requestId, ExecutionStatus.STATUS_OK, resultAmount);
		
	}
	public OperationStatus witdrawAmount(int requestId, float amount, boolean leaderToExecute) {
		if (leaderToExecute && !myReplicaNode.isLeader()) {
			return new OperationStatus(requestId, ExecutionStatus.UPDATE_REJECTED_NOT_LEADER, -1);
		}
		if (leaderToExecute) { 
			// Lider treba da upise u log i replikuje komandu ka pratiocima
			
			SubValueCommand command = new SubValueCommand(amount);
			try {
				myReplicaNode.getReplicatedLog().appendAndReplicate(command.writeToString().getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return new OperationStatus(requestId, ExecutionStatus.LOG_ERROR, -1);
			}
		}
		
		float resultAmount = accountService.witdrawAmount(amount);
		
		if (resultAmount<0) {
			return new OperationStatus(requestId, ExecutionStatus.WITHDRAWAL_REJECT_NOT_SUFFICIENT_AMOUNT, -1);
		}
		else {
			return new OperationStatus(requestId, ExecutionStatus.STATUS_OK, resultAmount);
		}
	}
	
	public float getAmount(int requestId) {
		return accountService.getAmount();
	}
	
	public static void main(String[] args) throws IOException, InterruptedException {
        
		 if (args.length != 3) {
				System.out.println("Usage java -cp PDS-FT1-1.0.jar;.;lib/* rs.raf.pds.faulttolerance.z1.AccountAppServer <zookeeper_server_host:port> <gRPC_port> <log_file_name>");
			    System.exit(1);
		 }
		
		 String zkConnectionString = args[0];
		 int gRPCPort = Integer.parseInt(args[1]);
		 String logFileName = args[2];
		 
		 String myGRPCaddress = InetAddress.getLocalHost().getHostName()+":"+gRPCPort;
		 
		 AccountServiceImpl accService = new AccountServiceImpl();
		 AccountAppServer appServer = new AccountAppServer(accService, zkConnectionString, APP_ROOT_NODE, myGRPCaddress, logFileName);
		 
		 
		Server gRPCServer = ServerBuilder
				.forPort(gRPCPort)
				.addService(new AccountServiceGRPCServer(appServer)) // 'AccountServiceGrpc'
				.addService(appServer.getReplicaNode())				//  'ReplicatedLogServiceGrpc'
				.build();
		

       gRPCServer.start();
       

             
       try{
    	   appServer.getReplicaNode().leaderElection();
    	   appServer.getReplicaNode().start();
	        	        	        
	       gRPCServer.awaitTermination();
	        
	       appServer.getReplicaNode().stop();
	        
	     } catch (KeeperException e){

	     } catch (InterruptedException e){

	     }

   }
	
		
}
