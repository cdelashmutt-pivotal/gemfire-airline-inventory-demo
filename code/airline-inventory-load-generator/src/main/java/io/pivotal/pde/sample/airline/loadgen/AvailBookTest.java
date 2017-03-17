package io.pivotal.pde.sample.airline.loadgen;

import java.util.Random;

import org.apache.geode.cache.Region;

import io.pivotal.pde.sample.airline.domain.Flight;
import io.pivotal.pde.sample.airline.keys.FlightKey;
import io.pivotal.pde.sample.gemfire.util.AvailBookClient;

public class AvailBookTest implements Test {

	private static String [] FARE_CLASSES = {"A","B","C"};
	
	private Random random;
	private int lookBookRatio;
	private double bookThreshold;
	
	
	public void setLookBookRatio(int ratio){
		this.lookBookRatio = ratio;
	}

	public void init(){
		this.random = new Random();
		
		bookThreshold  = (double) 1.0 / (double) this.lookBookRatio;
	}
	
	/*
	 * testCase is expected to be a FlightKeu
	 *  
	 */
	@Override
	public void doTest(Object testCase) {
		FlightKey k = (FlightKey) testCase;
		
		AvailBookClient.doAvail(k);
		
		if(random.nextDouble() < this.bookThreshold){
			String fareClass =FARE_CLASSES[ random.nextInt(FARE_CLASSES.length)];
			int count = 1 + random.nextInt(4);
			
			AvailBookClient.doBook(k, fareClass, count);
//			if (AvailBookClient.doBook(k, fareClass, count))
//				System.out.println(String.format("%d %s book %d seats: SUCCESS", k.getFlightNumber(), k.getFlightDate(), count));
//			else
//				System.out.println(String.format("%d %s book %d seats: FAILED", k.getFlightNumber(), k.getFlightDate(), count));
		}
	}

}
