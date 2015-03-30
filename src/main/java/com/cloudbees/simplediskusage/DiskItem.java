package com.cloudbees.simplediskusage;

import hudson.model.Job;

/**
 * Some Jenkins item which do consume disk
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DiskItem {

    private final String fullName;

    private final String displayName;

    private final String url;

    public DiskItem(Job job) {
        this.fullName = job.getFullName();
        this.displayName = job.getFullDisplayName();
        this.url = job.getUrl();
    }

    public String getFullName() {
        return fullName;
    }

    public String getFullDisplayName() {
        return displayName;
    }

    public String getUrl() {
        return url;
    }
}
