package com.cloudbees.dockerpublish;

import hudson.Extension;
import hudson.Launcher;
import hudson.console.ConsoleNote;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.*;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Expose endpoint for nagios to hit for monitoring the health of this instance.
 */
@Extension
@Singleton
public class QuickDiskUsage implements UnprotectedRootAction {


    static Executor ex = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("Simple disk usage checker");
            return t;
        }
    });

    String currentLog = "(not yet calculated, please try again soon)";
    long lastRun = 0;


    /**
     * This is accessed via: http://localhost:8080/jenkins/disk-usage
     * This will walk the tree of jobs, and do something. Exploring api.
     */
    public HttpResponse doIndex(final StaplerRequest req, final @QueryParameter String job) throws IOException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

        return new HttpResponse() {
            public void generateResponse(StaplerRequest staplerRequest, StaplerResponse staplerResponse, Object o) throws IOException, ServletException {
                fetchUsage(Jenkins.getInstance().getRootDir());
                staplerResponse.getWriter().println(currentLog);
            }
        };
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

                    File dir = new File(rootDir, "jobs");
                    File[] jobs = dir.listFiles(new FileFilter() {
                        public boolean accept(File pathname) {
                            return pathname.isDirectory();
                        }
                    });
                    duJob(lines, jobs);
                    self.currentLog = lines.toString();
                } catch (Exception e) {
                    logger.log(Level.INFO, "Unable to run disk usage check", e);
                }
                lastRun = System.currentTimeMillis();
            }
        });

    }

    private void duJob(StringBuffer lines, File[] jobs) throws IOException, InterruptedException {
        for (File jobDir : jobs) {
            logger.info("Estimating usage for: " + jobDir.getName());
            Process p = Runtime.getRuntime().exec("ionice -c 3 du -khs", null, jobDir);
            lines.append(jobDir.getName() + ": ");
            String line;
            BufferedReader stdOut = new BufferedReader (new InputStreamReader(p.getInputStream()));
            while ((line = stdOut.readLine ()) != null) {
                lines.append(line + "\n");
            }
            stdOut.close();
            Thread.sleep(1000); //To keep load average nice and low
        }
        logger.info("Finished re-estimating disk usage.");
    }


    public String getIconFileName() {
        return null;
    }




    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "disk-usage";
    }


    private static final Logger logger = Logger.getLogger(QuickDiskUsage.class.getName());

}
