/*
 * Copyright (c) 2011-2015 Pivotal Software Inc., Inc. All Rights Reserved.
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

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Dispatcher;
import reactor.core.dispatch.SynchronousDispatcher;
import reactor.fn.Consumer;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.terminal.ConsumerAction;
import reactor.rx.action.support.NonBlocking;

import java.util.Iterator;

/**
 * Adapted from https://github.com/ReactiveX/RxJava/blob/1.x/src/perf/java/rx/jmh/InputWithIncrementingInteger.java
 *
 * @author Stephane Maldini
 */
public abstract class InputWithIncrementingInteger {

	public Iterable<Integer>   iterable;
	public Stream<Integer>     observable;
	public Stream<Integer>     firehose;
	public Blackhole           bh;
	public Subscriber<Integer> observer;


	public abstract int getSize();

	@Setup
	public void setup(final Blackhole bh) {
		this.bh = bh;
		observable = Streams.range(0, getSize());

		firehose = Streams.create(new Publisher<Integer>() {

			@Override
			public void subscribe(Subscriber<? super Integer> s) {
				for (int i = 0; i < getSize(); i++) {
					s.onNext(i);
				}
				s.onComplete();
			}
		});

		iterable = new Iterable<Integer>() {

			@Override
			public Iterator<Integer> iterator() {
				return new Iterator<Integer>() {

					int i = 0;

					@Override
					public boolean hasNext() {
						return i < getSize();
					}

					@Override
					public Integer next() {
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
		return new ConsumerAction<>(SynchronousDispatcher.INSTANCE, new Consumer<Integer>() {
			@Override
			public void accept(Integer t) {
				bh.consume(t);
			}

		}, null, null);
	}

	private static class IntegerSubscriber implements Subscriber<Integer>, NonBlocking {
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

		@Override
		public boolean isReactivePull(Dispatcher dispatcher, long producerCapacity) {
			return false;
		}

		@Override
		public long getCapacity() {
			return Long.MAX_VALUE;
		}
	}
}
