package io.pivotal.pde.sample.gemfire.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * This class exists as a utility for use when working with DataInput DataOutput
 * methods.  It permits reading and writing null values by serializing / reading 
 * an indicator boolean for fields that may be null
 * 
 * @author wmay
 *
 */

public class SerializerUtil {
	public static void writeint(DataOutput out, int val) throws IOException{
		out.writeInt(val);
	}
	
	public static int readint(DataInput in) throws IOException{
		return in.readInt();
	}
	public static void writelong(DataOutput out, long val) throws IOException{
		out.writeLong(val);
	}
	
	public static long readlong(DataInput in) throws IOException{
		return in.readLong();
	}
	
	public static void writeString(DataOutput out, String val) throws IOException{
		out.writeBoolean(val != null);
		if (val != null) out.writeUTF(val);
	}
	
	public static String readString(DataInput in) throws IOException{
		boolean hasVal = in.readBoolean();
		String result = null;
		if (hasVal) result = in.readUTF();
		return result;
	}
}
