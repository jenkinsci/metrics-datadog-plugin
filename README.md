# Metrics DataDog Plugin

This plugin streams [Metrics](http://wiki.jenkins-ci.org/display/JENKINS/Metrics+Plugin) to
a [DataDog](https://www.datadoghq.com/).

Currently, this plugin only supports push of metrics through [DogStatsD](https://docs.datadoghq.com/developers/dogstatsd/) (UDP).
A DataDog agent with DogStatsD enabled and corresponding UDP port opened needs to be reachable from your Jenkins instance.

See also this [plugin's wiki page][wiki].

# Environment

The following build environment is required to build this plugin

* `java-1.8` and `maven-3.5.x`

# Build

To build the plugin locally:

    mvn clean verify

# Release

To release the plugin:

    mvn release:prepare release:perform -B

# Test with local instance

## Start a local DataDog agent

    docker run -d --name dd-agent -v /var/run/docker.sock:/var/run/docker.sock:ro -v /proc/:/host/proc/:ro -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro -e DD_API_KEY=XXXXXXXXXXXXXXXXXXX -p 8125:8125/udp -e DD_TAGS="env:local-dev owner:mpailloncy" datadog/agent:latest


## Start a local Jenkins instance

    mvn hpi:run


You can then configure Jenkins to target your DataDog agent DogStatsD UDP port.

  [wiki]: http://wiki.jenkins-ci.org/display/JENKINS/Metrics+DataDog+Plugin

