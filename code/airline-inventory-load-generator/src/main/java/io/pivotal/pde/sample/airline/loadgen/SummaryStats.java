package io.pivotal.pde.sample.airline.loadgen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SummaryStats {
	
	long startTime;
	double count;
	double sum;
	//double sumOfSquares;
	
	List<Double> observations = new ArrayList<Double>(100000);

	//TODO - this uses synchronization to achieve thread safety
	// at high throughput this could cause a bottleneck.  It would be
	// better to have a list per thread so no synchronization would be
	// required during the test
	public void addObservation(double d){
		synchronized(observations){
			count += 1.0;
			sum += d;
//			sumOfSquares += (d * d);
			
			observations.add(d);
		}
	}
	
	public SummaryStats(){
		reset();
	}
	
	public void reset(){
		count = 0;
		sum = 0;
//		sumOfSquares = 0;
		observations.clear();
		startTime = System.currentTimeMillis();
	}
	
	public void report(){
		synchronized(observations){
			//compute quartiles
			Collections.sort(observations);
			double q1 = observations.get( (1 * observations.size() / 2) -1);
			double q2 = observations.get( (3 * observations.size() / 4) -1);
			double q3 = observations.get( (9 * observations.size() / 10) -1);
			double q4 = observations.get( (99 * observations.size() / 100) -1);
			double q5 = observations.get(observations.size() - 1);
			
	//		double mean = sum/count;
	//		double stddev = Math.sqrt((sumOfSquares / count) - ((sum*sum)/(count * count))); 
	//		System.out.println("count: " + count + " mean: " + mean + " stddev: " + stddev);
			System.out.println("count: " + count );
			System.out.println("throughput: " + (1000l * (long) count)/(System.currentTimeMillis() - startTime) + " ops/sec" );
			System.out.println("distribution:");
			System.out.println(String.format("\t50%%  <= %,10.0f", q1)); 
			System.out.println(String.format("\t75%% <= %,10.0f", q2)); 
			System.out.println(String.format("\t90%% <= %,10.0f", q3)); 
			System.out.println(String.format("\t99%% <= %,10.0f", q4)); 
			System.out.println(String.format("\t100%% <= %,10.0f", q5));
		}
	}
	
	public static void main(String []args){
		SummaryStats stats = new SummaryStats();
		
		stats.addObservation(2);
		stats.addObservation(2);
		stats.addObservation(2);
		
		System.out.println("mean should be 2 stddev should be 0");
		stats.report();
		
		stats.reset();
		stats.addObservation(1.0);
		stats.addObservation(3.0);
		
		System.out.println("mean should be 2 stddev should be 1");
		stats.report();
		stats.reset();
		
		stats.addObservation(1.0);
		stats.addObservation(2.0);
		stats.addObservation(3.0);
		stats.addObservation(4.0);
		stats.addObservation(5.0);
		
		System.out.println("mean should be 3 stddev should be 1.4");
		stats.report();
		stats.reset();
		
		
	}
}
