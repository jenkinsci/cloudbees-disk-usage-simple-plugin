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

import java.io.File;
import java.util.Objects;

/**
 * A directory path on the disk with its usage information
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DiskItem implements Comparable<DiskItem> {

    private final String displayName;
    private final File path;
    private final Long usage;
    private final Long count;

    public DiskItem(String displayName, File path, Long usage, Long count) {
        this.displayName = displayName;
        this.path = path;
        this.usage = usage;
        this.count = count;
    }
    
    @Deprecated
    public DiskItem(String displayName, File path, Long usage) {
        this(displayName, path, usage, 0L);
    }

    public File getPath() {
        return path;
    }

    public Long getUsage() {
        return usage;
    }

    public Long getCount() {
        return count;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the usage value in MB
     * @return disk usage in MB
     */
    public String getUsageInMB() {
        float mbValue = usage / 1024.0f;
        return String.format("%.1f", mbValue);
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
