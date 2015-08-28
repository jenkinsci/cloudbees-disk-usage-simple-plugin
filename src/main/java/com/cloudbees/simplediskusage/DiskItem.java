package com.cloudbees.simplediskusage;

import hudson.model.Job;

import java.io.File;

/**
 * Some Jenkins item which do consume disk
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DiskItem {

    private final String fullName;

    private final String displayName;

    private final String url;

    private final File path;

    public DiskItem(Job job) {
        this.fullName = job.getFullName();
        this.displayName = "JENKINS_HOME » jobs » "+job.getFullDisplayName();
        this.url = job.getUrl();
        this.path = job.getRootDir();
    }

    public DiskItem(String fullName, String displayName, String url, File path) {
        this.fullName = fullName;
        this.displayName = displayName;
        this.url = url;
        this.path = path;
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

    public File getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiskItem diskItem = (DiskItem) o;

        return path.equals(diskItem.path);

    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
