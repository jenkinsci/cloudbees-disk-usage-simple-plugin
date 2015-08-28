package com.cloudbees.simplediskusage;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;

public class QuickDiskUsageInitializer {
    /**
     * Let's update data after Jenkins is started (to not slow down the loading)
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
        plugin.refreshData();
    }

}
