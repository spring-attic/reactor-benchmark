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
package org.projectreactor.bench.rx;

import org.openjdk.jmh.annotations.*;
import org.projectreactor.bench.rx.support.InputWithIncrementingInteger;
import reactor.event.dispatch.SynchronousDispatcher;
import reactor.function.Function;
import reactor.rx.Streams;
import reactor.rx.action.Action;
import reactor.rx.action.MapAction;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MapBenchmarks {

	@State(Scope.Thread)
	public static class Input extends InputWithIncrementingInteger {

		@Param({"1", "1000", "1000000"})
		public int size;

		@Override
		public int getSize() {
			return size;
		}

		public Action<Integer, Integer> map;

		@Override
		protected void postSetup() {
			map = new MapAction<Integer,
					Integer>(IDENTITY_FUNCTION, SynchronousDispatcher.INSTANCE
			);
		}
	}

	@Benchmark
	public void mapPassThruViaConnect(Input input) throws InterruptedException {
		input.observable.connect(input.map).subscribe(input.observer);
		input.postSetup();
	}

	@Benchmark
	public void mapInstance(Input input){
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
