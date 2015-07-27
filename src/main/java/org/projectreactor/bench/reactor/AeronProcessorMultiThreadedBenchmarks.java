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
@Threads(2)
@State(Scope.Group)
public class AeronProcessorMultiThreadedBenchmarks {

	private String CHANNEL = "udp://localhost:" + SocketUtils.findAvailableUdpPort();

	private int STREAM_ID = 10;

	private AeronProcessor processor;

	private Buffer buffer1;

	private Buffer buffer2;

	@Setup
	public void setup() {
		buffer1 = Buffer.wrap("Event");

		buffer2 = Buffer.wrap("Event");

		processor = AeronProcessor.share("aeron", true, true, CHANNEL, STREAM_ID);
		processor.onSubscribe(new DummySubscription());
		processor.subscribe(new DummySubscriber());
	}

	@TearDown
	public void teardown() {
		processor.shutdown();
	}

	@Benchmark
	@Group("onNext")
	public void first() {
		processor.onNext(buffer1);
		buffer1.flip();
	}

	@Benchmark
	@Group("onNext")
	public void second() {
		processor.onNext(buffer2);
		buffer2.flip();
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
