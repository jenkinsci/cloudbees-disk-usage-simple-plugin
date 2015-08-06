This is a silly simple disk usage thing.

When installed - it will use "du" command to estimate the size of jobs.
It does this with ionice to ensure it doesn't use too much io, and one job dir at a time. 
(allowing a gap between each job to prevent load average from climbing too high). 

This also only runs a maximum of once every 3 minutes, and only when requested, and only one at a time. 

To use it /disk-usage.

# Resources
* Continuous Integration: [![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/cloudbees-disk-usage-simple-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/cloudbees-disk-usage-simple-plugin)
* Issues Tracking: [Jira](https://issues.jenkins-ci.org/browse/JENKINS/component/20652)
