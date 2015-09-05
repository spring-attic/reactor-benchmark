/*
 * Copyright (c) 2011-2015 GoPivotal, Inc. All Rights Reserved.
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
import org.springframework.util.SocketUtils;
import reactor.aeron.processor.AeronProcessor;
import reactor.io.buffer.Buffer;

import java.util.concurrent.TimeUnit;

/**
 * @author Anatoly Kadyshev
 */
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(value = 3, jvmArgs = { "-Xmx4g" })
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Threads(2)
@State(Scope.Group)
public class AeronProcessorMultiThreadedBenchmarks {

	private static final int N = 10000000;

	private String CHANNEL = "udp://localhost:" + SocketUtils.findAvailableUdpPort();

	private int STREAM_ID = 10;

	private AeronProcessor processor;

	private Buffer[] buffers1;

	private Buffer[] buffers2;

	private SubscriberForBenchmark subscriber;

	private int i1, i2;

	@Setup
	public void setup() throws InterruptedException {
		buffers1 = populateBuffers();
		buffers2 = populateBuffers();

		processor = AeronProcessor.builder()
				.name("aeron-multi")
				.launchEmbeddedMediaDriver(true)
				.channel(CHANNEL)
				.streamId(STREAM_ID)
				.errorStreamId(STREAM_ID + 1)
				.commandRequestStreamId(STREAM_ID + 2)
				.commandReplyStreamId(STREAM_ID + 3)
				.publicationLingerTimeoutMillis(250)
				.ringBufferSize(128 * 1024)
				.share();

		processor.onSubscribe(new SubscriptionForBenchmark());
		subscriber = new SubscriberForBenchmark();
		processor.subscribe(subscriber);

		processor.subscribe(subscriber);
		processor.onNext(Buffer.wrap("Event"));

		subscriber.awaitNextSignal(1000);
	}

	private Buffer[] populateBuffers() {
		Buffer[] buffers = new Buffer[N];
		for (int i = 0; i < N; i++) {
			buffers[i] = Buffer.wrap("M");
		}
		return buffers;
	}

	@TearDown
	public void teardown() {
		processor.shutdown();
	}

	@TearDown(Level.Iteration)
	public void afterIteration() throws InterruptedException {
		i1 = 0;
		i2 = 0;

		System.out.println("Events received: " + subscriber.getAndResetNextSignalCounter());

		do {
			Thread.sleep(100);
		} while (subscriber.getAndResetNextSignalCounter() > 0);
	}

	@Benchmark
	@Group("onNext")
	public void first() {
		processor.onNext(buffers1[i1++]);
	}

	@Benchmark
	@Group("onNext")
	public void second() {
		processor.onNext(buffers2[i2++]);
	}

}
