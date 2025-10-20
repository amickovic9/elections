package rs.raf.pds.faulttolerance.z1;

import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import rs.raf.pds.faulttolerance.core.ReplicaNode;
import rs.raf.pds.faulttolerance.core.SyncPrimitive;
import rs.raf.pds.faulttolerance.gRPC.AccountRequest;
import rs.raf.pds.faulttolerance.gRPC.AccountRequestType;
import rs.raf.pds.faulttolerance.gRPC.AccountResponse;
import rs.raf.pds.faulttolerance.gRPC.AccountServiceGrpc;
import rs.raf.pds.faulttolerance.gRPC.RequestStatus;

public class AppClient extends SyncPrimitive {
	
	final String appRoot;
	String leaderNodeName = null;
	String leaderHostNamePort;
	Object zkNotifier = new Object();
	
	ManagedChannel channel = null;
	AccountServiceGrpc.AccountServiceBlockingStub blockingStub = null;
			
	protected AppClient(String zkAddress, String appRoot) throws KeeperException, InterruptedException {
		super(zkAddress);
		this.appRoot = appRoot;
	}
	@Override
	public void process(WatchedEvent event) {
		System.out.println("Stigla notifikacija od ZooKeepera!");
		try {
			synchronized(zkNotifier) {
				zkNotifier.notify();
			}
			//checkLeader();
		} catch(Exception e) {//catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	protected void newLeaderAwaiting() throws KeeperException, InterruptedException {
		System.out.println("Trazenje novog lidera!");
    	
		synchronized(zkNotifier) {
    		zkNotifier.wait();
    	}
    	checkLeader();
	}
    public synchronized void checkLeader() throws KeeperException, InterruptedException {
		//Thread.sleep(100);
    	List<String> list = zk.getChildren(appRoot, false);
        System.out.println("There are total:"+list.size()+ " replicas for elections!");
        for (int i=0; i<list.size(); i++) 
        	System.out.print("NODE:"+list.get(i)+", ");
        System.out.println();
        
        if (list.size() == 0) {
            System.out.println("0 Elemenata ? ");
        } else {
            Integer minValue = Integer.parseInt(list.get(0).substring(ReplicaNode.REPLICA_NODE_SEQUENCE_INDEX));
            String minNodeName = list.get(0);
            
            for(int i=1; i<list.size(); i++) {
            	Integer tempValue = Integer.parseInt(list.get(i).substring(ReplicaNode.REPLICA_NODE_SEQUENCE_INDEX));
                if(minValue > tempValue) {
                  minValue = tempValue;
                  minNodeName = list.get(i);
                }
            }
            if (leaderNodeName == null || !minNodeName.equals(leaderNodeName)) {
            	leaderNodeName = minNodeName;
            	byte[] b = zk.getData(appRoot + "/" + leaderNodeName, true, null);
            	leaderHostNamePort = new String(b);
            	
            	System.out.println("Leader je "+leaderNodeName);
            	
            	//if (channel != null)
            	//	channel.shutdown();
            	
            	blockingStub = getBlockingStub(leaderHostNamePort);
            }
        }
	}
    public  AccountServiceGrpc.AccountServiceBlockingStub getBlockingStub(String hostNamePort){
		String[] splits = hostNamePort.split(":");
		channel = ManagedChannelBuilder.forAddress(splits[0], Integer.parseInt(splits[1]))
	          .usePlaintext()
	          .build();

	    return AccountServiceGrpc.newBlockingStub(channel);
	}
	private void inviteServer() throws KeeperException, InterruptedException {
		for (int i=0; i<10; i++) {
	        try { 
	        	System.out.println("Iteracija #"+i);
	        	
	        	inviteServerFunctions(blockingStub);
	        		
	        	Thread.sleep(100);
	        	
	        	System.out.println("Leader je "+leaderNodeName);
	        
	        }catch(RuntimeException e) {
	        	System.out.println("ERROR - Server has crashed! Exception:"+e.getMessage());
	        	//e.printStackTrace();
	        	//channel.shutdown();
	        	//Thread.sleep(300);
	        	newLeaderAwaiting();
      	
	        } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				System.out.println("ERROR! Exception:"+e.getMessage());
	        	//e.printStackTrace();
			}
		}
         
        channel.shutdown();
	}
	public static void main(String[] args) {
        
		if (args.length != 1) {
			System.out.println("Usage java -cp PDS-FT1-1.0.jar;.;lib/* rs.raf.pds.faulttolerance.AppClient <zookeeper_server_host:port>");
		    System.exit(1);
		}
		
		AppClient client;
		try {
			client = new AppClient(args[0], AccountAppServer.APP_ROOT_NODE);
			client.checkLeader();
			client.inviteServer();
			
			
		} catch (KeeperException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
    }
	
	
	private void inviteServerFunctions(AccountServiceGrpc.AccountServiceBlockingStub blockingStub) {
		// Poziv AppServer-a preko gRPC-a
        System.out.println("Prvi poziv je getAmount()");
        AccountRequest request = AccountRequest.newBuilder()
        			.setRequestId(1)
        			.setOpType(AccountRequestType.GET)
        			.build();
        
        AccountResponse response;
        
        synchronized(this) {
        	response = blockingStub.getAmount(request);
        }
        ispisResponse(response, request);
        
        
         
        System.out.println("Drugi poziv je addAmount()");
        request = AccountRequest.newBuilder()
        			.setRequestId(2)
        			.setOpType(AccountRequestType.ADD)
        			.setAmount(150.0f)
        			.build();
        synchronized(this) {        
        	response = blockingStub.addAmount(request);
        }
        ispisResponse(response, request);
        
        
        
        System.out.println("Treci poziv je witdrawAmount()");
        request = AccountRequest.newBuilder()
        			.setRequestId(2)
        			.setOpType(AccountRequestType.WITHDRAWAL)
        			.setAmount(70.0f)
        			.build();
                
        synchronized(this) {  
        	response = blockingStub.withdrawAmount(request);
        }
        ispisResponse(response, request);
        
        
        System.out.println("Cetvrti poziv je witdrawAmount()");
        request = AccountRequest.newBuilder()
        			.setRequestId(2)
        			.setOpType(AccountRequestType.WITHDRAWAL)
        			.setAmount(100.0f)
        			.build();
        synchronized(this) {        
        	response = blockingStub.withdrawAmount(request);
        }
        ispisResponse(response, request);
	}
	
	public static void ispisResponse(AccountResponse response, AccountRequest request) {
		if (response.getStatus() == RequestStatus.STATUS_OK) {
			System.out.println("STATUS OK! REQUEST = "+request.getOpType()+ ", AMOUNT = "+request.getAmount()+", RESULT AMOUNT = "+response.getBalance());
		}else if (response.getStatus() == RequestStatus.UPDATE_REJECTED_NOT_LEADER) {
			System.out.println("UPDATE_REJECTED_NOT_LEADER! "+request.getOpType().toString());
		}
		else if (response.getStatus() == RequestStatus.WITHDRAWAL_REJECT_NOT_SUFFICIENT_AMOUNT) { 
			System.out.println("WITHDRAWAL_REJECT_NOT_SUFFICIENT_AMOUNT! Amount = "+request.getAmount());
		}
		
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
