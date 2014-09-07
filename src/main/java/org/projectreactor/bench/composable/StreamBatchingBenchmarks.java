package org.projectreactor.bench.composable;

import org.openjdk.jmh.annotations.*;
import reactor.core.Environment;
import reactor.function.Consumer;
import reactor.jarjar.com.lmax.disruptor.BlockingWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.dsl.ProducerType;
import reactor.rx.Stream;
import reactor.rx.spec.Streams;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

	private Environment            env;
	private String[]               data;
	private Stream<CountDownLatch> deferred;
	private CountDownLatch         latch = new CountDownLatch(8);

	@Setup
	public void setup() {
		env = new Environment();

		generateStream();

		//env.getRootTimer().schedule(i -> System.out.println(deferred.debug()), 200, TimeUnit.MILLISECONDS);

		data = new String[elements];
		for (int i = 0; i < elements; i++) {
			data[i] = Integer.toString(i);
		}
	}

	@TearDown
	public void tearDown() throws InterruptedException {
		env.shutdown();
	}

	@TearDown(Level.Iteration)
	public void cleanLatch() throws InterruptedException {
		deferred.broadcastComplete();
		if (!latch.await(30, TimeUnit.SECONDS))
			throw new RuntimeException(data.length + " elements trying to flow in, with latch count " + latch.getCount() +
					"\n " + deferred.debug().toString());

		generateStream();

	}

	@GenerateMicroBenchmark
	public void composedStream() throws InterruptedException {
		for (String i : data) {
			deferred.broadcastNext(latch);
		}
	}

	private void generateStream(){
		final Random random = new Random();

		deferred = Streams.<CountDownLatch>defer(env);

		//((WaitingMood)deferred.getDispatcher()).nervous();

		deferred
				.parallel(8, Environment.createDispatcherFactory("batch-stream",
						8,
						2048,
						null,
						ProducerType.SINGLE,
						new BlockingWaitStrategy()))
				//.monitorLatency(300)
				.consume(stream ->
								(filter ?
										stream.filter(i -> i.hashCode() != 0 ? true : true) :
										stream
								)
										.buffer(elements / 8)
										.timeout(1000)
										.consume(batch -> {
											try {
												Thread.sleep(random.nextInt(400) + 100);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
											for (CountDownLatch latch : batch) latch.countDown();

										}).finallyDo(new Consumer<Object>() {
									@Override
									public void accept(Object o) {
										latch.countDown();
									}
								})
				);
	}

}