package com.cloudbees.simplediskusage;

import hudson.model.Job;

/**
 * A job directory path on the disk with its usage information
 */
public class JobDiskItem extends DiskItem {
    final String fullName;
    final String url;

    public JobDiskItem(Job job, Long size) {
        super(job.getFullDisplayName(), job.getRootDir(), size);
        this.fullName = job.getFullName();
        this.url = job.getUrl();
    }

    public String getFullName() {
        return fullName;
    }

    public String getUrl() {
        return url;
    }

}
