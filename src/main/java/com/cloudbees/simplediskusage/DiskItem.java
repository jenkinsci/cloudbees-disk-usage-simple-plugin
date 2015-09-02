package com.cloudbees.simplediskusage;

import java.io.File;
import java.util.Objects;

/**
 * A directory path on the disk with its usage information
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DiskItem implements Comparable<DiskItem> {

    final String displayName;

    final File path;

    final Long usage;

    public DiskItem(String displayName, File path, Long usage) {
        this.displayName = displayName;
        this.path = path;
        this.usage = usage;
    }

    public File getPath() {
        return path;
    }

    public Long getUsage() {
        return usage;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int compareTo(DiskItem o) {
        return Objects.compare(
                getDisplayName() != null ? getDisplayName() : "",
                o != null && o.getDisplayName() != null ? o.getDisplayName() : "",
                String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiskItem diskItem = (DiskItem) o;
        return Objects.equals(getPath(), diskItem.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath());
    }
}
