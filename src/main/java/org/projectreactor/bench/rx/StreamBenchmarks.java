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

package org.projectreactor.bench.rx;

import org.openjdk.jmh.annotations.*;
import reactor.Environment;
import reactor.core.Dispatcher;
import reactor.core.processor.RingBufferProcessor;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.broadcast.Broadcaster;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
@Measurement(iterations = StreamBenchmarks.ITERATIONS, time = 5)
@Warmup(iterations = 0)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@OperationsPerInvocation(100)
public class StreamBenchmarks {

	public final static int ITERATIONS = 5;

	@Param({"100"})
	public int    elements;
	@Param({"sync", "shared", "partitioned"})
	public String dispatcher;

	private Environment          env;
	private CountDownLatch       latch;
	private int[]                data;
	private Broadcaster<Integer>   deferred;
	private Broadcaster<Integer> mapManydeferred;

	@Setup
	public void setup() {
		env = Environment.initializeIfEmpty();

		switch (dispatcher) {
			case "partitioned":
				deferred = Broadcaster.create();
				/*deferred.partition(2).consume(
						stream -> stream
								.dispatchOn(env.getCachedDispatcher())
								.map(i -> i)
								.scan(1, (last, next) -> last + next)
								.consume(i -> latch.countDown(), Throwable::printStackTrace)
								);*/
				Stream<Integer> s = deferred
						.process(RingBufferProcessor.create(Executors.newSingleThreadExecutor(), 2048));
				for(int v = 0; v < 1; v++){
					s.map(i -> i)
							.scan(1, (last, next) -> last + next)
							.consume(i -> latch.countDown(), Throwable::printStackTrace);
				}


				mapManydeferred = Broadcaster.create();
				mapManydeferred
						.partition(2)
						.consume(substream -> substream
								.dispatchOn(env.getCachedDispatcher())
								.map(i -> i)
								.consume(i -> latch.countDown(), Throwable::printStackTrace));

				break;
			default:
				final Dispatcher deferredDispatcher = dispatcher.equals("shared") ?
						env.getCachedDispatcher() :
						env.getDispatcher(dispatcher);

				deferred = Broadcaster.create(env, deferredDispatcher);
				deferred
						.map(i -> i)
						.scan(1, (last, next) -> last + next)
						.consume(i -> latch.countDown());

				mapManydeferred = Broadcaster.create();
				mapManydeferred
						.flatMap(Streams::just)
						.consume(i -> latch.countDown());
		}

		data = new int[elements];
		for (int i = 0; i < elements; i++) {
			data[i] = i;
		}
	}

	@TearDown
	public void tearDown() throws InterruptedException {
		Environment.terminate();
	}

	@Benchmark
	public void composedStream() throws InterruptedException {
		latch = new CountDownLatch(data.length);
		for (int i : data) {
			deferred.onNext(i);
		}
		if (!latch.await(30, TimeUnit.SECONDS)) throw new RuntimeException(deferred.debug().toString());
	}

	@Benchmark
	public void composedMapManyStream() throws InterruptedException {
		latch = new CountDownLatch(data.length);
		for (int i : data) {
			mapManydeferred.onNext(i);
		}
		if (!latch.await(30, TimeUnit.SECONDS)) throw new RuntimeException(mapManydeferred.debug().toString());
	}

}
