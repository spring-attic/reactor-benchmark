/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectreactor.bench.composable;

import org.openjdk.jmh.annotations.*;
import reactor.core.Environment;
import reactor.event.dispatch.Dispatcher;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.ParallelAction;
import reactor.tuple.Tuple2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
@Measurement(iterations = MergeBenchmarks.ITERATIONS, time = 5)
@Warmup(iterations = 0)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@OperationsPerInvocation(1)
public class MergeBenchmarks {

	public final static int ITERATIONS = 5;

	@Param({"100"})
	public int    elements;
	@Param({"sync", "ringBuffer", "partitioned"})
	public String dispatcher;

	private Environment     env;
	private CountDownLatch  latch;
	private Stream<Integer> deferred;
	private Stream<Integer> mapManydeferred;

	@Setup
	public void setup() {
		env = new Environment();

		switch (dispatcher) {
			case "partitioned":
				ParallelAction<Integer> parallelStream = Streams.<Integer>parallel(env);
				parallelStream
						.consume(stream -> stream
										.map(i -> i)
										.scan((Tuple2<Integer, Integer> tup) -> {
											int last = (null != tup.getT2() ? tup.getT2() : 1);
											return last + tup.getT1();
										})
										.consume(i -> latch.countDown())
						);

				deferred = Streams.defer(env);
				deferred.connect(parallelStream);

				mapManydeferred = Streams.<Integer>defer(env);
				mapManydeferred
						.parallel()
						.consume(substream -> substream.consume(i -> latch.countDown()));
				break;
			default:
				final Dispatcher deferredDispatcher = dispatcher.equals("ringBuffer") ?
						env.getDefaultDispatcherFactory().get():
						env.getDispatcher(dispatcher);

				deferred = Streams.<Integer>defer(env, deferredDispatcher);
				deferred
						.map(i -> i)
						.scan((Tuple2<Integer, Integer> tup) -> {
							int last = (null != tup.getT2() ? tup.getT2() : 1);
							return last + tup.getT1();
						})
						.consume(i -> latch.countDown());

				final Dispatcher flatMapDispatcher = dispatcher.equals("ringBuffer") ?
						env.getDefaultDispatcher():
						env.getDispatcher(dispatcher);

				mapManydeferred = Streams.<Integer>defer(env, deferredDispatcher);
				mapManydeferred
						.flatMap(i -> Streams.defer(env, flatMapDispatcher, i))
						.consume(i -> latch.countDown());
		}

	}

	@TearDown
	public void tearDown() throws InterruptedException {
		env.shutdown();
	}

	public void merge1StreamOfN() throws InterruptedException {
		Stream<Stream<Integer>> os = Streams.defer(1).map(i ->
						Streams.range(0, elements)
		);

		LatchedObserver<Integer> o = input.newLatchedObserver();
		Streams.merge(os).drain(o);
		latch.await();
	}


	@GenerateMicroBenchmark
	public void composedStream() throws InterruptedException {
		latch = new CountDownLatch(data.length);
		for (int i : data) {
			deferred.broadcastNext(i);
		}
		if (!latch.await(30, TimeUnit.SECONDS)) throw new RuntimeException(deferred.debug().toString());
	}
}
