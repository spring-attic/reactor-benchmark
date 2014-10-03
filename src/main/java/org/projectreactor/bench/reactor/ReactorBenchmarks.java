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

package org.projectreactor.bench.reactor;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import reactor.core.Environment;
import reactor.core.Reactor;
import reactor.core.spec.Reactors;
import reactor.event.Event;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static reactor.event.selector.Selectors.$;

/**
 * @author Jon Brisbin
 */
@Measurement(iterations = 5, time = 5)
@Warmup(iterations = 5)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ReactorBenchmarks {

	@Param({"100", "1000", "10000"})
	public int    numOfSelectors;
	@Param({"sync", "ringBuffer", "workQueue", "threadPoolExecutor"})
	public String dispatcher;

	private Environment    env;
	private CountDownLatch latch;
	private Reactor        reactor;
	private Object[]       keys;
	private Event<?>       event;

	@Setup
	public void setup() {
		env = new Environment();
		reactor = Reactors.reactor(env, dispatcher);
		event = new Event<>(null);
		latch = new CountDownLatch(numOfSelectors);
		keys = new Object[numOfSelectors];
		for (int i = 0; i < numOfSelectors; i++) {
			keys[i] = new Object();
			reactor.on($(keys[i]), event -> latch.countDown());
		}
	}

	@TearDown
	public void tearDown() {
		env.shutdown();
	}

	@Benchmark
	public void reactorThroughput(Blackhole bh) throws InterruptedException {
		for (int i = 0; i < numOfSelectors; i++) {
			reactor.notify(keys[i], event);
			bh.consume(i);
		}

		assert latch.await(30, TimeUnit.SECONDS);
	}

}
