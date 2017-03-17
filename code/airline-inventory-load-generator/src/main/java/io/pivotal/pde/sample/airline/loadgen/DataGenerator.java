package io.pivotal.pde.sample.airline.loadgen;

public interface DataGenerator {
	Object next(int threadNum);
}
