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

package org.projectreactor.bench.aeron;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.io.buffer.Buffer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Anatoly Kadyshev
 */
class SubscriberForBenchmark implements Subscriber<Buffer> {

	private final CountDownLatch nextSignalReceived = new CountDownLatch(1);

	private volatile int nextSignalCounter;

	@Override
	public void onSubscribe(Subscription s) {
		s.request(Long.MAX_VALUE);
	}

    @Override
	public void onNext(Buffer buffer) {
        nextSignalReceived.countDown();
		nextSignalCounter++;
	}

    @Override
	public void onError(Throwable t) {
	}

    @Override
	public void onComplete() {
	}

    public boolean awaitNextSignal(long timeoutMillis) throws InterruptedException {
        return nextSignalReceived.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

	public int getAndResetNextSignalCounter() {
		int counter = nextSignalCounter;
		nextSignalCounter = 0;
		return counter;
	}

    public int getNextSignalCounter() {
        return nextSignalCounter;
    }

}
