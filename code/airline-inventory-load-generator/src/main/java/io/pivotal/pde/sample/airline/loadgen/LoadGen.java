package io.pivotal.pde.sample.airline.loadgen;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 
 * @author wmay
 *
 */

public class LoadGen {
	
	private DataGenerator dataGenerator;
	private int threadCount;
	private int executionIntervalMs;
	private SummaryStats responseTimeSummaryStats;
	private Test test;

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public void setExecutionIntervalMs(int executionIntervalMs) {
		this.executionIntervalMs = executionIntervalMs;
	}

	public void setDataGenerator(DataGenerator dataGenerator) {
		this.dataGenerator = dataGenerator;
	}
	
	public void setTest(Test t){
		this.test = t;
	}

	public void init(){
		responseTimeSummaryStats = new SummaryStats(); 
	}
	
	public void run(){		
		try {
			Random rand = new Random();
			TestThread []testThread = new TestThread[threadCount];
			for(int i=0; i < threadCount; ++i) testThread[i] = new TestThread(i);

			for(int i=0; i < threadCount; ++i) {
				testThread[i].start();
				try {
					Thread.sleep(rand.nextInt(2000));
				} catch(InterruptedException x){
					// not a problem
				}
			}

			Thread reportThread = new ReportThread();
			reportThread.start();
			
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("press enter to stop the test");
			reader.readLine();
			
			for(int i=0; i < threadCount; ++i) testThread[i].shutdown();
			
		} catch(Exception x){
			x.printStackTrace(System.err);
		}
		
	}

	private static String locatorHost = null;
	private static String locatorPort= null;
	private static String loadgenThreads = "1";
	private static String loadgenExecIntervalMS = "500";
	private static String lookToBookRatio = "10";
	private static String fromDate = null;
	private static String toDate= null;
	
	private static String LOCATOR_PREFIX = "--locator=";
	private static String THREADS_PREFIX = "--threads=";
	private static String LOOK_TO_BOOK_PREFIX = "--looktobook=";
	private static String INTERVAL_PREFIX = "--intervalms=";
	private static String FROM_DATE_PREFIX = "--from-date=";
	private static String TO_DATE_PREFIX = "--to-date=";
	
	private static Pattern LOCATOR_PATTERN= Pattern.compile("(\\S+)\\[(\\d{1,5})\\]");
	
	private static void parseArgs(String []args){
		if (args.length == 0) {
			System.err.println("please provide the locator parameter");
			System.err.println("e.g. java io.pivotal.pde.sample.airline.loadgen.LoadGen --locator=host[port]");
			System.err.println("optional parameters include");
			System.err.println("\t--threads=n");
			System.err.println("\t--looktobook=n");
			System.err.println("\t--intervalms=n");
			System.err.println("\t--from-date=yyymmdd");
			System.err.println("\t--to-date=yyyymmdd");
			System.exit(1);
		}

		for(String arg: args){
			if (arg.startsWith(LOCATOR_PREFIX)){
				String val = args[0].substring(LOCATOR_PREFIX.length());
				Matcher m = LOCATOR_PATTERN.matcher(val);
				if (!m.matches()){
					System.err.println("argument \"" + val + "\" does not match the locator pattern \"host[port]\"");
					System.exit(1);
				} else {
					locatorHost = m.group(1);
					locatorPort = m.group(2);
				}
			} else if (arg.startsWith(THREADS_PREFIX)){
				loadgenThreads = arg.substring(THREADS_PREFIX.length());
			} else if (arg.startsWith(LOOK_TO_BOOK_PREFIX)){
				lookToBookRatio = arg.substring(LOOK_TO_BOOK_PREFIX.length());
			} else if (arg.startsWith(INTERVAL_PREFIX)){
				loadgenExecIntervalMS = arg.substring(INTERVAL_PREFIX.length());
			} else if (arg.startsWith(FROM_DATE_PREFIX)){
				fromDate = arg.substring(FROM_DATE_PREFIX.length());
			} else if (arg.startsWith(TO_DATE_PREFIX)){
				toDate = arg.substring(TO_DATE_PREFIX.length());
			} else {
				System.err.println("unrecognized argument: " + arg);
				System.exit(1);
			}
		}
		
		if (locatorHost == null){
			System.err.println("please provide the locator parameter");
			System.err.println("e.g. java io.pivotal.pde.sample.airline.loadgen.LoadGen --locator=host[port]");
			System.exit(1);
		}
		
		System.err.println("locatorPort = " + locatorPort);
		
		System.setProperty("locator.host", locatorHost);
		System.setProperty("locator.port", locatorPort);
		System.setProperty("loadgen.threads", loadgenThreads);
		System.setProperty("loadgen.looktobook", lookToBookRatio);
		System.setProperty("loadgen.executeintervalms", loadgenExecIntervalMS);		
		System.setProperty("loadgen.fromdate", fromDate);		
		System.setProperty("loadgen.todate", toDate);		
	}
	
	public static void main(String []args){
		
		parseArgs(args);
		
		ClassPathXmlApplicationContext ctx = null;
		try {
			ctx = new ClassPathXmlApplicationContext("loadgen-context.xml");
			
			LoadGen test = ctx.getBean("loadGenerator",LoadGen.class);
			test.run();
		} catch(Exception x){
			x.printStackTrace(System.err);
		} finally {
			ctx.close();
		}
	}
	
	private class ReportThread extends Thread  {
		public ReportThread(){
			this.setDaemon(true);
		}
		
		public void run(){
			while(true){
				try {
					Thread.sleep(10000);
				} catch(InterruptedException x){
					// not a prob
				}
				
				responseTimeSummaryStats.report();
			}
		}
	}
	
	private class TestThread extends Thread {
		
		private boolean running;
		private int threadNum;
		private int updateCount;
		
		public TestThread(int num){
			this.threadNum = num;
			this.updateCount = 0;
		}
		

		public void shutdown(){
			if (this.isAlive()){
				running = false;
				this.interrupt();
				try {
					this.join(20000);
				} catch(InterruptedException x){
					//
				}
				
				if (this.isAlive()) System.err.println("warning: could not verify thread shutdown");
			}
		}
		
		@Override
		public void run() {
			this.running = true;
			
			long now = System.currentTimeMillis();
			long start = now;
			long nextScheduledTime = now + executionIntervalMs;
			
			test.doTest(dataGenerator.next(this.threadNum));
			
			while(running){
				now = System.currentTimeMillis();
				responseTimeSummaryStats.addObservation(now - start);
				
				long sleep = nextScheduledTime - now;
				if (sleep > 0){
					try {
						Thread.sleep(sleep);
					} catch(InterruptedException x){
						// not a problem
					}
				} else {
					if (sleep < -10000){
						System.err.println("unable to achieve desired rate - stopping thread");
						break;  //BREAK
					}
				}
				
				nextScheduledTime += executionIntervalMs;
				start = System.currentTimeMillis();
				
				test.doTest(dataGenerator.next(this.threadNum));
			}
			
		}
				
	}
	
}
