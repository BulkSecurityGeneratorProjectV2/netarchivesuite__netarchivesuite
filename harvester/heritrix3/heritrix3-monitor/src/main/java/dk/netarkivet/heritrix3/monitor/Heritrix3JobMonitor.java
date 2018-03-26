/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.heritrix3.monitor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.netarchivesuite.heritrix3wrapper.ByteRange;
import org.netarchivesuite.heritrix3wrapper.Heritrix3Wrapper;
import org.netarchivesuite.heritrix3wrapper.JobResult;
import org.netarchivesuite.heritrix3wrapper.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.monitor.StartedJobInfo;

public class Heritrix3JobMonitor {

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(Heritrix3JobMonitorThread.class);

    protected NASEnvironment environment;

    public boolean bActive = true;

    public boolean bPull = false;

    public boolean bInitialized;

    public long jobId;

    public Job job;

    public Heritrix3Wrapper h3wrapper;

    public String h3HostnamePort;

    public String hostUrl;

    public String jobname;

    public JobResult jobResult;

    public String crawlLogFilePath;

    public IndexedTextFile indexedCrawllog;

    protected Heritrix3JobMonitor() {
    }

    public static Heritrix3JobMonitor getInstance(Long jobId, NASEnvironment environment) throws IOException {
        Heritrix3JobMonitor jobmonitor = new Heritrix3JobMonitor();
        jobmonitor.environment = environment;
        jobmonitor.jobId = jobId;
        jobmonitor.indexedCrawllog = new IndexedTextFile(environment.tempPath, "crawllog-" + jobId);
        jobmonitor.init();
        return jobmonitor;
    }

    public synchronized void init() {
        try {
            if (bActive && !bInitialized) {
                if (job == null) {
                    job = Heritrix3JobMonitorThread.jobDAO.read(jobId);
                }
                if (h3wrapper == null) {
                    StartedJobInfo startedInfo = Heritrix3JobMonitorThread.runningJobsInfoDAO.getMostRecentByJobId(jobId);
                    if (startedInfo != null) {
                        hostUrl = startedInfo.getHostUrl();
                        if (hostUrl != null && hostUrl.length() > 0) {
                            h3wrapper = Heritrix3WrapperManager.getHeritrix3Wrapper(hostUrl, environment.h3AdminName, environment.h3AdminPassword);
                        }
                    }
                }
                if (jobname == null && h3wrapper != null) {
                    jobname = Heritrix3WrapperManager.getJobname(h3wrapper, jobId);
                }
                if ((jobResult == null || jobResult.job == null) && jobname != null) {
                    jobResult = h3wrapper.job(jobname);
                }
                if (jobResult != null && jobResult.job != null) {
                    crawlLogFilePath = jobResult.job.crawlLogFilePath;
                }
                if (crawlLogFilePath != null) {
                	indexedCrawllog.init();
                    bInitialized = true;
                }
            }
        } catch (Throwable t) {
        	// TODO Save this state internally.
        }
    }

    public synchronized void update() {
        try {
            if (job != null) {
                Job tmpJob = job = Heritrix3JobMonitorThread.jobDAO.read(jobId);
                if (tmpJob != null) {
                    job = tmpJob;
                }
            }
            if (jobResult != null && jobResult.job != null && jobname != null) {
                JobResult tmpJobResult = h3wrapper.job(jobname);
                if (tmpJobResult != null) {
                    jobResult = tmpJobResult;
                }
            }
        } catch (Throwable t) {
        	// TODO Save this state internally.
        }
    }

    public synchronized void updateCrawlLog(byte[] tmpBuf) {
        long pos;
        long to;
        boolean bLoop;
        ByteRange byteRange;
        try {
            if (bActive && !bInitialized) {
                init();
            }
            if (bActive && bInitialized) {
                bLoop = true;
                while (bLoop) {
                    pos = indexedCrawllog.getTextFilesize();
                    to = pos;
                    if (jobResult != null && jobResult.job != null && jobResult.job.crawlLogFilePath != null) {
                        long rangeFrom = pos;
                        long rangeTo = pos + tmpBuf.length - 1;
                        StreamResult anypathResult = h3wrapper.anypath(jobResult.job.crawlLogFilePath, null, null, true);
                        if (anypathResult != null && rangeFrom < anypathResult.contentLength) {
                            LOG.info("Crawllog length for job {}={}.", jobId, anypathResult.contentLength);
                            if (rangeTo >= anypathResult.contentLength) {
                                rangeTo = anypathResult.contentLength - 1;
                            }
                            anypathResult = h3wrapper.anypath(jobResult.job.crawlLogFilePath, rangeFrom, rangeTo);
                            LOG.info("Crawllog byterange download for job {}. ({}-{})", jobId, rangeFrom, rangeTo);
                            if (anypathResult != null && anypathResult.byteRange != null && anypathResult.in != null) {
                                byteRange = anypathResult.byteRange;
                                if (byteRange.contentLength > 0) {
                                	to = indexedCrawllog.write(anypathResult.in, tmpBuf, to);
                                    IOUtils.closeQuietly(anypathResult);
                                    if (byteRange.contentLength == to) {
                                        bLoop = false;
                                    }
                                } else {
                                    bLoop = false;
                                }
                            } else {
                                bLoop = false;
                            }
                        } else {
                            bLoop = false;
                        }
                    } else {
                        bLoop = false;
                    }
                }
            }
        } catch (Throwable t) {
        	// TODO Save this state internally.
        }
    }

    public synchronized void cleanup(List<File> oldFilesList) {
        try {
            bActive = false;
            bInitialized = false;
            hostUrl = null;
            h3wrapper = null;
            jobname = null;
            jobResult = null;
            crawlLogFilePath = null;
            indexedCrawllog.close();
            indexedCrawllog.addFilesToOldFilesList(oldFilesList);
            Iterator<IndexedTextFileSearchResult> srIter = qSearchResultMap.values().iterator();
            IndexedTextFileSearchResult sr;
            while (srIter.hasNext()) {
                sr = srIter.next();
                sr.close();
                sr.addFilesToOldFilesList(oldFilesList);
            }
            qSearchResultMap.clear();
        } catch (Throwable t) {
        	LOG.error("Error while closing monitor for job {}!", t, jobId);
        }
    }

    public synchronized boolean isReady() {
        return (bActive && bInitialized);
    }

    /** Map of cached search results. */
    protected Map<String, IndexedTextFileSearchResult> qSearchResultMap = new HashMap<String, IndexedTextFileSearchResult>();

    /** Internal counter used when storing cached search files. */
    protected int searchResultNr = 1;

    /**
     * Returns a <code>SearchResult</code> object used to search in the crawllog.
     * @param q search string
     * @return a <code>SearchResult</code> object used to search in the crawllog
     * @throws IOException if an I/O exceptions occurs whule creating a <code>SearchResult</code> object 
     */
    public synchronized IndexedTextFileSearchResult getSearchResult(String q) throws IOException {
        IndexedTextFileSearchResult searchResult = qSearchResultMap.get(q);
        if (searchResult == null) {
            searchResult = new IndexedTextFileSearchResult(indexedCrawllog.textFile, environment.tempPath, "crawllog-" + jobId, q, searchResultNr++);
            qSearchResultMap.put(q, searchResult);
        }
        return searchResult;
    }

    /**
     * Set the file path to the crawl log
     * @param crawlLogFilePath File path to the crawl log
     */
    public void setCrawlLogFilePath(String crawlLogFilePath) {
        this.crawlLogFilePath = crawlLogFilePath;
    }

}
