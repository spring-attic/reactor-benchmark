/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
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

package org.projectreactor.bench.aeron;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.aeron.publisher.AeronFlux;
import reactor.aeron.subscriber.AeronSubscriber;
import reactor.core.test.TestSubscriber;
import reactor.io.buffer.Buffer;

/**
 * @author Anatoly Kadyshev
 */
public class AeronBenchmark {

	private static final int DELAY_MILLIS = 1000;

	private final int n;

	private final int signalLengthBytes;

	private final boolean useSingleDriverInstance;

	private AeronTestInfra serverInfra;

	private AeronTestInfra clientInfra;

	private Buffer[] buffers;

	protected Subscriber<Buffer> subscriber;

	protected Publisher<Buffer> publisher;

	protected TestSubscriber<String> testSubscriber;

	private final CountDownLatch requestReceived = new CountDownLatch(1);

	protected final AtomicLong demand = new AtomicLong(0);

	public AeronBenchmark(int n, int signalLengthBytes, boolean useSingleDriverInstance) {
		this.n = n;
		this.signalLengthBytes = signalLengthBytes;
		this.useSingleDriverInstance = useSingleDriverInstance;
	}

	public void setup() throws Exception {
		buffers = new BuffersFactory().populateBuffers(n, signalLengthBytes);
		launchServer();
		launchClient();
		createClientSubscriberAndSubscribe();
		sendAndReceiveInitialSignals();
	}

	private void createClientSubscriberAndSubscribe() throws InterruptedException {
		testSubscriber = TestSubscriber.<String>create(0).configureValuesStorage(false)
		                                          .configureValuesTimeout(Duration
				.ofSeconds(10L));
		Buffer.bufferToString(publisher).subscribe(testSubscriber);
		Thread.sleep(DELAY_MILLIS);
	}

	private void sendAndReceiveInitialSignals() throws InterruptedException {
		testSubscriber.request(1);

		sendInitialSignal();

		System.out.println("Initial signal sent");

		testSubscriber.awaitAndAssertNextValueCount(1);

		System.out.println("Initial signal received");
	}

	protected void sendInitialSignal() {
		try {
			if (!requestReceived.await(5, TimeUnit.SECONDS)) {
				throw new RuntimeException("No request received");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		subscriber.onNext(Buffer.wrap("Hello, world!!!"));
	}

	private void launchClient() throws Exception {
		if (useSingleDriverInstance) {
			clientInfra = serverInfra;
		} else {
			clientInfra = new AeronTestInfra("publisher");
			clientInfra.initialize();
		}

		publisher = AeronFlux.listenOn(clientInfra.newContext()
				.senderChannel("udp://localhost:12000")
				.receiverChannel("udp://localhost:12001"));
	}

	private void launchServer() throws Exception {
		serverInfra = new AeronTestInfra("subscriber");
		serverInfra.initialize();

		subscriber = AeronSubscriber.create(serverInfra.newContext()
				.senderChannel("udp://localhost:12000"));

		Publisher<Buffer> sourcePublisher = new Publisher<Buffer>() {
			@Override
			public void subscribe(Subscriber<? super Buffer> s) {
				s.onSubscribe(new Subscription() {
					@Override
					public void request(long n) {
						System.out.printf("[%s] - upstream subscription.request: %d\n",
								Thread.currentThread().getName(), n);

						demand.addAndGet(n);
						requestReceived.countDown();
					}

					@Override
					public void cancel() {
					}
				});
			}
		};
		sourcePublisher.subscribe(subscriber);

		Thread.sleep(DELAY_MILLIS);
	}

	public void tearDown() throws Exception {
		subscriber.onComplete();

		Thread.sleep(DELAY_MILLIS);

		if (clientInfra != serverInfra) {
			clientInfra.shutdown();
		}
		Thread.sleep(DELAY_MILLIS);

		serverInfra.shutdown();
	}

	public void runAndPrintResults() throws Exception {
		setup();

		long start = System.nanoTime();
		testSubscriber.request(n);
		sendAllSignals();
		awaitAllSignalsAreReceived();
		long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

		printResults(durationMillis);

		tearDown();
	}

	private void printResults(long durationMillis) {
		System.out.println("Test duration: " + durationMillis + " millis");

		double signalsPerSec = n / ((double) durationMillis / 1000);
		System.out.println("Signals per second: " + signalsPerSec);
		System.out.println("Mbit/sec: " + (signalsPerSec * signalLengthBytes * 8 / 1_000_000));
		System.out.println("MiB/sec: " + (signalsPerSec * signalLengthBytes / (1024 * 1024)));
	}

	private void awaitAllSignalsAreReceived() {
		testSubscriber.awaitAndAssertNextValueCount(n);
	}

	private void sendAllSignals() {
		int to = 0;
		int from;
		do {
			from = to;
			to = (int) Math.min(demand.get() - 1, n);

			for (int i = from; i < to; i++) {

				if (i % 100_000 == 0) {
					System.out.printf("Signals sent: %d\n", i);
				}

				try {
					subscriber.onNext(buffers[i]);
				} catch (Exception e) {
					System.out.println("from="+from+",to="+to+",demand="+demand.get());
				}
			}
		} while(to < n);
	}

}
