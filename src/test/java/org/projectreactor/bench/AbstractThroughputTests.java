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
		start = System.currentTimeMillis();
		log.info("Starting {} throughput test...", type);
	}

	protected void stop(String type) {
		end = System.currentTimeMillis();
		elapsed = end - start;
		throughput = (long)(counter.get() / (elapsed / 1000));

		log.info("{} throughput: {}/s in {}ms", type, throughput, (int)elapsed);
	}

	protected boolean withinTimeout() {
		return (System.currentTimeMillis() - start < getTimeout());
	}

}
