package io.pivotal.pde.sample.airline.loader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.geode.cache.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import io.pivotal.pde.sample.airline.domain.Airport;
import io.pivotal.pde.sample.airline.domain.Fare;
import io.pivotal.pde.sample.airline.domain.Flight;
import io.pivotal.pde.sample.airline.domain.FlightAvailability;
import io.pivotal.pde.sample.airline.domain.RevenueControl;
import io.pivotal.pde.sample.airline.keys.FlightKey;
import io.pivotal.pde.sample.gemfire.util.CSVFile;
import io.pivotal.pde.sample.gemfire.util.Dates;

/**
 * Loader
 * 
 * @author wmay
 *
 */

public class Loader {
	
	private static String LOCATOR_PREFIX="--locator=";
	private static Pattern LOCATOR_PATTERN= Pattern.compile("(\\S+)\\[(\\d{1,5})\\]");
	
	private static String AIRPORTS_FILE = "airports.txt";
	private static String FLIGHTS_FILE = "flights.txt";
	private static String FARES_FILE = "fares.txt";
	private static String REVENUE_CONTROLS_FILE = "revctls.txt";
	
	private static Logger log = LoggerFactory.getLogger(Loader.class);

	/////// helper methods
	private static int parseIntArg(String in, String message){
    	int result = 0;
    	
    	try{
    		result = Integer.parseInt(in);
    	} catch(NumberFormatException nfx){
    		System.err.println(message);
    		System.exit(1);  
    	}
		return result;
	}
	
	/////// MAIN
	
	// expects a --locator=host[port] argument
	public static void main(String []args){
		ClassPathXmlApplicationContext ctx = null;
		try {
			if (args.length == 0) {
				System.err.println("please provide the locator parameter");
				System.err.println("e.g. java io.pivotal.pde.sample.airline.loader --locator=host[port]");
				System.exit(-1);
			}

			String locatorHost = null;
			String locatorPort = null;
			if (args[0].startsWith(LOCATOR_PREFIX)){
    			String val = args[0].substring(LOCATOR_PREFIX.length());
    			Matcher m = LOCATOR_PATTERN.matcher(val);
    			if (!m.matches()){
    				System.err.println("argument \"" + val + "\" does not match the locator pattern \"host[port]\"");
    				System.exit(1);
    			} else {
    				locatorHost = m.group(1);
    				locatorPort = m.group(2);
    			}
			}
			
			if (locatorHost == null){
				System.err.println("please provide the locator parameter");
				System.err.println("e.g. java io.pivotal.pde.sample.airline.loader --locator=host[port]");
				System.exit(-1);
			}
			
			System.setProperty("locator.host", locatorHost);
			System.setProperty("locator.port", locatorPort);
			
			ctx = new ClassPathXmlApplicationContext("loader-context.xml");			
			
			loadAirports(ctx);		
			loadFares(ctx);
			loadFlights(ctx);
			loadRevenueControls(ctx);
			
			Region<String, FlightAvailability> flightAvailabilityRegion = (Region<String, FlightAvailability>) ctx.getBean("flightAvailabilityRegion");
			int count = clearRegion(flightAvailabilityRegion);
			log.info("removed " + count + " revenue controls entries");
			
		} catch(Exception x){
			log.error("Fatal Error - Loader Will Exit", x);
		} finally {
			ctx.close();
		}
	}
	
	private static void loadRevenueControls(ApplicationContext ctx){
		
		Region<Integer,RevenueControl> revenueControlRegion = null;
		// throws NoSuchBeanDefinitionException (RuntimeException) if the
		// bean cannot be found
		revenueControlRegion = (Region<Integer,RevenueControl>) ctx.getBean("revenueControlRegion");
		
		int cleared = clearRegion(revenueControlRegion);
		log.info("removed " + cleared + " revenue control entries");
		
		Resource resource = ctx.getResource(REVENUE_CONTROLS_FILE);
		if (!resource.exists()) error("Could not find revenue controls file on class path: " + REVENUE_CONTROLS_FILE);
		
		CSVFile revenueControlsFile = null;
		try {
			revenueControlsFile = new CSVFile(resource.getInputStream());
		} catch (IOException x){
			error("error while attempting to read " + REVENUE_CONTROLS_FILE, x);
		}
		
		int count = 0;
		try {
			while (revenueControlsFile.next()){
				RevenueControl rc = new RevenueControl();
				rc.setFlightNumber(Integer.parseInt(revenueControlsFile.getField(0)));
				rc.setZeroSeatPrice(new BigDecimal(revenueControlsFile.getField(1)));
				rc.setSlope(new BigDecimal(revenueControlsFile.getField(2)));
				
				revenueControlRegion.put(rc.getKey(), rc);
				log.info("loaded " + rc.toString());
				++count;
			}
		} finally {
			revenueControlsFile.close();
		}
		
		log.info("loaded " + count + " RevenueControls");
		
	}

	
	private static void loadAirports(ApplicationContext ctx){
		
		Region<String,Airport> airportRegion = null;
		// throws NoSuchBeanDefinitionException (RuntimeException) if the
		// bean cannot be found
		airportRegion = (Region<String,Airport>) ctx.getBean("airportRegion");
		
		int cleared = clearRegion(airportRegion);
		log.info("removed " + cleared + " airport entries");
		
		Resource resource = ctx.getResource(AIRPORTS_FILE);
		if (!resource.exists()) error("Could not find airports file on class path: " + AIRPORTS_FILE);
		
		CSVFile airportsFile = null;
		try {
			airportsFile = new CSVFile(resource.getInputStream());
		} catch (IOException x){
			error("error while attempting to read " + AIRPORTS_FILE, x);
		}
		
		int count = 0;
		try {
			while (airportsFile.next()){
				Airport airport = new Airport();
				airport.setCode(airportsFile.getField(0));
				airport.setTimeZone(airportsFile.getField(1));
				
				airportRegion.put(airport.getKey(), airport);
				log.info("loaded " + airport.toString());
				++count;
			}
		} finally {
			airportsFile.close();
		}
		
		log.info("loaded " + count + " Airports");
		
	}
	
	// loads fares for departures in the next X days 
	private static void loadFares(ApplicationContext ctx){
		// throws NoSuchBeanDefinitionException (RuntimeException) if the
		// bean cannot be found
		Region<Integer,Fare> fareRegion = null;
		fareRegion = (Region<Integer,Fare>) ctx.getBean("fareRegion");
		
		// throws NoSuchBeanDefinitionException (RuntimeException) if the
		// bean cannot be found
		Region<String,Airport> airportRegion = null;
		airportRegion = (Region<String,Airport>) ctx.getBean("airportRegion");
	
		LoaderConfig config = ctx.getBean("loaderConfig",LoaderConfig.class);		

		int cleared = clearRegion(fareRegion);
		log.info("removed " + cleared + " fare entries");
		
		
		Resource resource = ctx.getResource(FARES_FILE);
		if (!resource.exists()) error("Could not find fares file on class path: " + FARES_FILE);
		
		CSVFile faresFile = null;
		try {
			faresFile = new CSVFile(resource.getInputStream());
		} catch (IOException x){
			error("error while attempting to read " + FARES_FILE, x);
		}
		
		int count = 0;
		try {
			while (faresFile.next()){
				Fare fare = new Fare();
				fare.setId(count + 1);
				fare.setOrigin(faresFile.getField(0));
				fare.setDestination(faresFile.getField(1));
				fare.setFareClass(faresFile.getField(2));
				fare.setFare(new BigDecimal(faresFile.getField(3)));
				
				Date now = new Date();
				fare.setTimeZone(lookupTimeZone(ctx, fare.getOrigin()));
				fare.setStartingTravelDate(Dates.dateString(now, 0));
				fare.setEndingTravelDate(Dates.dateString(now, config.getDaysToLoad()));
				
				fareRegion.put(fare.getKey(), fare);
				++count;
				
				log.info("loaded " + fare.toString());
			}
		} finally {
			faresFile.close();
		}
		
		log.info("loaded " + count + " Fares");
	}

	// loads fares for departures in the next N days 
	private static void loadFlights(ApplicationContext ctx){
		// throws NoSuchBeanDefinitionException (RuntimeException) if the
		// bean cannot be found
		Region<FlightKey,Flight> flightRegion = null;
		flightRegion = (Region<FlightKey,Flight>) ctx.getBean("flightRegion");
		
		// throws NoSuchBeanDefinitionException (RuntimeException) if the
		// bean cannot be found
		Region<String,Airport> airportRegion = null;
		airportRegion = (Region<String,Airport>) ctx.getBean("airportRegion");
		
		LoaderConfig config = ctx.getBean("loaderConfig",LoaderConfig.class);		
		
		
		int cleared = clearRegion(flightRegion);
		log.info("removed " + cleared + " flight entries");
		
		
		Resource resource = ctx.getResource(FLIGHTS_FILE);
		if (!resource.exists()) error("Could not find flights file on class path: " + FLIGHTS_FILE);
		
		CSVFile flightsFile = null;
		try {
			flightsFile = new CSVFile(resource.getInputStream());
		} catch (IOException x){
			error("error while attempting to read " + FLIGHTS_FILE, x);
		}		
		
		int count = 0;
		try {
			while (flightsFile.next()){
				Flight flight = new Flight();
				flight.setFlightNumber(Integer.parseInt(flightsFile.getField(0)));
				flight.setOrigin(flightsFile.getField(1));
				flight.setDestination(flightsFile.getField(2));
				flight.setDepartureTime(validateTimeOfDay(flightsFile.getField(3)));
				flight.setArrivalTime(validateTimeOfDay(flightsFile.getField(4)));
				flight.setCapacity(Integer.parseInt(flightsFile.getField(5)));
				flight.setEquipmentCode(flightsFile.getField(6));
				
				// load one for each of the next ten days
				// the date will be relative to the origin time zone
				Date today = new Date();
				String originTimeZone = lookupTimeZone(ctx,flight.getOrigin());
				for (int d = 0; d < config.getDaysToLoad(); ++d){
					flight.setTimeZone(originTimeZone);
					flight.setDepartureDate(Dates.dateString(today, d));
					flightRegion.put(flight.getKey(), flight);
					++count;
					log.info("loaded " + flight.toString());					
				}
								
			}
		} finally {
			flightsFile.close();
		}
		
		log.info("loaded " + count + " Flights");
	}

	
	/**
	 * time of day has format HHMM
	 * @param 
	 * @return
	 */
	private static long parseTimeOfDay(String s){
		if (s.length() != 4) error("time of day must be a string consisting of 4 digits");
				
		long hour = Integer.parseInt(s.substring(0,2));
		long min = Integer.parseInt(s.substring(2,4));
		
		if (hour >23) error("hour portion of time of day cannot be greater than 23");
		if (min > 59) error("minute portion of time of day cannot be greater than 59");

		return (1000l * min * 60l) + (1000l * hour * 60l * 60l);
	}
	
	/**
	 * returns the original input String if it is valid or throw an exception if not
	 * @param s
	 * @return
	 */
	private static String validateTimeOfDay(String s){
		if (s.length() != 4) error("time of day must be a string consisting of 4 digits");
				
		long hour = Integer.parseInt(s.substring(0,2));
		long min = Integer.parseInt(s.substring(2,4));
		
		if (hour >23) error("hour portion of time of day cannot be greater than 23");
		if (min > 59) error("minute portion of time of day cannot be greater than 59");

		return s;
	}
	

	
	private static String  lookupTimeZone(ApplicationContext ctx, String airportCode){
		Region<String,Airport> airportRegion = null;
		// throws NoSuchBeanDefinitionException (RuntimeException) if the
		// bean cannot be found
		airportRegion = (Region<String,Airport>) ctx.getBean("airportRegion");
		
		Airport a = airportRegion.get(airportCode);
		return (a==null) ? null : a.getTimeZone();
		
	}
	
	private static int clearRegion(Region region){
		Set keys =  region.keySetOnServer();
		Iterator it = keys.iterator();
		while(it.hasNext()) region.remove(it.next());
		return keys.size();
	}

	private static void info(String msg){
		log.info(msg);
	}
	
	private static void error(String msg){
		log.error(msg);
		throw new RuntimeException(msg);
	}
	
	private static void error(String msg, Exception x){
		log.error(msg,x);
		throw new RuntimeException(msg,x);
	}
	
}
