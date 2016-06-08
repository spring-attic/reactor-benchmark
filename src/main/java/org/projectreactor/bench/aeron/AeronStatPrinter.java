/*
 * Copyright (c) 2011-2016 Pivotal Software, Inc. All Rights Reserved.
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
package org.projectreactor.bench.aeron;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import reactor.aeron.utils.AeronCounters;
import reactor.core.flow.Cancellation;
import reactor.core.scheduler.Schedulers;

public class AeronStatPrinter {

	private final String name;

	private final PrintStream ps;

	private final AeronCounters counters;

	private Cancellation pausable;

	public AeronStatPrinter(String name, String dirName) throws FileNotFoundException {
		this.name = name;
		final File cncFile = new File(dirName + "/cnc");
		final String statsFileName = dirName + "/" + this.name + ".stat.txt";

		ps = new PrintStream(new FileOutputStream(statsFileName));
		counters = new AeronCounters(dirName);

		System.out.println("Command `n Control file " + cncFile);
		System.out.println("Writing stats to " + statsFileName);
	}

	public void initialise() {
		if (pausable != null) {
			throw new IllegalStateException("Already initialised");
		}
		pausable = Schedulers.newTimer("aeron-stat-printer").schedule(() -> {
			ps.format("%1$tH:%1$tM:%1$tS - %2$s - Aeron Stat\n", new Date(), this.name);

			counters.forEach((id, label) ->
					ps.format("%3d: %,20d - %s\n", id, counters.getCounterValue(id), label));

			ps.flush();
		}, 5_000L, TimeUnit.MILLISECONDS);
	}

	public void shutdown() {
		pausable.dispose();
		counters.shutdown();
		ps.close();
	}

}
