package rs.raf.pds.faulttolerance.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import rs.raf.pds.faulttolerance.gRPC.LeaderInfo;
import rs.raf.pds.faulttolerance.gRPC.LeaderRequest;
import rs.raf.pds.faulttolerance.gRPC.LogEntry;
import rs.raf.pds.faulttolerance.gRPC.LogResponse;
import rs.raf.pds.faulttolerance.gRPC.LogStatus;
import rs.raf.pds.faulttolerance.gRPC.ReplicatedLogServiceGrpc;
import rs.raf.pds.faulttolerance.gRPC.ReplicatedLogServiceGrpc.ReplicatedLogServiceImplBase;


import java.util.concurrent.ExecutorService;

public class ReplicaNode extends ReplicatedLogServiceImplBase implements Runnable, ReplicatedLog.LogReplicator{

		public static interface LogCommandExecutor {
			
			/**
			 *  Treba da se implementira u klasi servera replike.
			 *  Pratioc izvrsava komandu dobijenu od lidera!
			 * @param data
			 */
			public abstract void executeReplicatedLogCommand(byte[] data);
		}
	
		public static final String REPLICA_NODE_NAME ="/candid";
		public static final int REPLICA_NODE_SEQUENCE_INDEX= REPLICA_NODE_NAME.length()-1; 
		
		public enum State {FOLLOWER, LEADER};
		
		final String root;
		final ZooKeeper zk;
		final Integer zkNotifier = Integer.valueOf(-1);
		
		int myId = -1;
		volatile State myState = State.FOLLOWER;
		final String myGRPCAddress; 
		
		Map<String, FollowerGRPCChannel> followersChannelMap = new HashMap<String, FollowerGRPCChannel>();
		String leaderGRPCAddress = null;
		
		protected ReplicatedLog replicatedLog;
		
		LogCommandExecutor logCommandExecutor = null;
		ExecutorService commandThreadPoolExecutor = Executors.newSingleThreadExecutor();
			
		
		volatile boolean running = false;
		private Thread thread = null;
		
		
		public ReplicaNode(String zkAddress, String zkRoot, String myGRPCAddress, String logFileName, LogCommandExecutor logCommandExecutor) throws FileNotFoundException {
			this.logCommandExecutor = logCommandExecutor;
						
			this.root = zkRoot;
			this.myGRPCAddress = myGRPCAddress;
			
			initReplicatedLog(logFileName);
			
			SyncPrimitive sp = new SyncPrimitive(zkAddress, zkNotifier);
			this.zk = sp.getZk();
			
			
			// Create membership node
	        if (zk != null) {
	            try {
	                Stat s = zk.exists(zkRoot, false);
	                if (s == null) {
	                    zk.create(zkRoot, new byte[0], Ids.OPEN_ACL_UNSAFE,
	                            CreateMode.PERSISTENT);
	                }
	               
	               // Kreira svoj čvor gde je value hostName:grpcPort kako bi mu pristupao lider
	               String myNodeName = zk.create(zkRoot + REPLICA_NODE_NAME, myGRPCAddress.getBytes(), Ids.OPEN_ACL_UNSAFE,
	                        CreateMode.EPHEMERAL_SEQUENTIAL);
	               
	               System.out.println("My Node election name:"+myNodeName);
	               int tempIndex = myNodeName.indexOf(REPLICA_NODE_NAME)+REPLICA_NODE_NAME.length();
	               this.myId = Integer.parseInt(myNodeName.substring(tempIndex));
	               
	               System.out.println("Node election ID = "+myId);
	                
	            } catch (KeeperException e) {
	                System.out
	                        .println("Keeper exception when instantiating queue: "
	                                + e.toString());
	            } catch (InterruptedException e) {
	                System.out.println("Interrupted exception");
	            }
	        }
		}
		protected void initReplicatedLog(String logFileName) throws FileNotFoundException {
			this.replicatedLog = new ReplicatedLog(logFileName, this);
		}
		
		public ReplicatedLog getReplicatedLog() {
			return replicatedLog;
		}
		protected void setLeader(List<String> nodeList) throws KeeperException, InterruptedException {
			
			myState = State.LEADER;
			setFollowersGRPCChannels(nodeList);
			
			// Lider prati da li se pojavila nova replika-čvor ili je neki od replika-pratilaca krahirala!
			// To će znati kada nova replika-server kreira svoj čvor potomak na ZooKeeper čvoru - korenu,
			// ili nestane čvor potomak na ZooKeeperu kreiran od postojećeg replike-pratioca.
			// Lider zato postavlja Watcher-a na koreni cvor da dobije notifikaciju kada se desi jedan od ta dva dogadjaja!
						
			zk.getChildren(root, true);
						
			System.out.println("JA SAM LIDER!");
		}
		/*
		 * Lider izvrsava ovu metodu da bi kreirao gRPC kanal ka svim pratiocima.
		 * Ako je pre toga imao vec kreiran kanal ka nekom pratiocu, koristice isti i dalje.
		 * Ulazni parametar je lista potomaka cvora korena /account koji su kreirali serveri replike!
		 */
		protected void setFollowersGRPCChannels(List<String> nodeList) {
			Map<String, FollowerGRPCChannel> oldMap = followersChannelMap;
			followersChannelMap = new HashMap<String, FollowerGRPCChannel>();
			
			// Prolazi se kroz listu cvorova potomaka od 1-od indexa, jer je liderov cvor potomak za i=0!
			for (int i=1; i<nodeList.size();i++) {
				String nodeName = nodeList.get(i);
				FollowerGRPCChannel followerChannel = oldMap.get(nodeName);
				try {
					if (followerChannel == null) {
						
						byte[] b = zk.getData(root + "/" + nodeName, false, null);
						String grpcConnection = new String(b);
						String[] tokens = grpcConnection.split(":");
						ManagedChannel channel = ManagedChannelBuilder.forAddress(tokens[0], Integer.parseInt(tokens[1]))
						          .usePlaintext()
						          .build();

						ReplicatedLogServiceGrpc.ReplicatedLogServiceBlockingStub blockingStub = ReplicatedLogServiceGrpc.newBlockingStub(channel);
						followerChannel = new FollowerGRPCChannel(nodeName, grpcConnection, blockingStub);
						
						
					} // Inace ce se uzeti followerChannel iz oldMap
						
					followersChannelMap.put(nodeName, followerChannel);
				
				} catch (KeeperException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			oldMap.clear();	// Brisanje sadrzaja stare mape sa gRPC informacijama, da ne bude memory leak!	
		}
		
		public void logAndReplicateCommandToFollowers(byte[] commandInBytes) throws IOException {
			replicatedLog.appendAndReplicate(commandInBytes);
		}
		
		/*
		 * Lider salje komandu ka pratiocima da je replikuju u svom logu! 
		 * 
		 */
		@Override
		public void replicateOnFollowers(Long entryAtIndex, byte[] data) {
			// Lider salje svim pratiocima novu komandu/zahtev da upisu u svoj log!
			
			LogEntry logEntry = LogEntry.newBuilder()
								.setEntryAtIndex(entryAtIndex)
								.setLogEntryData(ByteString.copyFrom(data))
								.build();
			
			for (FollowerGRPCChannel grpcChannel:followersChannelMap.values()) {
				grpcChannel.getBlockingStub().appendLog(logEntry);
			}
			
		}
		/**
		 * Replika-server izvrsava ovu gRPC metodu kada mu lider posalje novu komandu da upise kod sebe u log i 
		 * izvrsi je!
		 */
		@Override
		public void appendLog(LogEntry request, StreamObserver<LogResponse> responseObserver) {
		
			
			byte[] commandBytes = request.getLogEntryData().toByteArray();
			Long entryIndex = request.getEntryAtIndex();
			LogResponse response;
			
			if (replicatedLog.getLastLogEntryIndex()<(entryIndex-1)) {
				response = LogResponse.newBuilder().
										setStatus(LogStatus.LOG_HASNT_LAST_ENTRY).
										setLastEntryIndex(replicatedLog.getLastLogEntryIndex()).
										setEntryAtIndex(entryIndex).
										build();
			}
			else { 
				try { 
					replicatedLog.appendToLocalLog(commandBytes);
					
					// Izvrsavanje replikovane komande u posebnom threadu i to na AppServeru!
					// Razlog: da se odmah vrati potvrda lideru da je komanda primljena, 
					// a za to vreme da se ona izvrsava! 
					commandThreadPoolExecutor.submit(()->logCommandExecutor.executeReplicatedLogCommand(commandBytes));
					
					response = LogResponse.newBuilder().
							setStatus(LogStatus.LOG_OK).
							setEntryAtIndex(entryIndex).
							build();
					
				}catch(IOException e) {
					response = LogResponse.newBuilder().
							setStatus(LogStatus.IO_ERROR).
							setEntryAtIndex(entryIndex).
							build();
				}
			}
			// Vracanje rezultata izvrsavanja lideru!
			
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		}
		@Override
		public void getLeaderInfo(LeaderRequest req, StreamObserver<LeaderInfo> response) {
			LeaderInfo leader = null;
			if (isLeader()) {
				leader = LeaderInfo.newBuilder().
						  setImLeader(true).
						  setHostnamePort(getMyGRPCAddress()).
						  build();
			}
			else {
				leader = LeaderInfo.newBuilder().
						  setImLeader(false).
						  setHostnamePort(leaderGRPCAddress).
						  build();
			}
			response.onNext(leader);
			response.onCompleted();
		}
		
		private void checkReplicaCandidate() throws KeeperException, InterruptedException {
			List<String> list = zk.getChildren(root, false);
	        System.out.println("There are total:"+list.size()+ " replicas for elections!");
	        for (int i=0; i<list.size(); i++) 
	        	System.out.print("NODE:"+list.get(i)+", ");
	        System.out.println();
	        
	        if (list.size() == 0) {
	            System.out.println("0 Elemenata ? A ja ??? ");
	           // mutex.wait();
	        } else {
	            Collections.sort(list);
	            int myIndex = -1;
	            
	            for(int i=0; i<list.size(); i++) {
	            	Integer tempValue = Integer.parseInt(list.get(i).substring(REPLICA_NODE_SEQUENCE_INDEX));
	                if(myId == tempValue) {
	                   myIndex = i;
	                   break;
	                }
	            }
	            if (myIndex == 0) {
	            	System.out.println("Priprema za postavku lidera!");
	            	setLeader(list);
	            }
	            else {
	            	String totalLeader = list.get(0);
	            	byte[] b = zk.getData(root + "/" + totalLeader, false, null);
	            	leaderGRPCAddress = new String(b);
	            	
	            	int prevNodeIndex = myIndex-1;		
	            	while(true) {
		            	String myNodeToWatch = list.get(prevNodeIndex);	// Pošto nije lider, treba pratiti čvor koji prethodi po sekvenci
		            	// byte[] b = zk.getData(root + "/" + myLeaderNodeToWatch, true, null);
						Stat stat = zk.exists(root + "/" + myNodeToWatch, true);	// Postavljanje Watcher-a na taj node
		            	if (stat == null)
		            		// Čvor ne postoji (u medjuvremenu je krahirao) treba pratiti prvog pre njega 
		            		if (prevNodeIndex == 0) { // Da li je hteo da prati lidera (a lider krahirao)?
		            			setLeader(list);	// Onda ovaj server postaje lider
		            			break;
		            		}
		            		else 
		            			prevNodeIndex--;
		            	else {
		            		System.out.println("Pratim cvor "+myNodeToWatch);
		            		break;	// Sve je ok, postavljen je Watcher na prethodni čvor!
		            	}
		            }
	            	
	            }
	        }
		}
		public void leaderElection() throws KeeperException, InterruptedException {
			checkReplicaCandidate();
		}
		
		@Override
		public void run() {
			while(running) {
				synchronized(zkNotifier) {
				   try {
					zkNotifier.wait();
					System.out.println("Stigla notifikacija promene configuracije");
					checkReplicaCandidate();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				  
				}

			}
			
		}
		public void start() {
			if (!running) {
				thread = new Thread(this, "Node");
				running = true;
				thread.start();
			}
		}
		public void stop() {
			Thread stopThread = thread;
			thread = null;
			running = false;
			stopThread.interrupt();
			
		}
		public boolean isLeader() {
			return myState == State.LEADER;
		}
		public String getMyGRPCAddress() {
			
			return myGRPCAddress;
		}
		public String getLeaderGRPCAddress() {
			return leaderGRPCAddress;
		}
				
}
