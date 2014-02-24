package org.projectreactor.bench.reactor;

import org.junit.Test;
import org.projectreactor.bench.AbstractThroughputTests;

/**
 * @author Jon Brisbin
 */
public class MapTests extends AbstractThroughputTests {

	MapBenchmarks benchmarks;

	@Override
	protected void doSetup() {
		benchmarks = new MapBenchmarks();
		benchmarks.setup();
	}

	@Override
	protected long getTimeout() {
		return 5000;
	}

	@Test
	public void hashMapIntegerKey() {
		testThroughput("hash map int key", new Runnable() {
			@Override
			public void run() {
				benchmarks.hashMapIntegerKey();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void observableMapIntegerKey() {
		testThroughput("observable map int key", new Runnable() {
			@Override
			public void run() {
				benchmarks.observableMapIntegerKey();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void bogStandardBinarySearch() {
		testThroughput("standard binary search", new Runnable() {
			@Override
			public void run() {
				benchmarks.bogStandardBinarySearch();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void optimizedBinarySearch() {
		testThroughput("optimized binary search", new Runnable() {
			@Override
			public void run() {
				benchmarks.optimizedBinarySearch();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

	@Test
	public void partitionedOptimizedBinarySearch() {
		testThroughput("partitioned optimized binary search", new Runnable() {
			@Override
			public void run() {
				benchmarks.partitionedOptimizedBinarySearch();
				counter.addAndGet(IterationBenchmarks.ITEMS);
			}
		});
	}

}
