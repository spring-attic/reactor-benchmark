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
import reactor.core.Environment;
import reactor.event.Event;
import reactor.event.dispatch.Dispatcher;
import reactor.event.dispatch.RingBufferDispatcher;
import reactor.event.dispatch.ThreadPoolExecutorDispatcher;
import reactor.event.dispatch.WorkQueueDispatcher;
import reactor.event.routing.ArgumentConvertingConsumerInvoker;
import reactor.event.routing.ConsumerFilteringRouter;
import reactor.event.routing.Router;
import reactor.filter.PassThroughFilter;
import reactor.function.Consumer;
import reactor.jarjar.com.lmax.disruptor.YieldingWaitStrategy;
import reactor.jarjar.com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jon Brisbin
 * @author Stephane Maldini
 */
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class DispatcherBenchmarks {

	static int BACKLOG = 2048;

	Router                  eventRouter;
	RingBufferDispatcher         ringBufferDispatcher;
	WorkQueueDispatcher          workQueueDispatcher;
	ThreadPoolExecutorDispatcher threadPoolExecutorDispatcher;
	Event<?>                     event;
	AtomicLong                   counter;
	Consumer<Event<?>>           consumer;

	@Setup
	public void setup() {
		event = Event.wrap("Hello World!");
		counter = new AtomicLong(0);

		eventRouter = new ConsumerFilteringRouter(
				new PassThroughFilter(),
				new ArgumentConvertingConsumerInvoker(null)
		);

		ringBufferDispatcher = new RingBufferDispatcher(
				"ringBufferDispatcher",
				BACKLOG,
				null,
				ProducerType.SINGLE,
				new YieldingWaitStrategy()
		);
		workQueueDispatcher = new WorkQueueDispatcher(
				"workQueueDispatcher",
				Environment.PROCESSORS,
				BACKLOG,
				null,
				ProducerType.SINGLE,
				new YieldingWaitStrategy()
		);
		threadPoolExecutorDispatcher = new ThreadPoolExecutorDispatcher(
				Environment.PROCESSORS,
				BACKLOG,
				"threadPoolExecutorDispatcher"
		);

		consumer = new Consumer<Event<?>>() {
			@Override
			public void accept(Event<?> event) {
				counter.incrementAndGet();
			}
		};
	}

	@GenerateMicroBenchmark
	public void ringBuffer() {
		doTest(ringBufferDispatcher);
	}

	@GenerateMicroBenchmark
	public void workQueue() {
		doTest(workQueueDispatcher);
	}

	@GenerateMicroBenchmark
	public void threadPoolExecutor() {
		doTest(threadPoolExecutorDispatcher);
	}

	private void doTest(Dispatcher dispatcher) {
		dispatcher.dispatch(
				event,
				eventRouter,
				consumer,
				null
		);
	}

}
