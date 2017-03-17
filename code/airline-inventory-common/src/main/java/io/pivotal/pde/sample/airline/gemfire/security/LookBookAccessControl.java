package io.pivotal.pde.sample.airline.gemfire.security;

import java.security.Principal;
import java.util.Iterator;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.operations.ExecuteFunctionOperationContext;
import org.apache.geode.cache.operations.OperationContext;
import org.apache.geode.cache.operations.QueryOperationContext;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.security.AccessControl;
import org.apache.geode.security.NotAuthorizedException;

public class LookBookAccessControl implements AccessControl {

	private static String FLIGHT_REGION = "Flight";
	private static String FLIGHT_AVAIL_REGION = "FlightAvailability";
	
	private SimpleFilePrincipal principal;
	
	public static AccessControl create() { return new LookBookAccessControl();}
	
	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean authorizeOperation(String regionName, OperationContext op) {
		
		// if we are modifying anything but Flight or FlightAvailability - must be system
		
		// if we are reading anything but Flight or FlightAvailability - OK
		
		// if we are modifying Flight or FlightAvailability - must have "book"
		
		// if we are reading Flight or FlightAvailability - must have "look"
		
		boolean result = false;
		
		if (op.getOperationCode().isQuery()){
			QueryOperationContext qctx = (QueryOperationContext) op;
			
			// if the query contains anything other than Flight and FlightAvailability 
			// we require system, otherwise, need look
			boolean onlyFlightOrFlightAvail = true;
			Iterator<String> regionNames = qctx.getRegionNames().iterator();
			String rname;
			while(regionNames.hasNext()){
				rname = regionNames.next();
				if (!rname.equals(FLIGHT_REGION)){
					if (!rname.equals(FLIGHT_AVAIL_REGION)) onlyFlightOrFlightAvail = false;
				}
			}
			
			if (onlyFlightOrFlightAvail) 
				result = principal.canLook();
			else
				result = principal.isSystem();
		} else if (op.getOperationCode().isGet() || op.getOperationCode().isKeySet()) {
			if (regionName.equals(FLIGHT_REGION) || regionName.equals(FLIGHT_AVAIL_REGION))
				result = principal.canLook();
			else
				result = principal.isSystem();
		} else if (op.getOperationCode().isPut() || op.getOperationCode().isPutAll() || op.getOperationCode().isInvalidate() || op.getOperationCode().isDestroy()){
			if (regionName.equals(FLIGHT_REGION) || regionName.equals(FLIGHT_AVAIL_REGION))
				result = principal.canBook();
			else
				result = principal.isSystem();
		} else if (op.getOperationCode().isExecuteRegionFunction()){
			ExecuteFunctionOperationContext efCtx = (ExecuteFunctionOperationContext) op;
			if (efCtx.getFunctionId().equals("FlightAvailabilityFunction"))
				result = principal.canLook();
			else if (efCtx.getFunctionId().equals("FlightBookingFunction"))
				result = principal.canBook();
		}
		
		
		return result;
	}

	@Override
	public void init(Principal principal, DistributedMember dm, Cache cache)
			throws NotAuthorizedException {
		
		this.principal = (SimpleFilePrincipal) principal;
	}

}
