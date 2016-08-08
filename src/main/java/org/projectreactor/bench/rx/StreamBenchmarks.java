/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc. All Rights Reserved.
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.TopicProcessor;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

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
	@Param({"sync", "shared", "raw"})
	public String dispatcher;

	private CountDownLatch                  latch;
	private int[]                           data;
	private FluxProcessor<Integer, Integer> deferred;
	private FluxProcessor<Integer, Integer> mapManydeferred;
	private Scheduler                       partitionRunner;

	@Setup
	public void setup() {
		switch (dispatcher) {
			case "raw":
				deferred = TopicProcessor.create("test-w", 2048);
				/*deferred.partition(2).subscribe(
						stream -> stream
								.publishOn(env.getCachedDispatcher())
								.map(i -> i)
								.scan(1, (last, next) -> last + next)
								.subscribe(i -> latch.countDown(), Throwable::printStackTrace)
								);*/

				Flux.from(deferred)
				    .map(i -> i)
				    .scan(1, (last, next) -> last + next)
				    .subscribe(i -> latch.countDown(), Throwable::printStackTrace,
						  () -> System.out.println("complete test-w"));

				partitionRunner = Schedulers.newParallel("test", 2);

				mapManydeferred = UnicastProcessor.create();
				mapManydeferred
				  .parallel(2)
				  .runOn(partitionRunner)
				  .map(i -> i)
				  .subscribe(i -> latch.countDown(), Throwable::printStackTrace,
							() -> System.out.println("complete test"));

				break;

			case "shared":
				deferred = UnicastProcessor.create();
				deferred.publishOn(Schedulers.newParallel("test-deferred"))
				        .map(i -> i)
				        .scan(1, (last, next) -> last + next)
				        .subscribe(i -> latch.countDown());

				mapManydeferred = UnicastProcessor.create();
				mapManydeferred.publishOn(Schedulers.newParallel("test-flatmap"))
				               .flatMap(Flux::just)
				               .subscribe(i -> latch.countDown());
				break;

			default:
				deferred = UnicastProcessor.create();

				deferred
				      .map(i -> i)
				      .scan(1, (last, next) -> last + next)
				      .subscribe(i -> latch.countDown());

				mapManydeferred = UnicastProcessor.create();

				mapManydeferred.flatMap(Flux::just)
				               .subscribe(i -> latch.countDown());
		}

		data = new int[elements];
		for (int i = 0; i < elements; i++) {
			data[i] = i;
		}
	}

	@TearDown
	public void tearDown() throws InterruptedException {
		deferred.onComplete();
		mapManydeferred.onComplete();
		if(partitionRunner != null) {
			partitionRunner.shutdown();
		}
	}

	@Benchmark
	public void composedStream() throws InterruptedException {
		latch = new CountDownLatch(data.length);
		for (int i : data) {
			deferred.onNext(i);
		}
		if (!latch.await(30, TimeUnit.SECONDS)) throw new RuntimeException(deferred.toString());
	}

	@Benchmark
	public void composedMapManyStream() throws InterruptedException {
		latch = new CountDownLatch(data.length);
		for (int i : data) {
			mapManydeferred.onNext(i);
		}
		if (!latch.await(30, TimeUnit.SECONDS)) throw new RuntimeException(mapManydeferred.toString());
	}

}
