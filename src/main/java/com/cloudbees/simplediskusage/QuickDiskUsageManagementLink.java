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
     * Name of the category for this management link. Exists so that plugins with core dependency pre-dating the version
     * when this was introduced can define a category.
     *
     * TODO when the core version is &gt;2.226 change this to override {@code getCategory()} instead
     *
     * @return name of the desired category, one of the enum values of Category, e.g. {@code STATUS}.
     * @since 2.226 of Jenkins core
     */
    public String getCategoryName() {
        return "STATUS";
    }

    /**
     * Only sysadmin can access this page.
     */
    @Override
    public Object getTarget() {
        Jenkins jenkins = Jenkins.getInstance();
        jenkins.checkPermission(Jenkins.ADMINISTER);
        return jenkins.getPlugin(QuickDiskUsagePlugin.class);
    }
}
