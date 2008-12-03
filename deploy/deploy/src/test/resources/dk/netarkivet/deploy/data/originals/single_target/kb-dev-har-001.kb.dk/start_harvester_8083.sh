#!/bin/bash
export CLASSPATH=/home/dev/UNITTEST/lib/dk.netarkivet.harvester.jar:/home/dev/UNITTEST/lib/dk.netarkivet.archive.jar:/home/dev/UNITTEST/lib/dk.netarkivet.viewerproxy.jar:/home/dev/UNITTEST/lib/dk.netarkivet.monitor.jar:$CLASSPATH;
cd /home/dev/UNITTEST
java -Xmx1536m  -Dsettings.harvester.harvesting.heritrix.guiPort=8094  -Dsettings.harvester.harvesting.heritrix.jmxPort=8095 -Ddk.netarkivet.settings.file=/home/dev/UNITTEST/conf/settings_harvester_8083.xml -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -Djava.util.logging.config.file=/home/dev/UNITTEST/conf/log_harvestcontrollerapplication.prop -Dsettings.common.jmx.port=8102 -Dsettings.common.jmx.rmiPort=8202 -Dsettings.common.jmx.passwordFile=/home/dev/UNITTEST/conf/jmxremote.password -Djava.security.manager -Djava.security.policy=/home/dev/UNITTEST/conf/security.policy  dk.netarkivet.harvester.harvesting.HarvestControllerApplication < /dev/null > start_harvester_8083.sh.log 2>&1 &
