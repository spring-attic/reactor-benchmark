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

package org.projectreactor.bench.rx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.ProcessorGroup;
import reactor.core.publisher.Processors;
import reactor.rx.Stream;
import rx.Observable;
import rx.schedulers.Schedulers;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-Xmx1024m"})
@State(Scope.Thread)
public class ZipComparison {

	@Param({"1000000"})
	public int times;

//    Observable<Integer> rxJust;
//    Observable<Integer> rxRange;
//    Observable<Integer> rxJustAsync;
//    Observable<Integer> rxRangeAsync;

	Publisher<Integer>     rcJust;
	Publisher<Integer>     rcRange;
	Publisher<Integer>     rcJustAsync;
	Publisher<Integer>     rcRangeAsync;

	Observable<Integer>     rxJust;
	Observable<Integer>     rxRange;
	Observable<Integer>     rxJustAsync;
	Observable<Integer>     rxRangeAsync;

	ProcessorGroup<Object> processor;

	static final Publisher<Integer>  p1 = Stream.just(1);//.map(d -> d);
	static final Publisher<Integer>  p2 = Stream.just(2);//.map(d -> d);
	static final Observable<Integer> o1 = Observable.just(1);
	static final Observable<Integer> o2 = Observable.just(2);

	@Setup(Level.Iteration)
	public void setup() {
		//  Timers.global();


		rcJust = Flux.zip(p1, p2, (t1, t2) -> t2);
		rcRange = Flux.zip(Stream.range(0, times), Stream.range(0, times), (t1, t2) -> t2);

		processor = Processors.asyncGroup("processor", 1024 * 64, 1, Throwable::printStackTrace, null, false);

		rcJustAsync = Stream.from(rcJust).dispatchOn(processor);
		rcRangeAsync = Stream.from(rcRange).dispatchOn(processor);

		rxJust = Observable.zip(o1, o2, (t1, t2) -> t2);
		rxRange = Observable.zip(Observable.range(0, times), Observable.range(0, times), (t1, t2) -> t2);

		rxJustAsync = rxJust.observeOn(Schedulers.computation());
		rxRangeAsync = rxRange.observeOn(Schedulers.computation());
	}

	@TearDown(Level.Iteration)
	public void doTeardown() {
		processor.shutdown();
	}


	@Benchmark
    public void rx(Blackhole bh) {
        Observable.zip(o1, o2, (t1, t2) -> t2).subscribe(createRxObserver(bh));
    }

    @Benchmark
    public void rxJust(Blackhole bh) {
        rxJust.subscribe(createRxObserver(bh));
    }

    @Benchmark
    public void rxRange(Blackhole bh) {
        rxRange.subscribe(createRxObserver(bh));
    }

	@Benchmark
	public void rc(Blackhole bh) {
		Flux.zip(p1, p2, (t1, t2) -> t2).subscribe(createObserver(bh));
	}

	@Benchmark
	public void rcJust(Blackhole bh) {
		rcJust.subscribe(createObserver(bh));
	}

	@Benchmark
	public void rcRange(Blackhole bh) {
		rcRange.subscribe(createObserver(bh));
	}

    @Benchmark
    public void rxJustAsync(Blackhole bh) throws Exception {
	    LatchedRxObserver o = new LatchedRxObserver(bh);
        rxJustAsync.subscribe(o);
        o.cdl.await();
    }

    @Benchmark
    public void rxRangeAsync(Blackhole bh) throws Exception {
	    LatchedRxObserver o = new LatchedRxObserver(bh);
        rxRangeAsync.subscribe(o);
        o.cdl.await();
    }

	@Benchmark
	public void rcJustAsync(Blackhole bh) throws Exception {
		LatchedObserver o = new LatchedObserver(bh);
		rcJustAsync.subscribe(o);
		o.cdl.await();
	}

	@Benchmark
	public void rcRangeAsync(Blackhole bh) throws Exception {
		LatchedObserver o = new LatchedObserver(bh);
		rcRangeAsync.subscribe(o);
		o.cdl.await();
	}

	private Subscriber<Object> createObserver(Blackhole bh) {
		return new Subscriber<Object>() {
			@Override
			public void onSubscribe(Subscription s) {
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Object t) {
				bh.consume(t);
			}

			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
			}

			@Override
			public void onComplete() {
			}
		};
	}

	private rx.Subscriber<Object> createRxObserver(Blackhole bh) {
		return new rx.Subscriber<Object>() {
			@Override
			public void onNext(Object t) {
				bh.consume(t);
			}

			@Override
			public void onError(Throwable t) {
				t.printStackTrace();
			}

			@Override
			public void onCompleted() {
			}
		};
	}

	static final class LatchedObserver implements Subscriber<Object> {

		final CountDownLatch cdl;

		final Blackhole bh;

		public LatchedObserver(Blackhole bh) {
			cdl = new CountDownLatch(1);
			this.bh = bh;
		}

		@Override
		public void onSubscribe(Subscription s) {
			s.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Object t) {
			bh.consume(t);
		}

		@Override
		public void onError(Throwable t) {
			t.printStackTrace();
			cdl.countDown();
		}

		@Override
		public void onComplete() {
			cdl.countDown();
		}

	}

	static final class LatchedRxObserver extends rx.Subscriber<Object> {

		final CountDownLatch cdl;

		final Blackhole bh;

		public LatchedRxObserver(Blackhole bh) {
			cdl = new CountDownLatch(1);
			this.bh = bh;
		}

		@Override
		public void onNext(Object t) {
			bh.consume(t);
		}

		@Override
		public void onError(Throwable t) {
			t.printStackTrace();
			cdl.countDown();
		}

		@Override
		public void onCompleted() {
			cdl.countDown();
		}

	}
}