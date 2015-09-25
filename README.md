This is a silly simple disk usage thing.

When installed - it will use "du" command to estimate the size of jobs and 1st level directories under `$JENKINS_HOME`.
It does this with ionice to ensure it doesn't use too much io, and one job dir at a time. 
(allowing a gap between each job to prevent load average from climbing too high). 

A refresh of data is automatically launched when you display the screen and they are older than 15 minutes. 
A refresh can be also manually requested, but only one at a time will occur. 

To use it `Manage Jenkins` -> `Disk usage`.

# Resources
* Continuous Integration: [![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/cloudbees-disk-usage-simple-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/job/cloudbees-disk-usage-simple-plugin)
* Issues Tracking: [Jira](https://issues.jenkins-ci.org/browse/JENKINS/component/20652)
