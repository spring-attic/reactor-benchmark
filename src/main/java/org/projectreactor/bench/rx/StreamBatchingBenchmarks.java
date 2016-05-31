package org.projectreactor.bench.rx;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * from https://gist.github.com/oiavorskyi/a949aa6ef3556246c42d
 * issue https://github.com/reactor/reactor/issues/358
 *
 * @author Oleg Iavorskyi
 * @author Stephane Maldini
 *
 */
@Measurement(iterations = StreamBatchingBenchmarks.ITERATIONS, time = 30)
@Warmup(iterations = 1)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@OperationsPerInvocation(1000)
public class StreamBatchingBenchmarks {

	public final static int ITERATIONS = 5;

	@Param({"1000"})
	public int     elements;
	@Param({"false", "true"})
	public boolean filter;

	private String[]                    data;
	private EmitterProcessor<CountDownLatch> deferred;
	private CountDownLatch latch = new CountDownLatch(8);

	@Setup
	public void setup() {
		generateStream();

		//env.getRootTimer().schedule(i -> System.out.println(deferred.debug()), 200, TimeUnit.MILLISECONDS);

		data = new String[elements];
		for (int i = 0; i < elements; i++) {
			data[i] = Integer.toString(i);
		}
	}

	@TearDown(Level.Iteration)
	public void cleanLatch() throws InterruptedException {
		deferred.onComplete();
		if (!latch.await(30, TimeUnit.SECONDS))
			throw new RuntimeException(data.length + " elements trying to flow in, with latch count " + latch.getCount() +
					"\n " + deferred.debug().toString());

		generateStream();

	}

	@Benchmark
	public void composedStream() throws InterruptedException {
		for (String i : data) {
			deferred.onNext(latch);
		}
	}

	private void generateStream() {
		final Random random = new Random();


		Scheduler dispatcherSupplier = Schedulers.newComputation("batch-stream", 9, 2048);

		deferred = EmitterProcessor.<CountDownLatch>create().connect();
		deferred
		        .publishOn(dispatcherSupplier)
				.partition(8)
				.subscribe(
				  stream ->
					(filter ?
					  stream.publishOn(dispatcherSupplier).filter(i -> i.hashCode() != 0 ? true : true) :
					  stream.publishOn(dispatcherSupplier)
					)
					  .buffer(elements / 8, 1000)
					  .subscribe(batch -> {
						  try {
							  Thread.sleep(random.nextInt(400) + 100);
						  } catch (InterruptedException e) {
							  e.printStackTrace();
						  }
						  for (CountDownLatch latch : batch) latch.countDown();

					  }, null, () -> latch.countDown())
				);
	}

}