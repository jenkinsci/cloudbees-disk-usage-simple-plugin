package com.cloudbees.simplediskusage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class UsageComputationTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void compute() throws Exception {
        final AtomicBoolean notified = new AtomicBoolean(false);
        final AtomicLong testUsage = new AtomicLong(0);
        final AtomicLong testCount = new AtomicLong(0);

        final UsageComputation uc = new UsageComputation(List.of(Paths.get(".")));
        uc.addListener(Paths.get("."), (dir, usage, count) -> {
            notified.set(true);
            testUsage.set(usage);
            testCount.set(count);
        });
        uc.compute();

        assertTrue(notified.get());
        assertTrue(testUsage.get() >  0);
        assertTrue(testCount.get() > 0);
    }
}