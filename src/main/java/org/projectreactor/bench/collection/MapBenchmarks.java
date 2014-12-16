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
import reactor.core.support.Assert;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks around various kinds of Map interactions.
 *
 * @author Jon Brisbin
 */
@Measurement(iterations = 5, time = 3)
@Warmup(iterations = 5)
@Fork(1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MapBenchmarks {

	@Param({"1000"})
	public int    length;
	@Param({
			       "java.util.HashMap",
			       "java.util.concurrent.ConcurrentHashMap",
			       "com.gs.collections.impl.map.mutable.ConcurrentHashMapUnsafe"
	       })
	public String mapImpl;

	int[]                randomKeys;
	Map<Integer, Object> intMap;
	Random               random;
	int                  index;

	@SuppressWarnings("unchecked")
	@Setup
	public void setup() throws ClassNotFoundException,
	                           IllegalAccessException,
	                           InstantiationException {
		random = new Random(System.nanoTime());
		index = 0;
		randomKeys = new int[length];

		intMap = (Map<Integer, Object>) Class.forName(mapImpl).newInstance();

		for (int i = 0; i < length; i++) {
			final int hashCode = i;
			Object obj = new Object() {
				@Override
				public int hashCode() {
					return hashCode;
				}
			};

			randomKeys[i] = random.nextInt(length);
			intMap.put(i, obj);
		}
	}

	@Benchmark
	public void getRandomIntKey(Blackhole bh) {
		int key = randomKeys[index++ % length];
		Object obj = intMap.get(key);
		Assert.notNull(obj, "No object found for key " + key);
		bh.consume(obj);
	}

	@Benchmark
	public void entrySetIteration(Blackhole bh) {
		for (Map.Entry<Integer, Object> entry : intMap.entrySet()) {
			Object obj = entry.getValue();
			Assert.notNull(obj, "No object found for key " + entry.getKey());
			bh.consume(obj);
		}
	}

}
