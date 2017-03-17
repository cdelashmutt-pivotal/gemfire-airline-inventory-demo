package io.pivotal.pde.sample.airline.server;

import java.math.BigDecimal;
import java.util.ArrayList;
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

import io.pivotal.pde.sample.airline.keys.FlightAvailabilityKey;
import io.pivotal.pde.sample.airline.keys.FlightKey;



/**
 * Input is an array
 * entry 0 - int flightNo
 * entry 1 - String date in yyyymmdd format
 *  
 * returns an ArrayList of FlightAvailability items (one per fare class)
 * 
 * should be invoked with "onRegion" on the flight region
 * 
 * @author wmay
 *
 */
public class FlightAvailabilityFunction implements Function, Declarable {
	private static final long serialVersionUID = 6153350017083252569L;

	@Override
	public void init(Properties props) {
		// no props
	}

	@Override
	public void execute(FunctionContext ctx) {
		
		RegionFunctionContext rctx = (RegionFunctionContext) ctx;
		
		// TODO test - do you have to return the exception, or can you just throw it ?
		Object []args = (Object []) ctx.getArguments();
		FlightKey key = (FlightKey) args[0];
		
		ArrayList<PdxInstance> result = null;
		CacheTransactionManager tm = CacheFactory.getAnyInstance().getCacheTransactionManager();
		try {
			while(true){
				try {
					tm.begin();
					result = computeFlightAvailability(key, rctx);
					tm.commit();
					tm = null;
					break;
				} catch(CommitConflictException x){
					CacheFactory.getAnyInstance().getLogger().fine("commit conflict during computeAvailability transaction (flight=" + key.getFlightNumber() + " date=" + key.getFlightDate() + ") - will retry");
				}
			}
		} catch(Exception x){
			if (tm != null) tm.rollback();
			error("error computing availability for flight: " + key.getFlightNumber() +  " on " + key.getFlightDate(), x); //throws exception
		}
		
		
		rctx.getResultSender().lastResult(result);
	}

	@Override
	public String getId() {
		return FlightAvailabilityFunction.class.getSimpleName();
	}

	@Override
	public boolean hasResult() {
		return true;
	}

	@Override
	public boolean isHA() {
		return true;
	}

	@Override
	public boolean optimizeForWrite() {
		return true;
	}

	
	// computes flight availability
	// may create FlightAvailability entries as a side effect
	ArrayList<PdxInstance> computeFlightAvailability(FlightKey flightKey, RegionFunctionContext rctx){
		
		Region<String,?> flightRegion = PartitionRegionHelper.getLocalDataForContext(rctx);
		Map<String, Region<?,?>> regionMap = PartitionRegionHelper.getLocalColocatedRegions(rctx);
		Region revenueControlRegion = regionMap.get("/RevenueControl");
		Region flightAvailabilityRegion = regionMap.get("/FlightAvailability");
		Region fareRegion = CacheFactory.getAnyInstance().getRegion("Fare");
		
		
		// get the Flight
		PdxInstance flight = (PdxInstance) flightRegion.get(flightKey);		

		// pull the revenue control for the flight 
		PdxInstance revenueControl = (PdxInstance) revenueControlRegion.get(flightKey.getFlightNumber());
		if (revenueControl == null) error("no revenue control found for flight: "  + flightKey.getFlightNumber()); // throws exception		
		
		// figure out all of the possible fares
		SelectResults<Struct> fares = queryFares((String) flight.getField("origin"), (String) flight.getField("destination"), (String) flight.getField("departureDate"));
		
		
		// for each fare, see if there is already a FlightAvailability region entry - if there is
		// return it, otherwise compute the availability and record it in the FlightAvailability region
		ArrayList<PdxInstance> result = new ArrayList<PdxInstance>(fares.size());
		Struct fare = null;
		Iterator<Struct> fareIterator = fares.iterator();
		while(fareIterator.hasNext()){
			fare = fareIterator.next();
			FlightAvailabilityKey faKey = new FlightAvailabilityKey(flightKey.getFlightNumber(), flightKey.getFlightDate(), (String) fare.get("fareClass")); 
			PdxInstance flightAvailability = 
					(PdxInstance) flightAvailabilityRegion.get(faKey);
			
			if (flightAvailability == null){
				// compute the availability
				BigDecimal z = (BigDecimal) revenueControl.getField("zeroSeatPrice");
				BigDecimal m = (BigDecimal) revenueControl.getField("slope");
				BigDecimal f = (BigDecimal) fare.get("fare");
				
				int seatsOpen = ((Integer) flight.getField("capacity")).intValue() - ((Integer) flight.getField("seatsSold")).intValue();
				
				int s = z.subtract(f).divide(m).intValue();
				int avail = seatsOpen -s;
				if (avail < 0) avail = 0;
				
				flightAvailability = 
					CacheFactory.getAnyInstance().createPdxInstanceFactory("io.pivotal.pde.sample.airline.domain.FlightAvailability")
					.writeInt("flightNumber", flightKey.getFlightNumber())
					.writeString("flightDate", flightKey.getFlightDate())
					.writeString("origin", (String) flight.getField("origin"))
					.writeString("destination", (String) flight.getField("destination"))
					.writeString("fareClass", (String) fare.get("fareClass"))
					.writeObject("fare", fare.get("fare"))
					.writeInt("availableSeats", avail)
					.create();
				
				// add it to the FlightAvailability region				
				flightAvailabilityRegion.put(faKey, flightAvailability);
			}
			
			result.add(flightAvailability);
		}

		return result;
	}
	
	// returned Struct contains fareClass, fare
	private SelectResults<Struct> queryFares(String origin, String dest, String date){
		Query q = CacheFactory.getAnyInstance().getQueryService()
				.newQuery("SELECT fareClass, fare FROM /Fare WHERE" +
						" origin = $1 AND destination = $2 " +
						"AND startingTravelDate <= $3 AND endingTravelDate >= $3");
		
		SelectResults<Struct> result = null;
		try {
			result = (SelectResults<Struct>) q.execute(new Object[]{origin, dest, date});
		} catch (Exception x) {
			error("error executing query on Fare region", x);
		} 
		return result;
	}

	private void error(String msg){
		CacheFactory.getAnyInstance().getLogger().error(msg);
		throw new RuntimeException(msg);
	}

	private void error(String msg, Exception x){
		CacheFactory.getAnyInstance().getLogger().error(msg,x);
		throw new RuntimeException(msg, x);
	}
}
