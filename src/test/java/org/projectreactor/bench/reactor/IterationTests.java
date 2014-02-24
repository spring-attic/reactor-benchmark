package org.projectreactor.bench.reactor;

import org.junit.Test;
import org.projectreactor.bench.AbstractThroughputTests;

/**
 * @author Jon Brisbin
 */
public class IterationTests extends AbstractThroughputTests {

	IterationBenchmarks benchmarks;

	@Override
	protected long getTimeout() {
		return 1000;
	}

	@Override
	protected void doSetup() {
		benchmarks = new IterationBenchmarks();
		benchmarks.setup();
	}

	@Test
	public void listOptimizedForLoop() {
		testThroughput("list optimized for loop", new Runnable() {
			@Override
			public void run() {
				benchmarks.listOptimizedForLoop();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void arrayStandardForLoop() {
		testThroughput("array for loop", new Runnable() {
			@Override
			public void run() {
				benchmarks.arrayStandardForLoop();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void linkedNodeForLoop() {
		testThroughput("linked node for loop", new Runnable() {
			@Override
			public void run() {
				benchmarks.linkedNodeForLoop();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void linkedNodeIteratorOptimizedForLoop() {
		testThroughput("linked node iterator optimized for loop", new Runnable() {
			@Override
			public void run() {
				benchmarks.linkedNodeIteratorOptimizedForLoop();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void linkedNodeDoWhile() {
		testThroughput("linked node do while loop", new Runnable() {
			@Override
			public void run() {
				benchmarks.linkedNodeDoWhile();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void linkedNodeResettingIteratorWhileLoop() {
		testThroughput("linked node resetting iterator while loop", new Runnable() {
			@Override
			public void run() {
				benchmarks.linkedNodeResettingIteratorWhileLoop();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

}
