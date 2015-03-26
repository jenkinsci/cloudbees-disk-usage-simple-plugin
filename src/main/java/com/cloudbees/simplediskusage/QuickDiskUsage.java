package com.cloudbees.simplediskusage;

import hudson.Extension;
import hudson.model.*;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.*;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.*;
import java.util.Map;
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
                    ? "du -khs" // OSX doesn't have ionice, this is only used during dev on my laptop
                    : "ionice -c 3 du -khs";

    static Executor ex = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Simple disk usage checker");
            return t;
        }
    });

    String currentLog = "(not yet calculated, please try again soon)";
    long lastRun = 0;

    public String getDiskUsage() throws IOException {
        fetchUsage(Jenkins.getInstance().getRootDir());
        return currentLog;
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
                try {
                    StringBuffer lines = new StringBuffer();

                    for (TopLevelItem item : Jenkins.getInstance().getAllItems(TopLevelItem.class)) {
                        duJob(lines, item);
                    }
                    logger.info("Finished re-estimating disk usage.");

                    self.currentLog = lines.toString();
                } catch (Exception e) {
                    logger.log(Level.INFO, "Unable to run disk usage check", e);
                }
                lastRun = System.currentTimeMillis();
            }
        });

    }

    private void duJob(StringBuffer lines, TopLevelItem item) throws IOException, InterruptedException {
        logger.info("Estimating usage for: " + item.getDisplayName());
        Process p = Runtime.getRuntime().exec(DISK_USAGE, null, item.getRootDir());
        lines.append(item.getDisplayName() + ": ");
        String line;
        BufferedReader stdOut = new BufferedReader (new InputStreamReader(p.getInputStream()));
        while ((line = stdOut.readLine ()) != null) {
            lines.append(line + "\n");
        }
        stdOut.close();

        Thread.sleep(1000); //To keep load average nice and low
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
