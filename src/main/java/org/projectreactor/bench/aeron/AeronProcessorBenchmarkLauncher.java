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

/**
 * @author Anatoly Kadyshev
 */
public class AeronProcessorBenchmarkLauncher {

	private static final int N = 1_000_000;

	public static final int SIGNAL_LENGTH_BYTES = 1024;

	private static final String UNICAST_CHANNEL = "udp://127.0.0.1:12001";

	private static final String MULTICAST_CHANNEL = "udp://224.0.0.251:12001";

	private void run() throws Exception {
		AeronBenchmark bench = new AeronProcessorBenchmark(N, SIGNAL_LENGTH_BYTES, MULTICAST_CHANNEL, false);
		bench.runAndPrintResults();
	}

	public static void main(String[] args) throws Exception {
		new AeronProcessorBenchmarkLauncher().run();
	}

}
