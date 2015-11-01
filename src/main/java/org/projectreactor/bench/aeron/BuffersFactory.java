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

import reactor.io.buffer.Buffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * @author Anatoly Kadyshev
 */
class BuffersFactory {

	class PopulateSubarrayTask extends RecursiveTask<Void> {

		private final Buffer[] buffers;

		private final int from;

		private final int to;

		private final byte[] message;

		PopulateSubarrayTask(Buffer[] buffers, int from, int to, byte[] message) {
			this.buffers = buffers;
			this.from = from;
			this.to = to;
			this.message = message;
		}

		@Override
		protected Void compute() {
			List<PopulateSubarrayTask> recursiveTasks = new ArrayList<>();
			if (to - from + 1 >= 1000000) {
				int middle = (to + from) / 2;
				recursiveTasks.add(new PopulateSubarrayTask(buffers, from, middle, message));
				recursiveTasks.add(new PopulateSubarrayTask(buffers, middle + 1, to, message));

				for (PopulateSubarrayTask task : recursiveTasks) {
					task.fork();
				}

				for (PopulateSubarrayTask task : recursiveTasks) {
					task.join();
				}
			} else {
				for (int i = from; i <= to; i++) {
					buffers[i] = new Buffer(ByteBuffer.wrap(message));
				}
			}
			return null;
		}
	}

	Buffer[] populateBuffers(int n, int signalLengthBytes) {
		Buffer[] buffers = new Buffer[n];

		byte[] message = createMessage(signalLengthBytes);

		System.out.println("Signal length in bytes: " + message.length);

		new ForkJoinPool(Runtime.getRuntime().availableProcessors()).invoke(
				new PopulateSubarrayTask(buffers, 0, n - 1, message));

		System.out.println("Number of buffers created: " + n);
		return buffers;
	}

	private byte[] createMessage(int length) {
		StringBuilder msgBuilder = new StringBuilder();
		for (int i = 0; i < length; i++) {
			msgBuilder.append(i % 10);
		}
		return msgBuilder.toString().getBytes(StandardCharsets.UTF_8);
	}

}
