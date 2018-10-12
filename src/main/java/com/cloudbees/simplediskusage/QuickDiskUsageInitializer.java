/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.simplediskusage;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;

public class QuickDiskUsageInitializer {
    /**
     * Let's trigger an update after Jenkins is fully up.
     *
     * JOB_LOADED is the last milestone that can be triggered with an @Initializer,
     * COMPLETED doesn't work. So, we queue the update and check that Jenkins is fully up in the runnable
     * used in refreshDataOnStartup()
     */
    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void initialize() {
        // TODO switch to Jenkins.getActiveInstance() once 1.590+ is the baseline
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        QuickDiskUsagePlugin plugin = jenkins.getPlugin(QuickDiskUsagePlugin.class);
        if (plugin == null) return;
        plugin.refreshDataOnStartup();
    }

}
