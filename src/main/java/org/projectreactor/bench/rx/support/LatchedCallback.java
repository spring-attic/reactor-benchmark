/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
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
package org.projectreactor.bench.rx.support;

import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Dispatcher;
import reactor.rx.action.support.NonBlocking;

import java.util.concurrent.CountDownLatch;

/**
 * @author Stephane Maldini
 */
public class LatchedCallback<T> implements Subscriber<T>, NonBlocking {

	public final CountDownLatch latch = new CountDownLatch(1);
	private final Blackhole bh;

	public LatchedCallback(Blackhole bh) {
		this.bh = bh;
	}

	@Override
	public void onSubscribe(Subscription s) {
		s.request(Long.MAX_VALUE);
	}

	@Override
	public void onComplete() {
		latch.countDown();
	}

	@Override
	public void onError(Throwable e) {
	}

	@Override
	public boolean isReactivePull(Dispatcher dispatcher, long producerCapacity) {
		return false;
	}

	@Override
	public void onNext(T t) {
		bh.consume(t);
	}

	@Override
	public long getCapacity() {
		return Long.MAX_VALUE;
	}
}
