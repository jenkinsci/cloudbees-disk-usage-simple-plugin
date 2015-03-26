package com.cloudbees.simplediskusage;

import hudson.Extension;
import hudson.model.*;
import hudson.security.ACL;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.*;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.*;
import java.util.Comparator;
import java.util.Date;
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

    Map<TopLevelItem, Long> usage = new HashMap<TopLevelItem, Long>();
    long lastRun = 0;

    public Map<TopLevelItem, Long> getDiskUsage() throws IOException {
        fetchUsage(Jenkins.getInstance().getRootDir());
        Map<TopLevelItem, Long> sorted =  new TreeMap<TopLevelItem, Long>(BY_NAME);
        sorted.putAll(usage);
        return sorted;
    }

    private static Comparator<TopLevelItem> BY_NAME = new Comparator<TopLevelItem>() {
        @Override
        public int compare(TopLevelItem o1, TopLevelItem o2) {
            return o1.getFullName().compareTo(o2.getFullName());
        }
    };



    public long getLastRun() {
        return lastRun;
    }

    public Date getLastRunDate() {
        return new Date(lastRun);
    }

    public void doRefresh(StaplerRequest req, StaplerResponse res) throws IOException, ServletException {
        lastRun = 0;
        usage.clear();
        fetchUsage(Jenkins.getInstance().getRootDir());
        res.forwardToPreviousPage(req);
    }

    public void doClean(StaplerRequest req, StaplerResponse res) throws IOException, ServletException, InterruptedException {
        Job job = Jenkins.getInstance().getItemByFullName(req.getParameter("job"), Job.class);
        job.logRotate();
        res.forwardToPreviousPage(req);
    }


    @Override
    public Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }

    private void fetchUsage(final File rootDir) throws IOException {
        final QuickDiskUsage self = this;
        if (System.currentTimeMillis() - lastRun < 3 * 60 * 1000) {
            return;
        }
        logger.info("Re-estimating disk usage");
        ex.execute(new Runnable() {
            public void run() {
                Map<TopLevelItem, Long> usage = new HashMap<TopLevelItem, Long>();
                SecurityContext impersonate = ACL.impersonate(ACL.SYSTEM);
                try {
                    for (TopLevelItem item : Jenkins.getInstance().getAllItems(TopLevelItem.class)) {
                        usage.put(item, duJob(item));
                    }
                    logger.info("Finished re-estimating disk usage.");
                    QuickDiskUsage.this.usage = usage;

                } catch (Exception e) {
                    logger.log(Level.INFO, "Unable to run disk usage check", e);
                } finally {
                    SecurityContextHolder.setContext(impersonate);
                }
                lastRun = System.currentTimeMillis();
            }
        });

    }

    private long duJob(TopLevelItem item) throws IOException, InterruptedException {
        logger.info("Estimating usage for: " + item.getDisplayName());
        Process p = Runtime.getRuntime().exec(DISK_USAGE, null, item.getRootDir());
        StringBuilder du = new StringBuilder();
        BufferedReader stdOut = new BufferedReader (new InputStreamReader(p.getInputStream()));
        String line = stdOut.readLine();

        Thread.sleep(1000); //To keep load average nice and low
        if (line.matches("[0-9]*\t.")) return Long.parseLong(line.substring(0, line.length() -2));
        logger.log(Level.WARNING, "failed to parse `du` output : "+line);
        return -1;
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

}
