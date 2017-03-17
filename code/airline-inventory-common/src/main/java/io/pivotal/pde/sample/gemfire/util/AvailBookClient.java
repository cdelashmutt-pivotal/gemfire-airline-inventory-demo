package io.pivotal.pde.sample.gemfire.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.cache.query.CqAttributes;
import org.apache.geode.cache.query.CqAttributesFactory;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqListener;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.SelectResults;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import io.pivotal.pde.sample.airline.domain.Flight;
import io.pivotal.pde.sample.airline.domain.FlightAvailability;
import io.pivotal.pde.sample.airline.keys.FlightKey;

public class AvailBookClient {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ctx = null;
		try {
			ctx = new ClassPathXmlApplicationContext("client-context.xml");

			prompt();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					System.in));
			String line = reader.readLine();
			String[] words = null;
			while (line != null) {
				try {
					if (line.trim().length() > 0) {
						words = line.split("\\s+");
						if (words[0].equals("avail"))
							doAvail(words);
						else if (words[0].equals("book"))
							doBook(words);
						else if (words[0].equals("quit"))
							break;
						else if (words[0].equals("mode"))
							doMode(words);
						else if (words[0].equals("watch"))
							doWatch(words);
						else
							System.out.println("unrecognized command: "
									+ words[0]);
					}
				} catch (Exception x) {
					x.printStackTrace(System.err);
				}

				prompt();
				line = reader.readLine();
			}
		} catch (Exception x) {
			x.printStackTrace(System.err);
		} finally {
			if (ctx != null)
				ctx.close();
		}
	}

	private static void doWatch(String []words){
		// first query all of the matching flights
		// then do avail for each, then start a CQ
		
		String origin = words[1];
		String dest = words[2];
		String date = words[3];
		
		Query q = CacheFactory.getAnyInstance().getQueryService().newQuery("SELECT flightNumber FROM /Flight WHERE origin = $1 AND destination = $2 and departureDate = $3 ");
		SelectResults<Integer> results = null;
		
		try {
			results = (SelectResults<Integer>) q.execute(new Object[]{origin, dest, date});
		} catch(Exception x){
			x.printStackTrace(System.err);
			return; // RETURN
		}
		
		if (results.size() == 0){
			System.out.println("no matching flights");
			return; //RETURN
		}
		
		for(Integer f : results){
			doAvail(new String []{"avail",f.toString(),date});
		}
		
		// now set up a CQ 
		CqAttributesFactory cqaf = new CqAttributesFactory();
		cqaf.addCqListener(new AvailListener());
		CqAttributes cqa = cqaf.create();
		String query = "SELECT * FROM /FlightAvailability WHERE origin = \'" + origin + "\'  and destination = \'" + dest +  "\' and flightDate = \'" + date + "\'";
		CqQuery cq = null;
		try {
			cq = CacheFactory.getAnyInstance().getQueryService().newCq("avails", query, cqa, false);
			cq.execute();
		} catch(Exception x){
			x.printStackTrace(System.err);
			return; //RETURN
		}
	}
	
	private static void doMode(String[] words) {
		Region flightRegion = CacheFactory.getAnyInstance().getRegion(
				"SystemParameter");
		Execution exec = FunctionService.onRegion(flightRegion).withArgs(
				words[1]);
		exec.execute("CacheControlFunction");
	}

	public static ArrayList<FlightAvailability> doAvail(FlightKey key) {
		Object[] args = new Object[] { key };
		Set<Object> filterSet = new HashSet<Object>();
		filterSet.add(key);
		Region<FlightKey, Flight> flightRegion = CacheFactory.getAnyInstance().getRegion("Flight");
		Execution exec = FunctionService.onRegion(flightRegion).withArgs(args).withFilter(filterSet);
		ResultCollector rc = exec.execute("FlightAvailabilityFunction");

		List<ArrayList<FlightAvailability>> flightAvails = (List<ArrayList<FlightAvailability>>) rc.getResult();
		if (flightAvails.size() != 1)
			throw new RuntimeException("Unexpected received "
					+ flightAvails.size() + " results");
		
		return flightAvails.get(0);
	}
	
	private static void doAvail(String[] words) {
		int flightNumber = Integer.parseInt(words[1]);
		String date = words[2];

		ArrayList<FlightAvailability> availList = doAvail(new FlightKey(flightNumber, date));		
		Iterator<FlightAvailability> it = availList.iterator();
		FlightAvailability avail = null;
		while (it.hasNext()) {
			avail = it.next();
			System.out.println("\t" + avail);
		}
	}

	public static boolean doBook(FlightKey key, String fareClass, int count) {
		Object[] args = new Object[] {key, fareClass, count };
		Set<Object> filterSet = new HashSet<Object>();
		
		filterSet.add(key);
		Region flightRegion = CacheFactory.getAnyInstance().getRegion("Flight");
		Execution exec = FunctionService.onRegion(flightRegion).withArgs(args).withFilter(filterSet);
		ResultCollector rc = exec.execute("FlightBookingFunction");

		ArrayList<Boolean> results = (ArrayList<Boolean>) rc.getResult();
		if (results.size() != 1)
			throw new RuntimeException("Unexpected received " + results.size()
					+ " results");
		
		boolean success = results.get(0).booleanValue();
		if (success) doAvail(key);   // don't remember why we do this, maybe to update FlightAvailability, research it
		
		return success;
	}
	
	private static void doBook(String[] words) {
		int flightNumber = Integer.parseInt(words[1]);
		String date = words[2];
		String fareClass = words[3];
		int count = Integer.parseInt(words[4]);
		
		boolean success = doBook(new FlightKey(flightNumber, date), fareClass, count);
		
		System.out.println(success ? "booking successful"
				: "booking could not be completed");		
	}

	private static void prompt() {
		System.out.println("enter command");
		System.out.println("\tavail <flt> <yyyymmdd>");
		System.out.println("\tbook <flt> <yyyymmdd> <fareclass> <count>");
		System.out.println("\twatch <origin> <dest> <yyyymmdd>");
		System.out.println("\tmode readonly readwrite");
		System.out.println("\tquit");
		System.out.println();
	}
	
	private static class AvailListener implements CqListener {

		@Override
		public void close() {
		}

		@Override
		public void onError(CqEvent event) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onEvent(CqEvent cqEvent) {
			if (cqEvent.getQueryOperation().isUpdate() || cqEvent.getQueryOperation().isCreate()){
				FlightAvailability avail = (FlightAvailability) cqEvent.getNewValue();
				System.out.println(">> AVAILABILITY UPDATE: " + avail);
			}
		}
		
	}
}
