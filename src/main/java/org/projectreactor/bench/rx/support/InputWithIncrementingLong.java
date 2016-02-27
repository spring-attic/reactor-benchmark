/*
 * Copyright (c) 2011-2016 Pivotal Software Inc., Inc. All Rights Reserved.
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

import java.util.Iterator;
import java.util.function.Consumer;

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.subscriber.SignalEmitter;
import reactor.core.subscriber.Subscribers;
import reactor.rx.Fluxion;

/**
 * Adapted from https://github.com/ReactiveX/RxJava/blob/1.x/src/perf/java/rx/jmh/InputWithIncrementingInteger.java
 *
 * @author Stephane Maldini
 */
public abstract class InputWithIncrementingLong {

	public Iterable<Long>   iterable;
	public Fluxion<Integer>     observable;
	public Fluxion<Long>     firehose;
	public Blackhole           bh;
	public Subscriber<Integer> observer;


	public abstract int getSize();

	@Setup
	public void setup(final Blackhole bh) {
		this.bh = bh;
		observable = Fluxion.range(0, getSize());

		firehose = Fluxion.yield(new Consumer<SignalEmitter<Long>>() {
			@Override
			public void accept(SignalEmitter<Long> s) {
				for (long i = 0; i < getSize(); i++) {
					s.onNext(i);
				}
				s.onComplete();
			}
		});

		iterable = new Iterable<Long>() {

			@Override
			public Iterator<Long> iterator() {
				return new Iterator<Long>() {

					long i = 0;

					@Override
					public boolean hasNext() {
						return i < getSize();
					}

					@Override
					public Long next() {
						return i++;
					}

					@Override
					public void remove() {

					}

				};
			}

		};

		observer = new IntegerSubscriber(bh);

		postSetup();

	}

	protected void postSetup() {

	}

	public LatchedCallback<Integer> newLatchedCallback() {
		return new LatchedCallback<>(bh);
	}

	public Subscriber<Integer> newSubscriber() {
		return Subscribers.consumer(new Consumer<Integer>() {
			@Override
			public void accept(Integer integer) {
				bh.consume(integer);
			}
		});
	}

	private static class IntegerSubscriber implements Subscriber<Integer> {
		private final Blackhole bh;

		public IntegerSubscriber(Blackhole bh) {
			this.bh = bh;
		}

		@Override
		public void onSubscribe(Subscription s) {
			s.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Integer integer) {
			bh.consume(integer);
		}

		@Override
		public void onError(Throwable t) {
		}

		@Override
		public void onComplete() {
		}

	}
}
