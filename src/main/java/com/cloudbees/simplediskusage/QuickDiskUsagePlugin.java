package com.cloudbees.simplediskusage;

import hudson.Extension;
import hudson.Plugin;
import hudson.Util;
import hudson.model.*;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
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

    static Executor ex = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Simple disk usage checker");
            return t;
        }
    });

    Map<DiskItem, Long> usage = new ConcurrentHashMap<>();
    long lastRunStart = 0;
    long lastRunEnd = 0;

    public void refreshData(){
        if (!isRunning()) {
            ex.execute(computeDiskUsage);
        }
    }

    public Map<DiskItem, Long> getDiskUsage() throws IOException {
        if (System.currentTimeMillis() - lastRunEnd >= QUIET_PERIOD) {
            refreshData();
        }
        Map<DiskItem, Long> sorted =  new TreeMap<>(BY_NAME);
        sorted.putAll(usage);
        return sorted;
    }

    private static Comparator<DiskItem> BY_NAME = new Comparator<DiskItem>() {
        @Override
        public int compare(DiskItem o1, DiskItem o2) {
            return o1.getFullDisplayName().compareToIgnoreCase(o2.getFullDisplayName());
        }
    };

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
        Jenkins jenkins = Jenkins.getInstance();
        if(jenkins != null){
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
        }
        res.forwardToPreviousPage(req);
    }


    private long duJob(Job item) throws IOException, InterruptedException {
        logger.fine("Estimating usage for: " + item.getDisplayName());
        return duDir(item.getRootDir());
    }

    private long duDir(File dir) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(DISK_USAGE, null, dir);
        try (BufferedReader stdOut = new BufferedReader (new InputStreamReader(p.getInputStream(), Charset.defaultCharset().name())) ) {
            String line = stdOut.readLine();
            if (line != null && line.matches("[0-9]*\t.")) return Long.parseLong(line.substring(0, line.length() -2));
            logger.log(Level.WARNING, "failed to parse `du` output : "+line);
            return -1;
        }
    }



    private static final Logger logger = Logger.getLogger(QuickDiskUsagePlugin.class.getName());

    private final Runnable computeDiskUsage = new Runnable() {
        public void run() {
            logger.info("Re-estimating disk usage");
            lastRunStart = System.currentTimeMillis();
            SecurityContext impersonate = ACL.impersonate(ACL.SYSTEM);
            Jenkins jenkins = Jenkins.getInstance();
            if(jenkins != null){
                try {
                    // Remove useless entries
                    for (DiskItem item:usage.keySet()){
                        if(!item.getPath().exists()){
                            usage.remove(item);
                        }
                    }
                    for (Job item : jenkins.getAllItems(Job.class)) {
                        if (item instanceof TopLevelItem)
                            usage.put(new DiskItem(item), duJob(item));

                        Thread.sleep(1000); //To keep load average nice and low
                    }
                    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
                    usage.put(new DiskItem(
                                    "java.io.tmpdir",
                                    "System temporary files (" + tmpDir.getAbsolutePath() + ")",
                                    null,
                                    tmpDir),
                            duDir(tmpDir));
                    // Display JENKINS_HOME size
                    File jenkinsHomeDir = jenkins.getRootDir();
                    usage.put(new DiskItem(
                                    "JENKINS_HOME",
                                    "JENKINS_HOME",
                                    null,
                                    jenkinsHomeDir),
                            duDir(jenkinsHomeDir));
                    // Display JENKINS_HOME first level sub-directories sizes when non-empty
                    File[] jenkinsHomeRootDirectories = jenkinsHomeDir.listFiles();
                    if (jenkinsHomeRootDirectories != null) {
                        for (File child : jenkinsHomeRootDirectories) {
                            if (child.isDirectory()) {
                                long size = duDir(child);
                                if (size > 0) {
                                    usage.put(new DiskItem(
                                                    "JENKINS_HOME » " + child.getName(),
                                                    "JENKINS_HOME » " + child.getName(),
                                                    null,
                                                    child),
                                            size);
                                }
                            }
                        }
                    }
                    logger.info("Finished re-estimating disk usage.");
                    lastRunEnd = System.currentTimeMillis();
                } catch (IOException | InterruptedException e) {
                    logger.log(Level.INFO, "Unable to run disk usage check", e);
                    lastRunEnd = lastRunStart;
                } finally {
                    SecurityContextHolder.setContext(impersonate);
                }
            }
        }
    };

}
