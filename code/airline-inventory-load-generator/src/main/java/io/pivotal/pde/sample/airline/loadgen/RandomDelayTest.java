package io.pivotal.pde.sample.airline.loadgen;

import java.util.Random;

public class RandomDelayTest implements Test {

	private Random random;
	private int delay;
	
	public RandomDelayTest(int delay) {
		this.delay = delay;
		random = new Random();
	}

	@Override
	public void doTest(Object testCase) {
		try {
			Thread.sleep(random.nextInt(delay));
		} catch(InterruptedException x){
			//
		}
	}

}
