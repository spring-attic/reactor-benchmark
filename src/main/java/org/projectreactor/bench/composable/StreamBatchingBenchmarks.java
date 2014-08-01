package org.projectreactor.bench.composable;

import org.openjdk.jmh.annotations.*;
import reactor.core.Environment;
import reactor.rx.Stream;
import reactor.rx.spec.Streams;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * from https://gist.github.com/oiavorskyi/a949aa6ef3556246c42d
 * issue https://github.com/reactor/reactor/issues/358
 *
 * @author Oleg Iavorskyi
 */
@Measurement(iterations = StreamBatchingBenchmarks.ITERATIONS, time = 5)
@Warmup(iterations = 3)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class StreamBatchingBenchmarks {

	public final static int ITERATIONS = 5;

	@Param({"100", "1000", "10000"})
	public int     elements;
	@Param({"false", "true"})
	public boolean filter;

	private Environment    env;
	private String[]       data;
	private Stream<CountDownLatch> deferred;

	@Setup
	public void setup() {
		env = new Environment();

		deferred = Streams.<CountDownLatch>defer(env);
		deferred
				.parallel(2)
				.consume(stream -> (filter ? (stream
								.filter(i -> i.hashCode() != 0 ? true : true)) : stream)
								.buffer(elements/2)
								.timeout(100)
								.consume(batch -> {
									for (CountDownLatch latch : batch) latch.countDown();
								})
				);

		data = new String[elements];
		for (int i = 0; i < elements; i++) {
			data[i] = Integer.toString(i);
		}
	}

	@TearDown
	public void tearDown() throws InterruptedException {
		env.shutdown();
	}

	@GenerateMicroBenchmark
	public void composedStream() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(data.length);
		for (String i : data) {
			deferred.broadcastNext(latch);
		}
		if (!latch.await(30, TimeUnit.SECONDS))
			throw new RuntimeException(data.length + " elements trying to flow in, with latch count " + latch.getCount() +
					"\n " + deferred.debug().toString());
	}

}