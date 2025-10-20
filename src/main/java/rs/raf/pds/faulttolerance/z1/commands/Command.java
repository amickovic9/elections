package rs.raf.pds.faulttolerance.z1.commands;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public abstract class Command implements Serializable{

	public enum CommandType { ADD, SUB, GET }
	
	public void serialize(DataOutputStream os) throws IOException {
		
	}
	
	public abstract String writeToString();		
	
}
	

