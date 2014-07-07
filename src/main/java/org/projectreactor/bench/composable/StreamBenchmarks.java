/*
 * Copyright (c) 2011-2014 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import reactor.rx.Stream;
import reactor.rx.spec.Streams;
import reactor.tuple.Tuple2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
@Measurement(iterations = 5, time = 5)
@Warmup(iterations = 0)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class StreamBenchmarks {

	@Param({"100"})
	public int    iterations;
	@Param({"sync", "ringBuffer", "partitioned"})
	public String dispatcher;

	private Environment     env;
	private CountDownLatch  latch;
	private int[]           data;
	private Stream<Integer> deferred;
	private Stream<Integer> mapManydeferred;

	@Setup
	public void setup() {
		env = new Environment();
		latch = new CountDownLatch(iterations);
		switch (dispatcher) {
			case "partitioned":
				deferred = Streams.<Integer>defer();
				deferred
						.parallel()
						.consume(stream -> stream
								.map(i -> i)
								.scan((Tuple2<Integer, Integer> tup) -> {
									int last = (null != tup.getT2() ? tup.getT2() : 1);
									return last + tup.getT1();
								})
								.consume(i -> latch.countDown())
						);

				mapManydeferred = Streams.<Integer>defer();
				mapManydeferred
						.parallel()
						.map(substream -> substream.consume(i -> latch.countDown())).available();
				break;
			default:
				deferred = Streams.<Integer>defer(env, env.getDispatcher(dispatcher));
				deferred
						.map(i -> i)
						.scan((Tuple2<Integer, Integer> tup) -> {
							int last = (null != tup.getT2() ? tup.getT2() : 1);
							return last + tup.getT1();
						})
						.consume(i -> latch.countDown());

				mapManydeferred = Streams.<Integer>defer(env, env.getDispatcher(dispatcher));
				mapManydeferred
						.flatMap(i -> Streams.defer(i, env, env.getDispatcher(dispatcher)))
						.consume(i -> latch.countDown());
		}

		data = new int[iterations];
		for (int i = 0; i < iterations; i++) {
			data[i] = i;
		}
	}

	@TearDown
	public void tearDown() {
		env.shutdown();
	}

	@GenerateMicroBenchmark
	public void composedStream() throws InterruptedException {
		for (int i : data) {
			deferred.broadcastNext(i);
		}

		assert latch.await(30, TimeUnit.SECONDS);
	}

	@GenerateMicroBenchmark
	public void composedMapManyStream() throws InterruptedException {
		for (int i : data) {
			mapManydeferred.broadcastNext(i);
		}

		assert latch.await(30, TimeUnit.SECONDS);
	}

}
