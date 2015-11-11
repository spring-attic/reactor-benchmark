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

import reactor.io.buffer.Buffer;

import java.util.concurrent.TimeUnit;

/**
 * @author Anatoly Kadyshev
 */
abstract public class AeronBenchmark {

	private final int n;

	private final int signalLengthBytes;

	private final boolean useSingleDriverInstance;

	private static final int DELAY_MILLIS = 1000;

	private AeronTestInfra serverInfra;

	private AeronTestInfra clientInfra;

	private Buffer[] buffers;

	private SubscriberForBenchmark subscriber;

	private int i = 0;

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
		subscriber = new SubscriberForBenchmark();
		doSubscribeClient(subscriber);
		Thread.sleep(DELAY_MILLIS);
	}

	protected abstract void doSubscribeClient(SubscriberForBenchmark clientSubscriber);

	private void sendAndReceiveInitialSignals() throws InterruptedException {
		doSendInitialSignal();

		System.out.println("Initial signal sent");
		if (!subscriber.awaitNextSignal(5000)) {
			throw new IllegalStateException("The client didn't receive initial signal");
		}
		System.out.println("Initial signal received");
	}

	protected abstract void doSendInitialSignal();

	private void launchClient() throws Exception {
		if (useSingleDriverInstance) {
			clientInfra = serverInfra;
		} else {
			clientInfra = new AeronTestInfra("client");
			clientInfra.initialize();
		}

		doLaunchClient(clientInfra);
	}

	abstract protected void doLaunchClient(AeronTestInfra clientInfra);

	private void launchServer() throws Exception {
		serverInfra = new AeronTestInfra("server");
		serverInfra.initialize();

		doLaunchServer(serverInfra);
	}

	abstract protected void doLaunchServer(AeronTestInfra serverInfra);

	public void tearDownServer() throws Exception {
		doTearDownServer();
		Thread.sleep(DELAY_MILLIS);

		serverInfra.shutdown();
	}

	abstract protected void doTearDownServer();

	public void tearDownClient() throws Exception {
		doTearDownClient();
		Thread.sleep(DELAY_MILLIS);

		if (clientInfra != serverInfra) {
			clientInfra.shutdown();
		}
	}

	abstract protected void doTearDownClient();

	public void tearDown() throws Exception {
		awaitTillAllEventsAreReceived();

		tearDownClient();
		tearDownServer();
	}

	public void awaitTillAllEventsAreReceived() throws InterruptedException {
		System.out.println("Signals received: " + subscriber.getAndResetNextSignalCounter());

		do {
			Thread.sleep(100);
		} while (subscriber.getAndResetNextSignalCounter() > 0);
	}

	public void onNext() {
		doOnNext(buffers[i++]);
	}

	abstract protected void doOnNext(Buffer buffer);

	public int getNEventsReceived() {
		return subscriber.getNextSignalCounter();
	}

	public void runAndPrintResults() throws Exception {
		setup();

		long start = System.nanoTime();
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
		while (getNEventsReceived() < n) {
			if (System.nanoTime() - spinStart >= TimeUnit.SECONDS.toNanos(1)) {
				System.out.println("Waiting for all signals, received: " + getNEventsReceived());
				spinStart = System.nanoTime();
			}
		}
	}

	private void sendAllSignals() {
		for(int i = 0; i < n; i++) {

			if (i % 100_000 == 0) {
				int nSignalsReceived = subscriber.getNextSignalCounter();
				System.out.printf("Signals sent: %d, received: %d, completed: %.0f%%%n",
						i, nSignalsReceived, (double) nSignalsReceived / n * 100);
			}

			onNext();
		}
	}

}
