package rs.raf.pds.faulttolerance.z1.commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import rs.raf.pds.faulttolerance.z1.commands.Command.CommandType;

public class GetCommand extends Command{

	protected Float value;
	
	public GetCommand() {
		
	}
	
	@Override
	public void serialize(DataOutputStream os) throws IOException {
      os.writeUTF(CommandType.GET.toString());
    }
	
	@Override 
	public String writeToString() {
		StringBuffer sb = new StringBuffer();
		sb.append(CommandType.GET.toString());
		
		return sb.toString();
	}

	public Float getValue() {
		return value;
	}

	public void setValue(Float value) {
		this.value = value;
	}
	
	
}
