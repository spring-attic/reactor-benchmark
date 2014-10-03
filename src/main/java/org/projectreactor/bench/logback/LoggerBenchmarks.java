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

package org.projectreactor.bench.logback;

import ch.qos.logback.classic.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jon Brisbin
 */
@Measurement(iterations = 5, time = 5)
@Warmup(iterations = 5)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class LoggerBenchmarks {

	@Param({"sync", "async", "durable"})
	public String loggerName;

	private Logger     logger;
	private AtomicLong counter;

	@Setup
	public void setup() throws IOException {
		counter = new AtomicLong();
		logger = (Logger) LoggerFactory.getLogger(loggerName);
	}

	@TearDown
	public void cleanup() throws IOException {
		Files.deleteIfExists(Paths.get("target", "jmh.log"));
		Files.deleteIfExists(Paths.get("target", "durable.data"));
		Files.deleteIfExists(Paths.get("target", "durable.index"));
	}

	@Benchmark
	public void asyncAppenderToFile(Blackhole bh) throws InterruptedException {
		long l = counter.incrementAndGet();
		logger.info("count: {}", l);
		bh.consume(l);
	}

}
