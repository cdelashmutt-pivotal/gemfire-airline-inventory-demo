package io.pivotal.pde.sample.airline.loadgen;

import java.util.Random;
import java.util.Set;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;

import io.pivotal.pde.sample.airline.domain.Flight;
import io.pivotal.pde.sample.airline.keys.FlightKey;

/**
 * This random generator just pulls in all of the FlightKeys
 * and randomly hands them out - unless a date range is 
 * specified, in which case it pulls in keys within the 
 * given range
 * 
 * @author rmay
 */
public class SimpleRandomGenerator implements DataGenerator {

	private Random random;
	private Region<FlightKey, Flight> flightRegion;
	private FlightKey []keys;
	private String fromDate = null;
	private String toDate = null;
	
	
	public void setFlightRegion(Region<FlightKey, Flight> region){
		this.flightRegion = region;
	}
	
	
	public void setFromDate(String fromDate) {
		this.fromDate = fromDate;
	}

	public void setToDate(String toDate) {
		this.toDate = toDate;
	}

	public void init(){
		this.random = new Random();
		
		Set<FlightKey> keySet = null;
		if (fromDate != null && toDate != null){
			QueryService qs = CacheFactory.getAnyInstance().getQueryService();
			Query q = qs.newQuery("SELECT key FROM /Flight.entries WHERE value.departureDate >= $1 AND value.departureDate <= $2");
			SelectResults<FlightKey> results =null;
			try {
				results =(SelectResults<FlightKey>)  q.execute(new Object[]{this.fromDate, this.toDate});
			} catch( FunctionDomainException | TypeMismatchException | NameResolutionException | QueryInvocationTargetException x){
				x.printStackTrace(System.err);
				throw new RuntimeException(x);
			}
			
			keySet = results.asSet();
		} else {
			keySet = flightRegion.keySetOnServer();
		}
		this.keys = new FlightKey[keySet.size()];
		keySet.toArray(this.keys);
	}
	

	@Override
	public Object next(int threadNum) {
		return this.keys[random.nextInt(keys.length)];
	}

}
