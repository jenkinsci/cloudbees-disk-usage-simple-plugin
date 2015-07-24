package com.cloudbees.simplediskusage;

import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Expose endpoint for nagios to hit for monitoring the health of this instance.
 */
@Extension
@Singleton
public class QuickDiskUsage extends ManagementLink {


    public static final String DISK_USAGE =
            System.getProperty("os.name").toLowerCase().contains("mac")
                    ? "du -ks" // OSX doesn't have ionice, this is only used during dev on my laptop
                    : "ionice -c 3 du -ks";

    static Executor ex = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Simple disk usage checker");
            return t;
        }
    });

    Map<DiskItem, Long> usage = new HashMap<>();
    long lastRun = 0;

    public Map<DiskItem, Long> getDiskUsage() throws IOException {
        fetchUsage();
        Map<DiskItem, Long> sorted =  new TreeMap<>(BY_NAME);
        sorted.putAll(usage);
        return sorted;
    }

    private static Comparator<DiskItem> BY_NAME = new Comparator<DiskItem>() {
        @Override
        public int compare(DiskItem o1, DiskItem o2) {
            return o1.getFullName().compareTo(o2.getFullName());
        }
    };

    public long getLastRun() {
        return lastRun;
    }

    public String getSince() {
        return Util.getPastTimeString(System.currentTimeMillis() - lastRun);
    }

    @RequirePOST
    public void doRefresh(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
        lastRun = 0;
        usage.clear();
        fetchUsage();
        res.forwardToPreviousPage(req);
    }

    @RequirePOST
    public void doClean(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
        final Job job = Jenkins.getInstance().getItemByFullName(req.getParameter("job"), Job.class);
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

    @Override
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    private void fetchUsage() throws IOException {
        if (System.currentTimeMillis() - lastRun < 3 * 60 * 1000) {
            return;
        }
        logger.info("Re-estimating disk usage");
        ex.execute(computeDiskUsage);
    }

    private long duJob(Job item) throws IOException, InterruptedException {
        logger.info("Estimating usage for: " + item.getDisplayName());
        return duDir(item.getRootDir());
    }

    private long duDir(File dir) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(DISK_USAGE, null, dir);
        StringBuilder du = new StringBuilder();
        try (BufferedReader stdOut = new BufferedReader (new InputStreamReader(p.getInputStream())) ) {
            String line = stdOut.readLine();
            if (line.matches("[0-9]*\t.")) return Long.parseLong(line.substring(0, line.length() -2));
            logger.log(Level.WARNING, "failed to parse `du` output : "+line);
            return -1;
        }
    }

    public String getIconFileName() {
        return "/plugin/cloudbees-disk-usage-simple/images/disk.png";
    }

    public String getDescription() {
        return "Simple disk usage estimation";
    }

    public String getDisplayName() {
        return "Disk usage";
    }

    public String getUrlName() {
        return "disk-usage-simple";
    }


    private static final Logger logger = Logger.getLogger(QuickDiskUsage.class.getName());

    private final Runnable computeDiskUsage = new Runnable() {
        public void run() {
            Map<DiskItem, Long> usage = new HashMap<>();
            SecurityContext impersonate = ACL.impersonate(ACL.SYSTEM);
            try {
                for (Job item : Jenkins.getInstance().getAllItems(Job.class)) {
                    if (item instanceof TopLevelItem)
                        usage.put(new DiskItem(item), duJob(item));

                    Thread.sleep(1000); //To keep load average nice and low
                }
                File tmpDir = new File(System.getProperty("java.io.tmpdir"));
                usage.put(new DiskItem(
                                "java.io.tmpdir",
                                "java.io.tmpdir (" + tmpDir.getAbsolutePath() + ")",
                                null),
                        duDir(tmpDir));
                logger.info("Finished re-estimating disk usage.");
                QuickDiskUsage.this.usage = usage;

            } catch (Exception e) {
                logger.log(Level.INFO, "Unable to run disk usage check", e);
            } finally {
                SecurityContextHolder.setContext(impersonate);
            }
            lastRun = System.currentTimeMillis();
        }
    };
}
