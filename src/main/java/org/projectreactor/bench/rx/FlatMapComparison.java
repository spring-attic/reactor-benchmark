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
import reactor.core.publisher.SchedulerGroup;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = { "-Xmx1024m" })
@State(Scope.Thread)
public class FlatMapComparison {

    @Param({ "1000000" })
    public int times;

//    Observable<Integer> rxJust;
//    Observable<Integer> rxRange;
//    Observable<Integer> rxJustAsync;
//    Observable<Integer> rxRangeAsync;

    Publisher<Integer> rcJust;
    Publisher<Integer> rcRange;
    Publisher<Integer> rcJustAsync;
    Publisher<Integer> rcRangeAsync;
    SchedulerGroup processor;

    @Setup(Level.Iteration)
    public void setup() {
      //  Timer.global();

//        rxJust = Observable.range(0, times).flatMap(v -> Observable.just(1, v));
//        rxRange = Observable.range(0, times).flatMap(v -> Observable.range(v, 2));

//        rxJustAsync = rxJust.observeOn(Schedulers.single());
//        rxRangeAsync = rxRange.observeOn(Schedulers.single());

        rcJust = Flux.range(0, times).flatMap(Flux::just);
        rcRange = Flux.range(0, times).flatMap(v -> Flux.range(v, 2));

        processor = SchedulerGroup.async("processor", 1024 * 32, 1, null, null, false);

        rcJustAsync = Flux.range(0, times).flatMap(Flux::just)
                            .publishOn(processor);
        rcRangeAsync = Flux.from(rcRange).publishOn(processor);
    }

    @TearDown(Level.Iteration)
    public void doTeardown() {
        processor.shutdown();
    }

//    @Benchmark
//    public void rxJust(Blackhole bh) {
//        rxJust.subscribe(createObserver(bh));
//    }
//
//    @Benchmark
//    public void rxRange(Blackhole bh) {
//        rxRange.subscribe(createObserver(bh));
//    }

    @Benchmark
    public void rcJust(Blackhole bh) {
        rcJust.subscribe(createObserver(bh));
    }

    @Benchmark
    public void rcRange(Blackhole bh) {
        rcRange.subscribe(createObserver(bh));
    }

//    @Benchmark
//    public void rxJustAsync(Blackhole bh) throws Exception {
//        LatchedObserver o = new LatchedObserver(bh);
//        rxJustAsync.subscribe(o);
//        o.cdl.await();
//    }
//
//    @Benchmark
//    public void rxRangeAsync(Blackhole bh) throws Exception {
//        LatchedObserver o = new LatchedObserver(bh);
//        rxRangeAsync.subscribe(o);
//        o.cdl.await();
//    }

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
}