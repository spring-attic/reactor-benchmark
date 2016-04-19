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
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.Computations;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-Xmx1024m"})
@State(Scope.Thread)
public class HotComparison {

	@Param({ "1", "2", "4"})
	public int subscribers;

	PublishSubject<Integer> rxJust;
	PublishSubject<Integer> rxJustAsync;

	FluxProcessor<Integer, Integer> rcJust;
	FluxProcessor<Integer, Integer> rcJustAsync;

	LatchedRxObserver asyncRxObserver;
	LatchedObserver   asyncObserver;

	@Setup(Level.Iteration)
	public void setup(Blackhole bh) throws InterruptedException {
		//  Timer.global();

		rxJust = PublishSubject.create();
		for(int i = 0; i < subscribers; i++) {
			rxJust.onBackpressureBuffer()
			      .subscribe(new LatchedRxObserver(bh));
		}

		rxJustAsync = PublishSubject.create();
		asyncRxObserver = new LatchedRxObserver(bh);
		rxJustAsync.onBackpressureBuffer()
		           .observeOn(Schedulers.computation())
		           .subscribe(asyncRxObserver);

		for(int i = 1; i < subscribers; i++) {
			rxJustAsync.onBackpressureBuffer()
			           .observeOn(Schedulers.computation())
			           .subscribe(new LatchedRxObserver(bh));
		}

		rcJust = EmitterProcessor.create(256);
		for(int i = 0; i < subscribers; i++) {
			rcJust.subscribe(new LatchedObserver(bh));
		}
		rcJust.connect();

		asyncObserver = new LatchedObserver(bh);
		rcJustAsync = EmitterProcessor.async(Computations.single());
		rcJustAsync.subscribe(asyncObserver);

		for(int i = 1; i < subscribers; i++) {
			rcJustAsync.subscribe(new LatchedObserver(bh));
		}
		rcJustAsync.connect();
	}

	@TearDown(Level.Iteration)
	public void doTeardown() throws InterruptedException {
		rxJust.onCompleted();

		rcJust.onComplete();

		rxJustAsync.onCompleted();
		asyncRxObserver.cdl.await();

		rcJustAsync.onComplete();
		asyncObserver.cdl.await();
	}

	static final Integer DATA = 0;

	@Benchmark
	public void rxJust() {
		rxJust.onNext(DATA);
	}

	@Benchmark
	public void rcJust() {
		rcJust.onNext(DATA);
	}

	@Benchmark
	public void rxJustAsync() throws Exception {
		rxJustAsync.onNext(DATA);
	}

	@Benchmark
	public void rcJustAsync() throws Exception {
		rcJustAsync.onNext(DATA);
	}

	@Benchmark
	public void rx() throws Exception {
		Subject<Integer, Integer> s = PublishSubject.create();
		for(int i = 0; i < subscribers; i++){
			s.onBackpressureBuffer().subscribe(new LatchedRxObserver(null));
		}
	}

	@Benchmark
	public void rc() throws Exception {
		Processor<Integer, Integer> p = EmitterProcessor.create();
		for(int i = 0; i < subscribers; i++){
			p.subscribe(new LatchedObserver(null));
		}
	}

	static final class LatchedObserver implements Subscriber<Object> {

		final CountDownLatch cdl;

		final Blackhole bh;

		Subscription s;

		public LatchedObserver(Blackhole bh) {
			cdl = new CountDownLatch(1);
			this.bh = bh;
		}

		@Override
		public void onSubscribe(Subscription s) {
			this.s = s;
			s.request(1L);
		}

		@Override
		public void onNext(Object t) {
			bh.consume(t);
			s.request(1L);
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
		public void onStart() {
			request(1L);
		}

		@Override
		public void onNext(Object t) {
			bh.consume(t);
			request(1L);
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