/*
 * Copyright (c) 2011-2016 Pivotal Software, Inc. All Rights Reserved.
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

package org.projectreactor.bench.aeron;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.aeron.publisher.AeronPublisher;
import reactor.aeron.subscriber.AeronSubscriber;
import reactor.core.subscriber.test.TestSubscriber;
import reactor.io.buffer.Buffer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
		testSubscriber = TestSubscriber.createWithTimeoutSecs(5);
		publisher.subscribe(testSubscriber);
		Thread.sleep(DELAY_MILLIS);
	}

	private void sendAndReceiveInitialSignals() throws InterruptedException {
		testSubscriber.request(1);

		sendInitialSignal();

		System.out.println("Initial signal sent");

		testSubscriber.assertNumNextSignalsReceived(1);

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

		publisher = AeronPublisher.create(clientInfra.newContext());

		Publisher<Buffer> publisher = new Publisher<Buffer>() {
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
		publisher.subscribe(subscriber);
	}

	private void launchServer() throws Exception {
		serverInfra = new AeronTestInfra("subscriber");
		serverInfra.initialize();

		subscriber = AeronSubscriber.create(serverInfra.newContext());
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
		long spinStart = System.nanoTime();
		while (testSubscriber.getNumNextSignalsReceived() < n + 1) {
			if (System.nanoTime() - spinStart >= TimeUnit.SECONDS.toNanos(1)) {
				System.out.println("Waiting for all signals, received: " + testSubscriber.getNumNextSignalsReceived());
				spinStart = System.nanoTime();
			}
		}
	}

	private void sendAllSignals() {
		int to = 0;
		int from;
		do {
			from = to;
			to = (int) Math.min(demand.get() - 1, n);

			for (int i = from; i < to; i++) {

				if (i % 100_000 == 0) {
					long numSignalsReceived = testSubscriber.getNumNextSignalsReceived();
					System.out.printf("Signals sent: %d, received: %d, completed: %.0f%%%n",
							i, numSignalsReceived, (double) numSignalsReceived / n * 100);
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
