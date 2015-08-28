package com.cloudbees.simplediskusage;

import hudson.Extension;
import hudson.model.ManagementLink;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerProxy;

@Extension
public class QuickDiskUsageManagementLink extends ManagementLink implements StaplerProxy {
    @Override
    public String getIconFileName() {
        return "/plugin/cloudbees-disk-usage-simple/images/disk.png";
    }

    @Override
    public String getDescription() {
        return "Simple disk usage estimation";
    }

    @Override
    public String getDisplayName() {
        return "Disk usage";
    }

    @Override
    public String getUrlName() {
        return "disk-usage-simple";
    }

    /**
     * Only sysadmin can access this page.
     */
    @Override
    public Object getTarget() {
        // TODO switch to Jenkins.getActiveInstance() once 1.590+ is the baseline
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        jenkins.checkPermission(Jenkins.ADMINISTER);
        return jenkins.getPlugin(QuickDiskUsagePlugin.class);
    }

}
