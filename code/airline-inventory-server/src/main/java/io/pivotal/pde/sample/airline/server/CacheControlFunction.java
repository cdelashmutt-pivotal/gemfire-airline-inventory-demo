package io.pivotal.pde.sample.airline.server;

import java.util.Properties;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;

/**
 *  should be run on all members of the distributed system
 *  ( RegionService.onMembers(ds)  )
 * 
 * input is a single command
 *  valid commands are
 *  	readonly
 *  	readwrite
 * 
 * @author wmay
 *
 */
public class CacheControlFunction implements Function, Declarable {

	public static String SERVER_MODE_SYSTEM_PARAM="server_mode";
	public static String SERVER_MODE_RW = "RW";
	public static String SERVER_MODE_RO = "RO";
	public static String COMMAND_READONLY = "readonly";
	public static String COMMAND_READWRITE = "readwrite";
	
	
	private static final long serialVersionUID = -5424555919674820317L;

	@Override
	public void init(Properties props) {
	}

	@Override
	public void execute(FunctionContext ctx) {
		RegionFunctionContext rctx = (RegionFunctionContext) ctx;
		
		Region systemParameterRegion = CacheFactory.getAnyInstance().getRegion("SystemParameter");
		String cmd = (String) ctx.getArguments();
		if (cmd.equals(COMMAND_READONLY)) {
			systemParameterRegion.put(SERVER_MODE_SYSTEM_PARAM, SERVER_MODE_RO);
			CacheFactory.getAnyInstance().getLogger().info("setting server mode to READ ONLY");
		} else if (cmd.equals(COMMAND_READWRITE)){
			systemParameterRegion.put(SERVER_MODE_SYSTEM_PARAM, SERVER_MODE_RW);
			CacheFactory.getAnyInstance().getLogger().info("setting server mode to READ/WRITE");
		} else {
			CacheFactory.getAnyInstance().getLogger().warning("CacheControlFunction encountered unrecognized command: " + cmd);
		}
	}

	@Override
	public String getId() {
		return CacheControlFunction.class.getSimpleName();
	}

	@Override
	public boolean hasResult() {
		return false;
	}

	@Override
	public boolean isHA() {
		return false;
	}

	@Override
	public boolean optimizeForWrite() {
		return false;
	}

}
