/*
 * Copyright (c) 2011-2015 GoPivotal, Inc. All Rights Reserved.
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

import reactor.aeron.processor.AeronPublisher;
import reactor.aeron.processor.AeronSubscriber;
import reactor.io.buffer.Buffer;

/**
 * @author Anatoly Kadyshev
 */
public class AeronSubscriberPublisherBenchmark extends AeronBenchmark {

	private AeronSubscriber subscriber;

	private AeronPublisher publisher;

	public AeronSubscriberPublisherBenchmark(int n, int signalLengthBytes, boolean useSingleDriverInstance) {
		super(n, signalLengthBytes, useSingleDriverInstance);
	}

	@Override
	protected void doSubscribeClient(SubscriberForBenchmark clientSubscriber) {
		publisher.subscribe(clientSubscriber);
	}

	@Override
	protected void doSendInitialSignal() {
		subscriber.onNext(Buffer.wrap("Hello, world!!!"));
	}

	@Override
	protected void doLaunchClient(AeronTestInfra clientInfra) {
		publisher = AeronPublisher.create(clientInfra.newContext());
	}

	@Override
	protected void doLaunchServer(AeronTestInfra serverInfra) {
		subscriber = AeronSubscriber.create(serverInfra.newContext());
	}

	@Override
	protected void doTearDownServer() {
	}

	@Override
	protected void doTearDownClient() {
		subscriber.onComplete();
	}

	@Override
	protected void doOnNext(Buffer buffer) {
		subscriber.onNext(buffer);
	}

}
