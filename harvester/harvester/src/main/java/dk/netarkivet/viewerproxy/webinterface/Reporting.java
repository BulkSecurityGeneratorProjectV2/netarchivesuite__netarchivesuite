package dk.netarkivet.viewerproxy.webinterface;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.common.utils.cdx.ArchiveExtractCDXJob;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Methods for generating the batch results needed by the QA pages. 
 */
@SuppressWarnings({ "serial"})
public class Reporting {
    /**
     * Utility class, do not initialise.
     */
    private Reporting() {}

    /** The suffix for the data arc/warc files produced by Heritrix. */
    static final String archivefile_suffix = ".*\\.(w)?arc(\\.gz)?";
    
    /** The suffix for the data arc/warc metadata file created by NetarchiveSuite. */
    static final String metadatafile_suffix = "-metadata-[0-9]+\\.(w)?arc(\\.gz)?";
       
    /**
     * Submit a batch job to list all files for a job, and report result in a
     * sorted list.
     * @param jobid The job to get files for.
     * @param harvestprefix The harvestprefix for the files produced by heritrix
     * @return A sorted list of files.
     * @throws ArgumentNotValid If jobid is 0 or negative. 
     * @throws IOFailure On trouble generating the file list
     */
    public static List<String> getFilesForJob(int jobid, String harvestprefix) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        FileBatchJob fileListJob = new FileListJob();
        List<String> acceptedPatterns = new ArrayList<String>();
        acceptedPatterns.add(jobid + metadatafile_suffix);
        acceptedPatterns.add(harvestprefix + archivefile_suffix);
        fileListJob.processOnlyFilesMatching(acceptedPatterns);
        
        File f;
        try {
            f = File.createTempFile(jobid + "-files", ".txt",
                                    FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure("Could not create temorary file", e);
        }
        BatchStatus status
                = ArcRepositoryClientFactory.getViewerInstance().batch(
                fileListJob,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        List<String> lines = new ArrayList<String>(
                FileUtils.readListFromFile(f));
        FileUtils.remove(f);
        Set<String> linesAsSet = new HashSet<String>();
        linesAsSet.addAll(lines);
        lines = new ArrayList<String>();
        lines.addAll(linesAsSet);
        Collections.sort(lines);
        return lines;
    }

    /**
     * Submit a batch job to generate cdx for all metadata files for a job, and
     * report result in a list.
     * @param jobid The job to get cdx for.
     * @return A list of cdx records.
     * @throws ArgumentNotValid If jobid is 0 or negative.
     * @throws IOFailure On trouble generating the cdx
     */
    public static List<CDXRecord> getMetadataCDXRecordsForJob(long jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        FileBatchJob cdxJob = new ArchiveExtractCDXJob(false) {
            @Override
            public ArchiveBatchFilter getFilter() {
                return ArchiveBatchFilter.EXCLUDE_NON_WARCINFO_RECORDS;
            }
        };
        cdxJob.processOnlyFilesMatching(jobid + metadatafile_suffix);
        
        File f;
        try {
            f = File.createTempFile(jobid + "-reports", ".cdx",
                                    FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure("Could not create temporary file", e);
        }
        BatchStatus status
                = ArcRepositoryClientFactory.getViewerInstance().batch(
                cdxJob, Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        List<CDXRecord> records;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            records = new ArrayList<CDXRecord>();
            for (String line = reader.readLine();
                 line != null; line = reader.readLine()) {
                String[] parts = line.split("\\s+");
                CDXRecord record = new CDXRecord(parts);
                records.add(record);
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to read results from file '" + f
                                + "'", e);
        } finally {
            IOUtils.closeQuietly(reader);
            FileUtils.remove(f);
        }
        return records;
    }

    /**
     * Submit a batch job to extract the part of a crawl log that is associated
     * with the given domain and job.
     * @param domain The domain to get crawl.log-lines for.
     * @param jobid The jobid to get the crawl.log-lines for.
     * @return A file containing the crawl.log lines. This file is temporary,
     * and should be deleted after use.
     * @throws ArgumentNotValid On negative jobids, or if domain is null or the
     * empty string.
     */
    public static File getCrawlLogForDomainInJob(String domain, int jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        ArgumentNotValid.checkNotNullOrEmpty(domain, "String domain");
        FileBatchJob urlsForDomainBatchJob
                = new HarvestedUrlsForDomainBatchJob(domain);
        urlsForDomainBatchJob.processOnlyFilesMatching(
                jobid + metadatafile_suffix);
        return getResultFile(urlsForDomainBatchJob);
    }
    
    /** 
     * Helper method to get result from a batchjob.
     * @param batchJob a certain FileBatchJob
     * @return a file with the result.
     */
    private static File getResultFile(FileBatchJob batchJob) {
        File f;
        File fsorted;
        try {
            final String uuid = UUID.randomUUID().toString();
            f = File.createTempFile("temp", uuid + ".txt", 
                    FileUtils.getTempDir());
            f.deleteOnExit();
            fsorted = File.createTempFile("temp", uuid + "-sorted.txt",
                    FileUtils.getTempDir());
            fsorted.deleteOnExit();
        } catch (IOException e) {
            throw new IOFailure("Unable to create temporary file", e);
        }
        BatchStatus status
                = ArcRepositoryClientFactory.getViewerInstance().batch(
                batchJob,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        FileUtils.sortCrawlLogOnTimestamp(f, fsorted);
        FileUtils.remove(f);
        return fsorted;
    }

    /**
     * Return any crawllog lines for a given jobid  matching the given regular
     * expression. 
     * @param jobid The jobid 
     * @param regexp A regular expression
     * @return a File with the matching lines.
     */
    public static File getCrawlLoglinesMatchingRegexp(int jobid, String regexp) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        ArgumentNotValid.checkNotNullOrEmpty(regexp, "String regexp");
        FileBatchJob crawlLogBatchJob = new CrawlLogLinesMatchingRegexp(regexp);
        crawlLogBatchJob.processOnlyFilesMatching(jobid
                + metadatafile_suffix);
        return getResultFile(crawlLogBatchJob);
    }
    
}
