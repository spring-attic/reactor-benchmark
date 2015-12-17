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

import reactor.aeron.Context;
import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.aeron.driver.ThreadingMode;

import java.nio.file.Files;

/**
 * @author Anatoly Kadyshev
 */
class AeronTestInfra {

	private static final long DELAY_MILLIS = 1000;

	private final String name;

	private MediaDriver.Context driverContext;

	private MediaDriver mediaDriver;

	private Aeron aeron;

	AeronTestInfra(String name) {
		this.name = name;
	}

	Aeron createAeron(MediaDriver mediaDriver) {
		Aeron.Context context = new Aeron.Context();
		context.aeronDirectoryName(mediaDriver.aeronDirectoryName());
		return Aeron.connect(context);
	}

	MediaDriver launchMediaDriver() throws Exception {
		driverContext.threadingMode(ThreadingMode.SHARED);
		String dirName = Files.createTempDirectory("aeron-").toString();
		driverContext.aeronDirectoryName(dirName);
		MediaDriver mediaDriver = MediaDriver.launch(driverContext);

		AeronStatPrinter statPrinter = new AeronStatPrinter(name);
		statPrinter.setup(dirName);

		System.out.println(name + " media driver launched");
		Thread.sleep(DELAY_MILLIS);

		return mediaDriver;
	}

	void initialize() throws Exception {
		driverContext = new MediaDriver.Context();
		mediaDriver = launchMediaDriver();
		aeron = createAeron(mediaDriver);

		System.out.println("Test infrastructure: " + name + " initialized");
	}

	void shutdown() throws Exception {
		aeron.close();
		Thread.sleep(DELAY_MILLIS);

		mediaDriver.close();
		Thread.sleep(DELAY_MILLIS);

		try {
			driverContext.deleteAeronDirectory();
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}
	}

	public Context newContext() {
		return new Context()
				.name(name)
				.publicationRetryMillis(500)
				.ringBufferSize(128 * 1024)
				.aeron(aeron);
	}

}
