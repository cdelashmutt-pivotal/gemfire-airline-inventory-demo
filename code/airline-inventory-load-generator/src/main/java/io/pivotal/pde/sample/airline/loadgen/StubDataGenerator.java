package io.pivotal.pde.sample.airline.loadgen;

public class StubDataGenerator implements DataGenerator {

	public StubDataGenerator() {
	}

	@Override
	public Object next(int threadNum) {
		return null;
	}

}
