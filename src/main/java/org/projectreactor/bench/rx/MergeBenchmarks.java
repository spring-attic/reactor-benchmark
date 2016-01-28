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

package org.projectreactor.bench.rx;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.projectreactor.bench.rx.support.InputWithIncrementingLong;
import org.projectreactor.bench.rx.support.LatchedCallback;
import org.reactivestreams.Publisher;
import reactor.core.publisher.ProcessorGroup;
;
import reactor.rx.Stream;

/**
 * Adapted from https://github.com/ReactiveX/RxJava/blob/1.x/src/perf/java/rx/operators/OperatorMergePerf.java
 *
 * @author Stephane Maldini
 */
public class MergeBenchmarks {

	@Benchmark
	public void merge1StreamOfN(final Input input) throws InterruptedException {
		Publisher<Integer> stream =
				Stream.just(1)
						.flatMap(i -> Stream.range(0, input.size));

		LatchedCallback<Integer> latchedCallback = input.newLatchedCallback();
		stream.subscribe(latchedCallback);
	}


	@Benchmark
	public void merge1StreamOfNPooledinputDispatcher(final Input input) throws InterruptedException {
		Publisher<Integer> stream =
		  Stream.just(1)
			.flatMap(i -> Stream.range(0, input.size).dispatchOn(input.processor))
		;

		LatchedCallback<Integer> latchedCallback = input.newLatchedCallback();
		stream.subscribe(latchedCallback);

		latchedCallback.latch.await();
	}


	@State(Scope.Thread)
	public static class Input extends InputWithIncrementingLong {

		@Param({  "1", "1000", "1000000" })
		public int size;

		@Override
		public int getSize() {
			return size;
		}

		public ProcessorGroup<Long> processor;

		@Param({"sync", "ringBuffer"})
		public String dispatcherName;

		@Override
		protected void postSetup() {
			processor = "sync".equalsIgnoreCase(dispatcherName) ? ProcessorGroup.sync() : ProcessorGroup.async();
		}
	}
}
