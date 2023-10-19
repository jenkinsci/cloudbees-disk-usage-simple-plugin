package com.cloudbees.simplediskusage;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class UsageComputationTest {
    @Test
    public void compute() throws Exception {
        final AtomicBoolean notified = new AtomicBoolean(false);
        final AtomicLong testUsage = new AtomicLong(0);
        final AtomicLong testCount = new AtomicLong(0);

        final UsageComputation uc = new UsageComputation(Arrays.asList(Paths.get(".")));
        uc.addListener(Paths.get("."), new UsageComputation.CompletionListener() {
            @Override
            public void onCompleted(Path dir, long usage, long count) {
                notified.set(true);
                testUsage.set(usage);
                testCount.set(count);
            }
        });
        uc.compute();

        Assert.assertTrue(notified.get());
        Assert.assertTrue(testUsage.get() >  0);
        Assert.assertTrue(testCount.get() > 0);
    }
}