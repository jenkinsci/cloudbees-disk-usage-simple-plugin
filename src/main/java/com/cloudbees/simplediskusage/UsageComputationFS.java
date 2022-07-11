package com.cloudbees.simplediskusage;

import hudson.FilePath;
import jenkins.model.Jenkins;

import javax.inject.Singleton;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper class that will include fileSystem commands to compute disk usage
 * Should be very quick to run, as no file traversal is included
 */
public class UsageComputationFS {

    private static String getRelatedFS(ArrayList<String> fileSystemList, String path){
        // output would be the closest path to a list of filesystems
        String relatedFS = "";
        int tempSimilarityValue = -1;
        
        for (String singleFS: fileSystemList){
            int similarityValue = path.compareTo(singleFS);
            if (similarityValue == 0){
                return singleFS;
            }
            else if (similarityValue > tempSimilarityValue){
                relatedFS = singleFS;
                tempSimilarityValue = similarityValue;
            }
        }
        if (tempSimilarityValue >= 0){
            return relatedFS;
        }
        return null;
    }

    private static ArrayList<String> listFileSystems(ArrayList excludeFS) throws IOException{
        // ArrayList fileStoreList;
        ArrayList<String> fileStoreList = new ArrayList<String>();

        String osname = System.getProperty("os.name");
        if (osname.startsWith("Windows")){
            // Windows
            for (Path fs: FileSystems.getDefault().getRootDirectories()){
                fileStoreList.add(fs.toString());
            }
        }
        else {
            // linux
            for (FileStore fs: FileSystems.getDefault().getFileStores()) {
                if (!excludeFS.contains(fs.type().toString())){
                    fileStoreList.add(fs.toString().split(" ")[0]);
                }
            }
        }
        return fileStoreList;
    }

    public static long jenkinsFSUsage() {
        Jenkins jenkins = Jenkins.get();
        File rd = jenkins.getRootDir();
        long totalJenkins = rd.getTotalSpace();
        long usableJenkins = rd.getUsableSpace();
        return (totalJenkins - usableJenkins);
    }

    public static String jenkinsFS() throws IOException{
        // Linux exception type to ignore:
        ArrayList<String> ignoreFSTypes = new ArrayList<String>(Arrays.asList("cgroup","proc"));
        ArrayList<String> fileStoreList = listFileSystems(ignoreFSTypes);
        
        Jenkins jenkins = Jenkins.get();
        String jenkinsRootDir = jenkins.getRootDir().toString();

        // returns closest Jenkins FileSystem
        return getRelatedFS(fileStoreList, jenkinsRootDir);
    }

    public static String jenkinsFSExactMatch() throws IOException{
        Jenkins jenkins = Jenkins.get();
        String jenkinsRootDir = jenkins.getRootDir().toString();
        
        if (jenkinsFS().equals(jenkinsRootDir)){
            return jenkinsFS();
        }
        return "";
    }
}