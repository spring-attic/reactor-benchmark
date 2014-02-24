package org.projectreactor.bench;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jon Brisbin
 */
public abstract class AbstractThroughputTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private long   start;
	private long   end;
	private double elapsed;
	private long   throughput;

	protected AtomicLong counter;

	@Before
	public void setup() {
		counter = new AtomicLong(0);
		doSetup();
	}

	protected abstract void doSetup();

	protected abstract long getTimeout();

	protected void start(String type) {
	}

	protected void testThroughput(String type, Runnable runnable) {
		start = System.currentTimeMillis();
		//log.info("Starting {} throughput test...", type);

		long timeout = getTimeout();
		while(System.currentTimeMillis() - start < timeout) {
			runnable.run();
		}

		end = System.currentTimeMillis();
		elapsed = end - start;
		throughput = (long)(counter.get() / elapsed);

		log.info("{} throughput: {}/ms in {}ms", type, throughput, (int)elapsed);
	}

}
