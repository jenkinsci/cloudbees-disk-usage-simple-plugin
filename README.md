This is a silly simple disk usage thing.

When installed - it will use "du" command to estimate the size of jobs.
It does this with ionice to ensure it doesn't use too much io, and one job dir at a time. 
(allowing a gap between each job to prevent load average from climbing too high). 

This also only runs a maximum of once every 3 minutes, and only when requested, and only one at a time. 

To use it /disk-usage.

