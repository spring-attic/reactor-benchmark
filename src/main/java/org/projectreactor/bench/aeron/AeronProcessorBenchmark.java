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

import reactor.aeron.processor.AeronProcessor;
import reactor.aeron.processor.Context;
import reactor.io.buffer.Buffer;

/**
 * @author Anatoly Kadyshev
 */
public class AeronProcessorBenchmark extends AeronBenchmark {

	private AeronProcessor serverProcessor;

	private AeronProcessor clientProcessor;

	private final String channel;

	public AeronProcessorBenchmark(int n, int signalLengthBytes, String channel, boolean useSingleDriverInstance) {
		super(n, signalLengthBytes, useSingleDriverInstance);
		this.channel = channel;
	}

	protected void doSubscribeClient(SubscriberForBenchmark clientSubscriber) {
		clientProcessor.subscribe(clientSubscriber);
	}

	protected void doSendInitialSignal() {
		serverProcessor.onNext(Buffer.wrap("Event"));
	}

	protected void doLaunchClient(AeronTestInfra clientInfra) {
		Context context = clientInfra.newContext();
		context.channel(channel);
		clientProcessor = AeronProcessor.create(context);
	}

	protected void doLaunchServer(AeronTestInfra serverInfra) {
		Context context = serverInfra.newContext();
		context.channel(channel);
		serverProcessor = AeronProcessor.create(context);
	}

	protected void doTearDownServer() {
		serverProcessor.awaitAndShutdown();
	}

	protected void doTearDownClient() {
		clientProcessor.awaitAndShutdown();
	}

	protected void doOnNext(Buffer buffer) {
		serverProcessor.onNext(buffer);
	}

}
