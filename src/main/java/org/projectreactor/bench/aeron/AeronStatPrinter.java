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

import reactor.aeron.utils.AeronCounters;
import reactor.core.scheduler.Timer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;

public class AeronStatPrinter {

	private final String name;

	private final PrintStream ps;

	private final AeronCounters counters;

	private Runnable pausable;

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
		pausable = Timer.global().schedule(aLong -> {
			ps.format("%1$tH:%1$tM:%1$tS - %2$s - Aeron Stat\n", new Date(), this.name);

			counters.forEach((id, label) ->
					ps.format("%3d: %,20d - %s\n", id, counters.getCounterValue(id), label));

			ps.flush();
		}, 5_000);
	}

	public void shutdown() {
		pausable.run();
		counters.shutdown();
		ps.close();
	}

}
