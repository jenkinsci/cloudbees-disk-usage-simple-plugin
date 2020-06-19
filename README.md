# CloudBees Disk Usage Simple Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/cloudbees-disk-usage-simple-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/cloudbees-disk-usage-simple-plugin/job/master/)
[![Contributors](https://img.shields.io/github/contributors/jenkinsci/cloudbees-disk-usage-simple-plugin.svg)](https://github.com/jenkinsci/cloudbees-disk-usage-simple-plugin/graphs/contributors)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/cloudbees-disk-usage-simple.svg)](https://plugins.jenkins.io/cloudbees-disk-usage-simple)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/cloudbees-disk-usage-simple-plugin.svg?label=changelog)](https://github.com/jenkinsci/cloudbees-disk-usage-simple-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/cloudbees-disk-usage-simple.svg?color=blue)](https://plugins.jenkins.io/cloudbees-disk-usage-simple)

Simple disk usage plugin that calculates disk usage while limiting the performance impact on the Jenkins master.

# Features
* uses Java 7 NIO directory walker API to calculate the size of jobs and 1st level directories under `$JENKINS_HOME`
* throttle the directory scan to help prevent the load average from climbing too high
* refreshes usage data when you load the disk usage page, and the data is older than 15 minutes
* a refresh of usage data can be manually requested, but only one at a time will occur

To use this plugin visit the `Manage Jenkins` -> `Disk usage` page.

# Resources
* Issues Tracking: [Jira](https://issues.jenkins-ci.org/issues/?jql=project+%3D+JENKINS+AND+component+%3D+cloudbees-disk-usage-simple-plugin)
