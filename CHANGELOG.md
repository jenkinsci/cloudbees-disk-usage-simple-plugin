# Changelog

#### 0.9 (2017-08-03)

-   Postpone
    disk usage update after Jenkins is fully up
    ([JENKINS-45943](https://issues.jenkins-ci.org/browse/JENKINS-45943))[](https://issues.jenkins-ci.org/browse/JENKINS-44646)

#### 0.8 (2017-06-06)

-   Reduce scan impact on filesystem freeze
    ([JENKINS-44646](https://issues.jenkins-ci.org/browse/JENKINS-44646))

#### 0.7 (2017-05-24)

-   Rewrite the low level code to use Java 7 NIO directory walker. The
    plugin does not rely on du command anymore and works everywhere now.
    ([JENKINS-44445](https://issues.jenkins-ci.org/browse/JENKINS-44445))

#### 0.6 (2017-05-24)

- Not released, had permission issue during publication

#### 0.5 (2015-10-05)

-   `du` wasn't correctly launched using `ionice`
-   Don't let `du` processes run for more than 20 seconds for a period
    of time. The periodic task will retry to compute directory size next
    time it gets scheduled. By that time, we assume that OS system cache
    will hold more inode entries to complete faster.
-   Use the the system property
    `com.cloudbees.simplediskusage.QuickDiskUsagePlugin.command` to
    override the `du` command

#### 0.4 (2015-09-25)

-   Plugin is too verbose
    ([JENKINS-30279](https://issues.jenkins-ci.org/browse/JENKINS-30279))
-   When a refresh of data is manually triggered, it is processed 2
    times
    ([JENKINS-30631](https://issues.jenkins-ci.org/browse/JENKINS-30631))
-   When a refresh of data is manually triggered, the screen displays
    nothing
    ([JENKINS-30633](https://issues.jenkins-ci.org/browse/JENKINS-30633))
-   List JENKINS\_HOME and its first level sub directories disk usages
    ([JENKINS-30311](https://issues.jenkins-ci.org/browse/JENKINS-30311))
-   Computes data when Jenkins is up and stores them across restarts
    ([JENKINS-30635](https://issues.jenkins-ci.org/browse/JENKINS-30635))
-   Improve messages about data status
    ([JENKINS-30636](https://issues.jenkins-ci.org/browse/JENKINS-30636))
-   Allow auto-refresh on disk usage screens
    ([JENKINS-30637](https://issues.jenkins-ci.org/browse/JENKINS-30637))
-   Increase the refresh period from 3 to 15 minutes
    ([JENKINS-30638](https://issues.jenkins-ci.org/browse/JENKINS-30638))  
    + various UI and code improvements

#### 0.3 (2015-08-06)

-   Display disk usage of \`java.io.tmpdir\`
    ([JENKINS-29516](https://issues.jenkins-ci.org/browse/JENKINS-29516))
-   The plugin identifier was renamed from cloudbees-disk-usage-simple
    to cloudbees-disk-usage-simple-plugin. You will have to uninstall
    the 0.1 version and install the 0.2 from the update center.
-   Improve message displayed when statistics aren't yet available

#### 0.2

- Trashed use 0.3 instead

#### 0.1

- Initial release
