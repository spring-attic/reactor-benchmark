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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.projectreactor.bench.rx.support.InputWithIncrementingLong;
import reactor.core.publisher.PublisherMap;
import reactor.fn.Function;
import reactor.rx.Streams;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MapBenchmarks {

	@State(Scope.Thread)
	public static class Input extends InputWithIncrementingLong {

		@Param({"1", "1000", "1000000"})
		public int size;

		@Override
		public int getSize() {
			return size;
		}
	}

	@Benchmark
	public void mapPassThruViaConnect(Input input) throws InterruptedException {
		new PublisherMap<>(input.observable, IDENTITY_FUNCTION).subscribe(input.observer);
	}

	@Benchmark
	public void mapInstance(Input input) {
		Streams.just(1).map(IDENTITY_FUNCTION);
	}

	@Benchmark
	public void mapPassThru(Input input) throws InterruptedException {
		input.observable.map(IDENTITY_FUNCTION).subscribe(input.observer);
	}

	private static final Function<Integer, Integer> IDENTITY_FUNCTION = new Function<Integer, Integer>() {
		@Override
		public Integer apply(Integer value) {
			return value;
		}
	};
}
