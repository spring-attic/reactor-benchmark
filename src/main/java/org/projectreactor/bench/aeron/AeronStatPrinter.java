package org.projectreactor.bench.aeron;

import reactor.Timers;
import reactor.fn.Consumer;
import uk.co.real_logic.aeron.CncFileDescriptor;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.IoUtil;
import uk.co.real_logic.agrona.concurrent.AtomicBuffer;
import uk.co.real_logic.agrona.concurrent.CountersManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.MappedByteBuffer;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Based on <a href="https://github.com/real-logic/Aeron/blob/master/aeron-samples/src/main/java/uk/co/real_logic/aeron/samples/AeronStat.java">AeronStat.java from Aeron</a>
 */
public class AeronStatPrinter {

    private final String name;
    private FileOutputStream fos;
    private PrintStream ps;

    public AeronStatPrinter(String name) {
        this.name = name;
    }

    public void setup(String dirName) throws FileNotFoundException {
        final File cncFile = new File(dirName + "/cnc");
        System.out.println("Command `n Control file " + cncFile);

        fos = new FileOutputStream(dirName + "/" + name + ".stat.txt");
        ps = new PrintStream(fos);

        final MappedByteBuffer cncByteBuffer = IoUtil.mapExistingFile(cncFile, "cnc");
        final DirectBuffer metaDataBuffer = CncFileDescriptor.createMetaDataBuffer(cncByteBuffer);
        final int cncVersion = metaDataBuffer.getInt(CncFileDescriptor.cncVersionOffset(0));

        if (CncFileDescriptor.CNC_VERSION != cncVersion)
        {
            throw new IllegalStateException("CNC version not supported: version=" + cncVersion);
        }

        final AtomicBuffer labelsBuffer = CncFileDescriptor.createCounterLabelsBuffer(cncByteBuffer, metaDataBuffer);
        final AtomicBuffer valuesBuffer = CncFileDescriptor.createCounterValuesBuffer(cncByteBuffer, metaDataBuffer);
        final CountersManager countersManager = new CountersManager(labelsBuffer, valuesBuffer);

        Timers.global().schedule(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) {
                try {
                    print(valuesBuffer, countersManager);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void print(AtomicBuffer valuesBuffer, CountersManager countersManager) throws InterruptedException {
        ps.print("\033[H\033[2J");
        ps.format("%1$tH:%1$tM:%1$tS - %2$s - Aeron Stat\n", new Date(), name);
        ps.println("=========================");

        countersManager.forEach(
                (id, label) ->
                {
                    final int offset = CountersManager.counterOffset(id);
                    final long value = valuesBuffer.getLongVolatile(offset);

                    ps.format("%3d: %,20d - %s\n", id, value, label);
                });

        ps.flush();
    }

}
