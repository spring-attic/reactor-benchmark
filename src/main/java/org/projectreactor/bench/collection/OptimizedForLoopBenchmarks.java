/*
 * Copyright (c) 2011-2014 GoPivotal, Inc. All Rights Reserved.
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

package org.projectreactor.bench.collection;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Measurement(iterations = 5)
@Warmup(iterations = 5)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class OptimizedForLoopBenchmarks {

	@Param({"1000", "10000", "100000", "1000000"})
	public int length;

	private List<Integer> list;
	private int[]         array;

	@Setup
	public void setup() {
		list = new LinkedList<>();
		array = new int[length];
		for(int c = 0; c < length; c++) {
			int i = 10000 + c;
			list.add(i);
			array[c] = i;
		}
	}

	@Benchmark
	public void forLoopWithoutSideEffect() {
		for(Integer i : list) {}
	}

	@Benchmark
	public void forLoopWithSideEffect(Blackhole bh) {
		for(Integer i : list) { bh.consume(i); }
	}

	@Benchmark
	public void arrayForLoopWithAssignment() {
		for(int i = 0; i < length; i++) {
			Integer j = array[i];
		}
	}

	@Benchmark
	public void arrayForLoopWithSideEffect(Blackhole bh) {
		for(int i = 0; i < length; i++) { bh.consume(array[i]); }
	}

}
