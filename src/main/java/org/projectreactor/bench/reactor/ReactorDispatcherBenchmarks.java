package org.projectreactor.bench.reactor;

import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.ProducerType;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import reactor.core.Environment;
import reactor.event.Event;
import reactor.event.dispatch.Dispatcher;
import reactor.event.dispatch.RingBufferDispatcher;
import reactor.event.dispatch.ThreadPoolExecutorDispatcher;
import reactor.event.dispatch.WorkQueueDispatcher;
import reactor.event.routing.ArgumentConvertingConsumerInvoker;
import reactor.event.routing.ConsumerFilteringEventRouter;
import reactor.event.routing.EventRouter;
import reactor.filter.PassThroughFilter;
import reactor.function.Consumer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jon Brisbin
 */
@State(Scope.Thread)
public class ReactorDispatcherBenchmarks {

	static int BACKLOG = 2048;

	EventRouter                  eventRouter;
	RingBufferDispatcher         ringBufferDispatcher;
	WorkQueueDispatcher          workQueueDispatcher;
	ThreadPoolExecutorDispatcher threadPoolExecutorDispatcher;
	Event<?>                     event;
	AtomicLong                   counter;
	Consumer<Event<?>>           consumer;

	@Setup
	public void setup() {
		event = Event.wrap("Hello World!");
		counter = new AtomicLong(0);

		eventRouter = new ConsumerFilteringEventRouter(
				new PassThroughFilter(),
				new ArgumentConvertingConsumerInvoker(null)
		);

		ringBufferDispatcher = new RingBufferDispatcher(
				"ringBufferDispatcher",
				BACKLOG,
				null,
				ProducerType.SINGLE,
				new YieldingWaitStrategy()
		);
		workQueueDispatcher = new WorkQueueDispatcher(
				"workQueueDispatcher",
				Environment.PROCESSORS,
				BACKLOG,
				null,
				ProducerType.SINGLE,
				new YieldingWaitStrategy()
		);
		threadPoolExecutorDispatcher = new ThreadPoolExecutorDispatcher(
				Environment.PROCESSORS,
				BACKLOG,
				"threadPoolExecutorDispatcher"
		);

		consumer = new Consumer<Event<?>>() {
			@Override
			public void accept(Event<?> event) {
				counter.incrementAndGet();
			}
		};
	}

	@GenerateMicroBenchmark
	public void ringBufferDispatcherBenchmarks() {
		doTest(ringBufferDispatcher);
	}

	@GenerateMicroBenchmark
	public void workQueueDispatcherBenchmarks() {
		doTest(workQueueDispatcher);
	}

	@GenerateMicroBenchmark
	public void threadPoolExecutorDispatcherBenchmarks() {
		doTest(threadPoolExecutorDispatcher);
	}

	private void doTest(Dispatcher dispatcher) {
		dispatcher.dispatch(
				event,
				eventRouter,
				consumer,
				null
		);
	}

}
