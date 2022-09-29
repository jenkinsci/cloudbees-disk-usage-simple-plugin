package com.cloudbees.simplediskusage;

import hudson.FilePath;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compute disk usage of a list of paths. Results are published using
 * listeners registered for interesting paths, so we only walk the disk once.
 *
 * The walker process is throttled to prevent IO starvation for other Jenkins
 * tasks.
 */
public class UsageComputation {

    public interface CompletionListener {
        void onCompleted(Path dir, long usage);
    }

    private final Map<Path, CompletionListener> listenerMap;
    private final List<Path> pathsToScan;

    public UsageComputation(List<Path> pathsToScan) {
        this.pathsToScan = pathsToScan;
        this.listenerMap = new HashMap<>();
    }

    public void addListener(Path path, CompletionListener listener) {
        listenerMap.put(path.toAbsolutePath(), listener);
    }

    public int getItemsCount() {
        return listenerMap.size();
    }

    public void compute() throws IOException {
        for (Path path : pathsToScan) {
            computeUsage(path.toAbsolutePath());
        }
    }

    public void computeFS() {
        // setting the disk space usage for the entire FS        
        for (Path path : pathsToScan) {
            try {
                Path dir = path.toAbsolutePath();
                long pathDiskUsage = jenkinsFSUsage();
                CompletionListener listener = listenerMap.get(dir);
                if (listener != null) {
                    listener.onCompleted(dir, pathDiskUsage);
                }
            }
            catch (Exception e){
                logger.log(Level.WARNING, "cloudbees-disk-usage-plugin: FS information could not get acquired.");
            }
        }
    }

    protected long jenkinsFSUsage() {
        File rd = Jenkins.get().getRootDir();
        long totalJenkins = rd.getTotalSpace();
        long usableJenkins = rd.getUsableSpace();
        if (usableJenkins <= 0 || totalJenkins <= 0) {
            // information unavailable. pointless to try.
            logger.log(Level.WARNING, "cloudbees-disk-usage-plugin: JENKINS_HOME disk usage information isn't available.");
            return -1;
        }
        return (totalJenkins - usableJenkins);
    }

    protected void computeUsage(Path path) throws IOException {
        // we don't really need AtomicLong here, walking the tree is synchronous, but
        // it's convenient for the operations it provides

        // used to throttle IO
        final AtomicLong chunkStartTime = new AtomicLong(System.currentTimeMillis());

        // used to lock this thread if there's a FS freeze ongoing
        final AtomicLong writableLastCheckTime = new AtomicLong(System.currentTimeMillis());

        final Stack<AtomicLong> computeStack = new Stack<>();
        computeStack.push(new AtomicLong(0));
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                computeStack.push(new AtomicLong(0));

                // check every 10 seconds that the process can write on JENKINS_HOME
                // this will lock this thread if the filesystem is frozen
                // this is to speed up the FS freeze operation which is otherwise slowed down
                if (System.currentTimeMillis() - writableLastCheckTime.get() > 10000) {
                    writableLastCheckTime.set(System.currentTimeMillis());
                    FilePath jenkinsHome = Jenkins.get().getRootPath();
                    try {
                        jenkinsHome.touch(System.currentTimeMillis());
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Exception while touching JENKINS_HOME", e);
                    }
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                computeStack.peek().addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (exc != null) {
                    logger.log(Level.WARNING, "Exception thrown while walking {}: {}", new Object[] {dir, exc });
                }

                // throttle the walking process so it only consumes at most half of the available IO bandwidth
                // only pause every 100ms to ensure the walk is efficient anyway
                long runTimeInMillis = System.currentTimeMillis() - chunkStartTime.get();
                if (runTimeInMillis > 100) {
                    try {
                        Thread.sleep(runTimeInMillis);
                        chunkStartTime.set(System.currentTimeMillis());
                    } catch (InterruptedException e) {
                        return FileVisitResult.TERMINATE;
                    }
                }

                long pathDiskUsage = computeStack.pop().get();
                CompletionListener listener = listenerMap.get(dir);
                if (listener != null) {
                    listener.onCompleted(dir, pathDiskUsage);
                }

                computeStack.peek().addAndGet(pathDiskUsage);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final Logger logger = Logger.getLogger(UsageComputation.class.getName());
}
