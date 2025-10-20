package rs.raf.pds.faulttolerance.core;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class ReplicatedLog {

	public static interface LogReplicator {
		public void replicateOnFollowers(Long entryAtIndex, byte[] data);
	}
	
	Long lastLogEntryIndex = 0L;
	final LogReplicator leaderReplicaNode;
	FileOutputStream fs;
	OutputStreamWriter writer;
	
	public ReplicatedLog(String fileName, LogReplicator node) throws FileNotFoundException {
		this.leaderReplicaNode = node;
		fs = new FileOutputStream(fileName);
		writer = new OutputStreamWriter(fs);
	}
		
	public synchronized void appendAndReplicate(byte[] commandBytes) throws IOException {
		Long lastLogEntryIndex = appendToLocalLog(commandBytes);
		leaderReplicaNode.replicateOnFollowers(lastLogEntryIndex, commandBytes);  
	}
	
	public Long appendToLocalLog(byte[] data) throws IOException {
		String s = new String(data);
		System.out.println("Log #"+lastLogEntryIndex+":"+s);
		
		//fs.write(data);
		//fs.flush();
		writer.write(s);writer.write("\r\n");
		writer.flush();
		fs.flush();
		
		return ++lastLogEntryIndex;
	}

	public Long getLastLogEntryIndex() {
		return lastLogEntryIndex;
	}
	
}
