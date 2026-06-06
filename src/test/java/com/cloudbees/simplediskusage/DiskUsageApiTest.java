package com.cloudbees.simplediskusage;

import hudson.model.FreeStyleProject;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class DiskUsageApiTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void apiEndpointIsReachable() throws Exception {
        var response = j.createWebClient().goTo("manage/disk-usage-simple/api/json", "application/json");
        assertEquals(200, response.getWebResponse().getStatusCode());
    }

    @Test
    void apiResponseContainsTopLevelFields() throws Exception {
        var response = j.createWebClient().goTo("manage/disk-usage-simple/api/json", "application/json");
        var json = JSONObject.fromObject(response.getWebResponse().getContentAsString());

        assertTrue(json.has("directoriesUsages"), "missing directoriesUsages");
        assertTrue(json.has("jobsUsages"), "missing jobsUsages");
        assertTrue(json.has("lastRunStart"), "missing lastRunStart");
        assertTrue(json.has("lastRunEnd"), "missing lastRunEnd");
    }

    @Test
    void directoryItemsExposeExpectedFields() throws Exception {
        var plugin = j.jenkins.getPlugin(QuickDiskUsagePlugin.class);
        assertNotNull(plugin);

        // Inject a known item directly so the test doesn't depend on the slow async scan
        inject(plugin, "directoriesUsages", List.of(
                new DiskItem("TEST_DIR", new File("/tmp"), 2048L, 42L)));
        markScanComplete(plugin);

        var response = j.createWebClient().goTo("manage/disk-usage-simple/api/json", "application/json");
        var json = JSONObject.fromObject(response.getWebResponse().getContentAsString());
        var dirs = json.getJSONArray("directoriesUsages");

        assertFalse(dirs.isEmpty(), "directoriesUsages should not be empty");

        var item = dirs.getJSONObject(0);
        assertTrue(item.has("displayName"), "missing displayName");
        assertTrue(item.has("usageKB"), "missing usageKB");
        assertTrue(item.has("count"), "missing count");
        assertTrue(item.has("pathString"), "missing pathString");
        assertFalse(item.has("usage"), "should not expose raw 'usage' field — use 'usageKB'");

        assertEquals("TEST_DIR", item.getString("displayName"));
        assertEquals(2048L, item.getLong("usageKB"));
        assertEquals(42L, item.getLong("count"));
    }

    @Test
    void jobItemsExposeExpectedFields() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("api-test-job");
        var plugin = j.jenkins.getPlugin(QuickDiskUsagePlugin.class);
        assertNotNull(plugin);

        inject(plugin, "jobsUsages", List.of(
                new JobDiskItem(project, 512L, 7L)));
        markScanComplete(plugin);

        var response = j.createWebClient().goTo("manage/disk-usage-simple/api/json", "application/json");
        var json = JSONObject.fromObject(response.getWebResponse().getContentAsString());
        var jobs = json.getJSONArray("jobsUsages");

        var jobEntry = findByFullName(jobs, "api-test-job");
        assertNotNull(jobEntry, "api-test-job not found in jobsUsages");

        assertTrue(jobEntry.has("displayName"), "missing displayName");
        assertTrue(jobEntry.has("usageKB"), "missing usageKB");
        assertTrue(jobEntry.has("count"), "missing count");
        assertTrue(jobEntry.has("pathString"), "missing pathString");
        assertTrue(jobEntry.has("fullName"), "missing fullName");
        assertTrue(jobEntry.has("url"), "missing url");
        assertFalse(jobEntry.has("usage"), "should not expose raw 'usage' field — use 'usageKB'");

        assertEquals("api-test-job", jobEntry.getString("fullName"));
        assertEquals(512L, jobEntry.getLong("usageKB"));
    }

    @Test
    void usageKBReflectsActualDiskSize() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("disk-size-job");

        // Write ~1MB of data into the job's root dir (simulating archived artifacts)
        var artifactDir = new File(project.getRootDir(), "builds/1/archive");
        artifactDir.mkdirs();
        Files.write(new File(artifactDir, "data.bin").toPath(), new byte[1024 * 1024]);

        // Run UsageComputation synchronously on just this one job directory,
        // bypassing the slow full-Jenkins scan that also walks java.io.tmpdir
        var result = new AtomicLong(-1);
        var uc = new UsageComputation(List.of(project.getRootDir().toPath()));
        uc.addListener(project.getRootDir().toPath(), (dir, usage, count) -> result.set(usage));
        uc.compute();

        assertTrue(result.get() >= 0, "UsageComputation did not call the listener");
        // usage from UsageComputation is in bytes; plugin divides by 1024 before storing
        long usageKB = result.get() / 1024;
        // The 1MB file should produce ~1024 KB (within 10% tolerance for filesystem metadata)
        assertTrue(usageKB >= 900, "expected usageKB >= 900, got " + usageKB);
        assertTrue(usageKB <= 2048, "expected usageKB <= 2048, got " + usageKB);
    }

    @Test
    void xmlEndpointIsReachable() throws Exception {
        var response = j.createWebClient().goTo("manage/disk-usage-simple/api/xml", "application/xml");
        assertEquals(200, response.getWebResponse().getStatusCode());
        var body = response.getWebResponse().getContentAsString();
        // Stapler derives the root element name from the class name; attributes may follow
        assertTrue(body.contains("<quickDiskUsagePlugin"), "XML response missing root element");
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private <T> void inject(QuickDiskUsagePlugin plugin, String fieldName, List<T> items)
            throws ReflectiveOperationException {
        Field f = QuickDiskUsagePlugin.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        var list = (CopyOnWriteArrayList<T>) f.get(plugin);
        list.clear();
        list.addAll(items);
    }

    private void markScanComplete(QuickDiskUsagePlugin plugin) throws ReflectiveOperationException {
        long now = System.currentTimeMillis();
        Field start = QuickDiskUsagePlugin.class.getDeclaredField("lastRunStart");
        Field end = QuickDiskUsagePlugin.class.getDeclaredField("lastRunEnd");
        start.setAccessible(true);
        end.setAccessible(true);
        start.set(plugin, now - 1000);
        end.set(plugin, now);
    }

    private JSONObject findByFullName(JSONArray jobs, String fullName) {
        for (int i = 0; i < jobs.size(); i++) {
            var entry = jobs.getJSONObject(i);
            if (fullName.equals(entry.optString("fullName"))) {
                return entry;
            }
        }
        return null;
    }
}
