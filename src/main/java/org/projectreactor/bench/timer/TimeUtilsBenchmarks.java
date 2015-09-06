package org.projectreactor.bench.timer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import reactor.fn.timer.TimeUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author Anatoly Kadyshev
 */
@Measurement(iterations = 5, time = 1)
@Warmup(iterations = 5, time = 1)
@Fork(value = 3)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class TimeUtilsBenchmarks {

    @Setup
    public void setup() {
        TimeUtils.approxCurrentTimeMillis();
    }

    @Benchmark
    public void testApproxCurrentTimeMillis() {
        TimeUtils.approxCurrentTimeMillis();
    }

    @Benchmark
    public void testCurrentTimeMillis() {
        System.currentTimeMillis();
    }

    @Benchmark
    public void testNanoTime() {
        System.nanoTime();
    }

}
