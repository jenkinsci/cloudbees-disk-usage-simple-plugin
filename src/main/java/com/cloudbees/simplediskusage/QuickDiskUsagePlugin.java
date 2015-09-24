/**
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
import hudson.Plugin;
import hudson.Util;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;


@Extension
@Singleton
public class QuickDiskUsagePlugin extends Plugin {

    public static final String DISK_USAGE =
            System.getProperty("os.name").toLowerCase().contains("mac")
                    ? "du -ks" // OSX doesn't have ionice, this is only used during dev on my laptop
                    : "ionice -c 3 du -ks";

    public static final int QUIET_PERIOD = 15 * 60 * 1000;

    private static Executor ex = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Simple disk usage checker");
            return t;
        }
    });

    private static final Logger logger = Logger.getLogger(QuickDiskUsagePlugin.class.getName());

    private CopyOnWriteArrayList<DiskItem> directoriesUsages = new CopyOnWriteArrayList<>();

    private CopyOnWriteArrayList<JobDiskItem> jobsUsages = new CopyOnWriteArrayList<>();

    private long lastRunStart = 0;

    private long lastRunEnd = 0;

    @Override
    public void start() throws Exception {
        try {
            load();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to load "+getConfigXml(),e);
        }
        if(isRunning()){
            // It's impossible, the plugin was just loaded. Let's reset end date
            lastRunEnd = lastRunStart;
        }
    }

    public void refreshData() {
        if (!isRunning()) {
            ex.execute(computeDiskUsage);
        }
    }

    public CopyOnWriteArrayList<DiskItem> getDirectoriesUsages() throws IOException {
        if (System.currentTimeMillis() - lastRunEnd >= QUIET_PERIOD) {
            refreshData();
        }
        return directoriesUsages;
    }

    public CopyOnWriteArrayList<JobDiskItem> getJobsUsages() throws IOException {
        if (System.currentTimeMillis() - lastRunEnd >= QUIET_PERIOD) {
            refreshData();
        }
        return jobsUsages;
    }

    public long getLastRunStart() {
        return lastRunStart;
    }

    public long getLastRunEnd() {
        return lastRunEnd;
    }

    public String getSince() {
        return Util.getPastTimeString(System.currentTimeMillis() - lastRunEnd);
    }

    public String getDuration() {
        return Util.getTimeSpanString(lastRunEnd - lastRunStart);
    }

    public boolean isRunning() {
        return lastRunEnd < lastRunStart;
    }

    @RequirePOST
    public void doRefresh(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
        refreshData();
        res.forwardToPreviousPage(req);
    }

    @RequirePOST
    public void doClean(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
        // TODO switch to Jenkins.getActiveInstance() once 1.590+ is the baseline
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        final Job job = jenkins.getItemByFullName(req.getParameter("job"), Job.class);
        Timer.get().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    job.logRotate();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "logRotate failed", e);
                }
            }
        });
        res.forwardToPreviousPage(req);
    }

    private long duDir(File path) throws IOException, InterruptedException {
        if (path == null || !path.exists() || !path.isDirectory()) return -1;
        logger.fine("Estimating usage for: " + path.getAbsolutePath());
        Process p = Runtime.getRuntime().exec(DISK_USAGE, null, path);
        try (BufferedReader stdOut = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset().name()))) {
            String line = stdOut.readLine();
            if (line != null && line.matches("[0-9]*\t.")) return Long.parseLong(line.substring(0, line.length() - 2));
            logger.warning("failed to parse `du` output : " + line);
            return -1;
        }
    }

    private JobDiskItem computeJobUsage(Job job) throws IOException, InterruptedException {
        long size = duDir(job.getRootDir());
        if (size > 0) {
            return new JobDiskItem(job, size);
        } else {
            return null;
        }
    }

    private void computeJobsUsages() throws IOException, InterruptedException {
        // TODO switch to Jenkins.getActiveInstance() once 1.590+ is the baseline
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        // Remove useless entries for jobs
        for (JobDiskItem item : jobsUsages) {
            if (!item.getPath().exists() || jenkins.getItemByFullName(item.getFullName(), Job.class) == null) {
                jobsUsages.remove(item);
            }
        }
        // Add or update entries for jobs
        for (Job item : jenkins.getAllItems(Job.class)) {
            if (item instanceof TopLevelItem) {
                JobDiskItem usage = computeJobUsage(item);
                if (usage != null) {
                    if (jobsUsages.contains(usage)) {
                        jobsUsages.remove(usage);
                    }
                    jobsUsages.add(usage);
                }

            }
            Thread.sleep(1000); //To keep load average nice and low
        }
    }

    private DiskItem computeDirectoryUsage(String displayName, File path) throws IOException, InterruptedException {
        long size = duDir(path);
        if (size > 0) {
            return new DiskItem(displayName, path, size);
        } else {
            return null;
        }
    }


    private void computeDirectoriesUsages() throws IOException, InterruptedException {
        // TODO switch to Jenkins.getActiveInstance() once 1.590+ is the baseline
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        Map<File, String> directoriesToProcess = new HashMap<>();
        // Display JENKINS_HOME size
        directoriesToProcess.put(jenkins.getRootDir(), "JENKINS_HOME");
        // Display JENKINS_HOME first level sub-directories
        File[] jenkinsHomeRootDirectories = jenkins.getRootDir().listFiles();
        if (jenkinsHomeRootDirectories != null) {
            for (File child : jenkinsHomeRootDirectories) {
                if (child.isDirectory()) {
                    directoriesToProcess.put(child, "JENKINS_HOME/" + child.getName());
                }
            }
        }
        // Display java.io.tmpdir size
        directoriesToProcess.put(new File(System.getProperty("java.io.tmpdir")), "java.io.tmpdir");

        // Remove useless entries for directories
        for (DiskItem item : directoriesUsages) {
            if (!item.getPath().exists() || !directoriesToProcess.containsKey(item.getPath())) {
                directoriesUsages.remove(item);
            }
        }

        // Add or update entries for directories
        for (Map.Entry<File,String> item : directoriesToProcess.entrySet()) {
            DiskItem usage = computeDirectoryUsage(item.getValue(), item.getKey());
            if (usage != null) {
                if (directoriesUsages.contains(usage)) {
                    directoriesUsages.remove(usage);
                }
                directoriesUsages.add(usage);
            }
            Thread.sleep(1000); //To keep load average nice and low
        }
    }

    private transient final Runnable computeDiskUsage = new Runnable() {
        public void run() {
            logger.info("Re-estimating disk usage");
            lastRunStart = System.currentTimeMillis();
            SecurityContext impersonate = ACL.impersonate(ACL.SYSTEM);
            // TODO switch to Jenkins.getActiveInstance() once 1.590+ is the baseline
            Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                throw new IllegalStateException("Jenkins has not been started, or was already shut down");
            }
            try {
                computeJobsUsages();
                computeDirectoriesUsages();
                logger.info("Finished re-estimating disk usage.");
                lastRunEnd = System.currentTimeMillis();
            } catch (IOException | InterruptedException e) {
                logger.log(Level.INFO, "Unable to run disk usage check", e);
                lastRunEnd = lastRunStart;
            } finally {
                SecurityContextHolder.setContext(impersonate);
            }
            try{
                // Save data
                save();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to save "+getConfigXml(),e);
            }
        }
    };

}
