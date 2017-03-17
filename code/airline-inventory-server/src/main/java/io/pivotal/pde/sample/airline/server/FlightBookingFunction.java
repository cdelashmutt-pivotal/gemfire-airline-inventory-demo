package io.pivotal.pde.sample.airline.server;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.CacheTransactionManager;
import org.apache.geode.cache.CommitConflictException;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.partition.PartitionRegionHelper;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.Struct;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.WritablePdxInstance;

import io.pivotal.pde.sample.airline.keys.FlightAvailabilityKey;
import io.pivotal.pde.sample.airline.keys.FlightKey;

/**
 * arguments are 
 * int flight
 * String date
 * String fareClass
 * int count
 * 
 * return is true or false
 * 
 * @author wmay
 *
 */

public class FlightBookingFunction implements Function, Declarable {

	private static final long serialVersionUID = 6168984565142308613L;

	@Override
	public void init(Properties args) {
		// no props
	}

	@Override
	public void execute(FunctionContext ctx) {
		
		RegionFunctionContext rctx = (RegionFunctionContext) ctx;
		Object []args = (Object []) ctx.getArguments();
		
		FlightKey flightKey = (FlightKey) args[0];
		String fareClass = (String) args[1];
		int count = (Integer) args[2];
				
		
		// check availability for the given flight and date and fare class
		// assuming it is there - increment seats sold and 
		// delete all related availability records
		boolean result = false;
		CacheTransactionManager tm = CacheFactory.getAnyInstance().getCacheTransactionManager();
		try {
			while(true){
				try {
					tm.begin();
					result = computeAvailabilityAndBook(flightKey, fareClass, count, rctx);
					tm.commit();
					tm = null;
					break;
				} catch(CommitConflictException x){
					CacheFactory.getAnyInstance().getLogger().fine("commit conflict during bookFlight transaction (flight=" + flightKey.getFlightNumber() + " date=" + flightKey.getFlightDate() + " fareClass=" + fareClass + " count=" + count +  " ) - will retry");
				}
			}
		} catch(Exception x){
			if (tm != null) tm.rollback();
			error("error bookFlight flight=" + flightKey.getFlightNumber() + " date=" + flightKey.getFlightDate() + " fareClass=" + fareClass + " count=" + count , x); //throws exception
		}
		
		
		rctx.getResultSender().lastResult(result);
	}

	// may create FlightAvailability entries as a side effect
	boolean computeAvailabilityAndBook(FlightKey key, String fareClass, int count, RegionFunctionContext rctx){
		
		Region<FlightKey,PdxInstance> flightRegion = PartitionRegionHelper.getLocalDataForContext(rctx);
		Map<String, Region<?,?>> regionMap = PartitionRegionHelper.getLocalColocatedRegions(rctx);
		Region revenueControlRegion = regionMap.get("/RevenueControl");
		Region flightAvailabilityRegion = regionMap.get("/FlightAvailability");
		Region fareRegion = CacheFactory.getAnyInstance().getRegion("Fare");
		
		
		// get the Flight
		PdxInstance flight = null;
		Object obj = flightRegion.get(key);
		if (obj == null) error("no such flight: " + key.getFlightNumber() + " on " + key.getFlightDate());
		flight = (PdxInstance) obj;

		// pull the revenue control for the flight 
		PdxInstance revenueControl = (PdxInstance) revenueControlRegion.get(key.getFlightNumber());
		if (revenueControl == null) error("no revenue control found for flight: "  + key.getFlightNumber()); // throws exception		
		
		// pull the fare
		Struct fare = queryFares((String) flight.getField("origin"), (String) flight.getField("destination"), (String) flight.getField("departureDate"), fareClass);
				
		//  see if there is already a FlightAvailability region entry - if there is
		// return it, otherwise compute the availability 
		
		int availability;
		PdxInstance flightAvailability = 
				(PdxInstance) flightAvailabilityRegion.get( new FlightAvailabilityKey(key.getFlightNumber(), key.getFlightDate(),(String) fare.get("fareClass")) );
		
		if (flightAvailability == null){
			// compute the availability
			BigDecimal z = (BigDecimal) revenueControl.getField("zeroSeatPrice");
			BigDecimal m = (BigDecimal) revenueControl.getField("slope");
			BigDecimal f = (BigDecimal) fare.get("fare");
			
			int seatsOpen = ((Integer) flight.getField("capacity")).intValue() - ((Integer) flight.getField("seatsSold")).intValue();
			
			int s = z.subtract(f).divide(m).intValue();
			availability = seatsOpen -s;
		} else {
			availability = (Integer) flightAvailability.getField("availableSeats");
		}

		boolean result = availability >= count;

		// update seats sold
		if (result){
			int seatsSold = (Integer) flight.getField("seatsSold");
			WritablePdxInstance newFlight = flight.createWriter();
			
			newFlight.setField("seatsSold", Integer.valueOf(seatsSold + count));
			flightRegion.put(key, newFlight);
			
			//remove all availability records
			removeFlightAvailability(key, flightAvailabilityRegion);
		}
		
		return result;
	}
	
	private void removeFlightAvailability(FlightKey flightKey, Region flightAvailabilityRegion){
		Query q = CacheFactory.getAnyInstance().getQueryService()
				.newQuery("SELECT fareClass FROM /FlightAvailability WHERE flightNumber = $1 AND flightDate = $2 ");
	
		SelectResults<String> results = null;
		
		try {
			results = (SelectResults<String>) q.execute(new Object[] {Integer.valueOf(flightKey.getFlightNumber()), flightKey.getFlightDate()});
		} catch(Exception x){
			error("error executing query on FlightAvailability region", x);
		}
		
		if ( (results != null) && (results.size() > 0)) {
			String fareClass = null;
			Iterator<String> it = results.iterator();
			while(it.hasNext()){
				fareClass = it.next();
				flightAvailabilityRegion.remove(new FlightAvailabilityKey(flightKey.getFlightNumber(), flightKey.getFlightDate(), fareClass));
			}
		}
		
	}
	
	// returned Struct contains fareClass, fare
	private Struct queryFares(String origin, String dest, String date, String fareClass){
		Query q = CacheFactory.getAnyInstance().getQueryService()
				.newQuery("SELECT fareClass, fare FROM /Fare WHERE" +
						" origin = $1 AND destination = $2 " +
						"AND startingTravelDate <= $3 AND endingTravelDate >= $3 " + 
						"AND fareClass = $4 ");
		
		SelectResults<Struct> result = null;
		try {
			result = (SelectResults<Struct>) q.execute(new Object[]{origin, dest, date, fareClass});
		} catch (Exception x) {
			error("error executing query on Fare region", x);
		} 

		if ( (result == null) || (result.size() == 0)) {
			warn("requested fare not found: origin=" + origin + " destination=" + dest + " travel date=" + date + " fareClass=" + fareClass );
		}
		
		return result.asList().get(0);
	}
	
	@Override
	public String getId() {
		return FlightBookingFunction.class.getSimpleName();
	}

	@Override
	public boolean hasResult() {
		return true;
	}

	@Override
	public boolean isHA() {
		return false;
	}

	@Override
	public boolean optimizeForWrite() {
		return true;
	}
	
	private void error(String msg){
		CacheFactory.getAnyInstance().getLogger().error(msg);
		throw new RuntimeException(msg);
	}

	private void error(String msg, Exception x){
		CacheFactory.getAnyInstance().getLogger().error(msg,x);
		throw new RuntimeException(msg, x);
	}
	
	private void warn(String msg){
		CacheFactory.getAnyInstance().getLogger().warning(msg);
	}

}
