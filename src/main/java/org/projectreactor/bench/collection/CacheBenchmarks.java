/*
 * Copyright (c) 2011-2014 Pivotal Software, Inc. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.impl.multimap.list.FastListMultimap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import reactor.core.util.Assert;

/**
 * @author Jon Brisbin
 */
@Measurement(iterations = 5, time = 3)
@Warmup(iterations = 5)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class CacheBenchmarks {

	@Param({"100", "1000", "10000", "100000", "1000000"})
	public int length;

	int[]                                randomKeys;
	Object[]                             objs;
	Map<Integer, List<Object>>           intMap;
	MutableListMultimap<Integer, Object> gsMap;
	Multimap<Integer, Object>            guavaMap;
	Random                               random;
	int                                  index;

	@SuppressWarnings("unchecked")
	@Setup
	public void setup() throws ClassNotFoundException,
	                           IllegalAccessException,
	                           InstantiationException {
		random = new Random(System.nanoTime());
		index = 0;
		randomKeys = new int[length];
		objs = new Object[length];

		intMap = new ConcurrentHashMap<>();
		gsMap = FastListMultimap.newMultimap();
		guavaMap = HashMultimap.create();

		for (int i = 0; i < length; i++) {
			final int hashCode = i;
			Object obj = new Object() {
				@Override
				public int hashCode() {
					return hashCode;
				}
			};
			objs[i] = obj;

			randomKeys[i] = random.nextInt(length);
			List<Object> objs = new ArrayList<>();
			for (int j = 0; j < 100; j++) {
				objs.add(obj);
			}
			intMap.put(i, objs);
			gsMap.put(i, objs);
			guavaMap.putAll(i, objs);
		}
	}

	@Benchmark
	public void concurrentHashMap(Blackhole bh) {
		int key = randomKeys[index++ % length];
		Object obj = intMap.get(key);
		Assert.notNull(obj, "No object found for key " + key);
		bh.consume(obj);
	}

	@Benchmark
	public void gsMultimap(Blackhole bh) {
		int key = randomKeys[index++ % length];
		Object obj = gsMap.get(key);
		Assert.notNull(obj, "No object found for key " + key);
		bh.consume(obj);
	}

	@Benchmark
	public void guavaMultimap(Blackhole bh) {
		int key = randomKeys[index++ % length];
		Object obj = guavaMap.get(key);
		Assert.notNull(obj, "No object found for key " + key);
		bh.consume(obj);
	}

}
