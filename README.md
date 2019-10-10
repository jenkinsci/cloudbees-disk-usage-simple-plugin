This is a simple disk usage plugin that calculates disk usage while limiting the performance impact on the Jenkins master.

# Features
* uses Java 7 NIO directory walker API to calculate the size of jobs and 1st level directories under `$JENKINS_HOME`
* throttle the directory scan to help prevent the load average from climbing too high
* a refresh of usage data is automatically launched when you display the screen and they are older than 15 minutes
* a refresh of usage data can be also manually requested, but only one at a time will occur

To use this plugin visit the `Manage Jenkins` -> `Disk usage` page.

# Resources
* Continuous Integration: [![Build Status](https://ci.jenkins.io/job/Plugins/job/cloudbees-disk-usage-simple-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/cloudbees-disk-usage-simple-plugin/job/master/)
* Issues Tracking: [Jira](https://issues.jenkins-ci.org/issues/?jql=project+%3D+JENKINS+AND+component+%3D+cloudbees-disk-usage-simple-plugin)
