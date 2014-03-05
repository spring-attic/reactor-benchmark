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
import reactor.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks around various kinds of Map interactions.
 *
 * @author Jon Brisbin
 */
@Measurement(iterations = 5)
@Warmup(iterations = 5)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MapBenchmarks {

	@Param({"1000", "10000", "100000", "1000000"})
	public int length;

	int[]                hashCodes;
	int[]                randomKeys;
	Map<Integer, Object> intMap;
	Map<Integer, Object> concurrentIntMap;
	Map<Integer, Object> linkedNodeIntMap;
	Random               random;

	@SuppressWarnings("unchecked")
	@Setup
	public void setup() {
		random = new Random(System.nanoTime());

		hashCodes = new int[length];
		randomKeys = new int[length];
		intMap = new HashMap<>();
		concurrentIntMap = new ConcurrentHashMap<>();
		linkedNodeIntMap = new LinkedNodeMap<>(length / 4);

		for(int i = 0; i < length; i++) {
			final int hashCode = i;
			Object obj = new Object() {
				@Override
				public int hashCode() {
					return hashCode;
				}
			};

			hashCodes[i] = i;
			randomKeys[i] = random.nextInt(length);

			intMap.put(i, obj);
			linkedNodeIntMap.put(i, obj);
			concurrentIntMap.put(i, obj);
		}
	}

	@GenerateMicroBenchmark
	public void hashMapIntegerKey() {
		for(int i = 0; i < length; i++) {
			int key = randomKeys[i];
			Object obj = intMap.get(key);
			Assert.notNull(obj, "No object found for key " + key);
		}
	}

	@GenerateMicroBenchmark
	public void hashMapIteration() {
		for(Map.Entry<Integer, Object> entry : intMap.entrySet()) {
			Object obj = entry.getValue();
			Assert.notNull(obj, "No object found for key " + entry.getKey());
		}
	}

	@GenerateMicroBenchmark
	public void concurrentHashMapIteration() {
		for(Map.Entry<Integer, Object> entry : concurrentIntMap.entrySet()) {
			Object obj = entry.getValue();
			Assert.notNull(obj, "No object found for key " + entry.getKey());
		}
	}

	@GenerateMicroBenchmark
	public void concurrentHashMapIntegerKey() {
		for(int i = 0; i < length; i++) {
			int key = randomKeys[i];
			Object obj = concurrentIntMap.get(key);
			Assert.notNull(obj, "No object found for key " + key);
		}
	}

	@GenerateMicroBenchmark
	public void linkedNodeMapIntegerKey() {
		for(int i = 0; i < length; i++) {
			int key = randomKeys[i];
			Object obj = linkedNodeIntMap.get(key);
			Assert.notNull(obj, "No object found for key " + key);
		}
	}

}
