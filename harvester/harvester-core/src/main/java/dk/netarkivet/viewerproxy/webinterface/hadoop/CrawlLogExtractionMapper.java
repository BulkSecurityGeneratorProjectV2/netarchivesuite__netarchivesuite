package dk.netarkivet.viewerproxy.webinterface.hadoop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.viewerproxy.webinterface.CrawlLogLinesMatchingRegexp;

/**
 * Hadoop Mapper for extracting crawllog lines from metadata files.
 * Expects the Configuration provided for the job to have a regex set, which is used to filter for relevant lines.
 * If no regex is set an all-matching regex will be used.
 */
public class CrawlLogExtractionMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
    private static final Logger log = LoggerFactory.getLogger(CrawlLogExtractionMapper.class);

    /**
     * Mapping method.
     *
     * @param linenumber The linenumber. Is ignored.
     * @param archiveFilePath The path to the archive file.
     * @param context Context used for writing output.
     * @throws IOException If it fails to generate the CDX indexes.
     */
    @Override
    protected void map(LongWritable linenumber, Text archiveFilePath, Context context) throws IOException,
            InterruptedException {
        // reject empty or null warc paths.
        if (archiveFilePath == null || archiveFilePath.toString().trim().isEmpty()) {
            log.warn("Encountered empty path in job {}", context.getJobID().toString());
            return;
        }
        Path path = new Path(archiveFilePath.toString());
        Configuration conf = context.getConfiguration();
        List<String> crawlLogLines;
        Pattern crawlLogRegex = conf.getPattern("regex", Pattern.compile(".*"));

        log.info("Extracting crawl log lines matching regex: {}", crawlLogRegex);
        final FileSystem fileSystem = path.getFileSystem(conf);
        if (!(fileSystem instanceof LocalFileSystem)) {
            final String status = "Crawl log extraction only implemented for LocalFileSystem. Cannot extract from " + path;
            context.setStatus(status);
            System.err.println(status);
            crawlLogLines = new ArrayList<>();
        } else {
            LocalFileSystem localFileSystem = ((LocalFileSystem) fileSystem);
            crawlLogLines = extractCrawlLogLines(localFileSystem.pathToFile(path), crawlLogRegex.pattern());
        }

        for (String crawlLog : crawlLogLines) {
            context.write(NullWritable.get(), new Text(crawlLog));
        }
    }

    /**
     * Extract the crawl logs from a file matching the provided regex
     * @param file File to look for crawl logs in.
     * @param regex The regex to match lines with.
     * @return A list of crawl log lines extracted from the file.
     */
    private List<String> extractCrawlLogLines(File file, String regex) {
        FileBatchJob batchJob = new CrawlLogLinesMatchingRegexp(regex);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        batchJob.processFile(file, baos);
        try {
            baos.flush();
        } catch (IOException e) {
            log.warn("Error when trying to flush batch job output stream", e);
        }
        return Arrays.asList(baos.toString().split("\\n"));
    }
}
