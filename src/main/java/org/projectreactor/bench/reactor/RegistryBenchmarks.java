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
import reactor.event.Event;
import reactor.event.registry.CachingRegistry;
import reactor.event.registry.Registration;
import reactor.event.registry.Registry;
import reactor.event.selector.Selector;
import reactor.fn.Consumer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static reactor.event.selector.Selectors.$;

/**
 * @author Jon Brisbin
 */
@Measurement(iterations = 5)
@Warmup(iterations = 5)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class RegistryBenchmarks {

	@Param({"10", "100", "1000", "10000"})
	public  int                          length;
	private Registry<Consumer<Event<?>>> registry;
	private CountDownLatch               latch;

	@SuppressWarnings("unchecked")
	@Setup
	public void setup() {
		latch = new CountDownLatch(length);
		registry = new CachingRegistry<>();

		int j = 0;
		for (int i = 0; i < length; i++) {
			if (i % 10 == 0) {
				j++;
			}
			Selector sel = $("i=" + i + ",j=" + j);
			Consumer<Event<?>> consumer = ev -> latch.countDown();
			registry.register(sel, consumer);
		}
	}

	@SuppressWarnings("unchecked")
	@Benchmark
	public void registryThroughput(Blackhole bh) {
		int j = 0;
		for (int i = 0; i < length; i++) {
			if (i % 10 == 0) {
				j++;
			}
			for (Registration<? extends Consumer<Event<?>>> reg : registry.select("i=" + i + ",j=" + j)) {
				if (!reg.isCancelled()) {
					reg.getObject().accept(null);
				}
				bh.consume(reg);
			}
		}

		assert latch.getCount() == 0;
	}

}
