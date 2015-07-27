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
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.util.SocketUtils;
import reactor.aeron.processor.AeronProcessor;
import reactor.io.buffer.Buffer;

import java.util.concurrent.TimeUnit;

/**
 * @author Anatoly Kadyshev
 */
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(value = 3, jvmArgs = { "-Xmx1g" })
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class AeronProcessorBenchmarks {

	private String CHANNEL = "udp://localhost:" + SocketUtils.findAvailableUdpPort();

	private int STREAM_ID = 10;

	private String CHANNEL_2 = "udp://localhost:" + SocketUtils.findAvailableUdpPort();

	private int STREAM_ID_2 = 20;

	private AeronProcessor sharedProcessor;

	private AeronProcessor singleThreadedProcessor;

	private Buffer buffer;

	@Setup
	public void setup() {
		buffer = Buffer.wrap("Event");

		sharedProcessor = AeronProcessor.share("aeron", true, true, CHANNEL, STREAM_ID);
		sharedProcessor.onSubscribe(new DummySubscription());
		sharedProcessor.subscribe(new DummySubscriber());

		singleThreadedProcessor = AeronProcessor.create("aeron", true, true, CHANNEL_2, STREAM_ID_2);
		singleThreadedProcessor.onSubscribe(new DummySubscription());
		singleThreadedProcessor.subscribe(new DummySubscriber());
	}

	@TearDown
	public void teardown() {
		sharedProcessor.shutdown();
		singleThreadedProcessor.shutdown();
	}

	@Benchmark
	public void sharedOnNext() {
		sharedProcessor.onNext(buffer);
		buffer.flip();
	}

	@Benchmark
	public void singleThreadedOnNext() {
		singleThreadedProcessor.onNext(buffer);
		buffer.flip();
	}

	private static class DummySubscription implements Subscription {
		@Override
        public void request(long n) {
        }

		@Override
        public void cancel() {
        }
	}

	private static class DummySubscriber implements Subscriber<Buffer> {
		@Override
        public void onSubscribe(Subscription s) {
            s.request(Long.MAX_VALUE);
        }

		@Override
        public void onNext(Buffer buffer) {
        }

		@Override
        public void onError(Throwable t) {
        }

		@Override
        public void onComplete() {
        }
	}
}
