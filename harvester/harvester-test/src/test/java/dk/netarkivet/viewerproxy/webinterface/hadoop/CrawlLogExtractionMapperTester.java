package dk.netarkivet.viewerproxy.webinterface.hadoop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.hadoop.HadoopJobTool;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.viewerproxy.webinterface.TestInfo;

public class CrawlLogExtractionMapperTester {
    private static final File WORKING_DIR = new File(TestInfo.DATA_DIR, "working");
    private final File WARC_FILE = new File(WORKING_DIR, "2-metadata-1.warc");
    private final File ARC_FILE = new File(WORKING_DIR, "2-metadata-1.arc");
    private static MoveTestFiles mtf;
    private static MiniDFSCluster hdfsCluster;
    private static Configuration conf;
    private static MiniYARNCluster miniYarnCluster;
    private static DistributedFileSystem fileSystem;

    @BeforeClass
    public static void setUp() throws IOException {
        setupTestFiles();
        initCluster();
        // System.out.println("YARN started");
    }

    public static void setupTestFiles() {
        mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, WORKING_DIR);
        mtf.setUp();
        // There is probably a better solution, but would need 2 working dirs if using MoveTestFiles since it deletes working dir on setUp()
        for (File file : TestInfo.WARC_ORIGINALS_DIR.listFiles()) {
            FileUtils.copyFile(file, new File(WORKING_DIR, file.getName()));
        }
    }

    // TODO Should be handled through some Hadoop test helper class
    private static void initCluster() throws IOException {
        File baseDir = Files.createTempDirectory("test_hdfs").toFile().getAbsoluteFile();
        conf = new YarnConfiguration();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, baseDir.getAbsolutePath());
        MiniDFSCluster.Builder builder = new MiniDFSCluster.Builder(conf);
        hdfsCluster = builder.build();

        fileSystem = hdfsCluster.getFileSystem();
        // System.out.println("HDFS started");

        conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 64);
        conf.setClass(YarnConfiguration.RM_SCHEDULER,
                FifoScheduler.class, ResourceScheduler.class);
        miniYarnCluster = new MiniYARNCluster("name", 1, 1, 1);
        miniYarnCluster.init(conf);
        miniYarnCluster.start();
    }

    @Test
    public void testGetCrawlLogWARCMetadataFile() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + WARC_FILE.getAbsolutePath());
        jobInputFile.deleteOnExit();
        String regex = ".*(https?:\\/\\/(www\\.)?|dns:|ftp:\\/\\/)([\\w_-]+\\.)?([\\w_-]+\\.)?([\\w_-]+\\.)?"
                + "netarkivet.dk" +  "($|\\/|\\w|\\s).*";
        conf.set("regex", regex);


        // Start the job
        try {
            Tool job = new HadoopJobTool(conf, new CrawlLogExtractionMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> crawlLogLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            final String uuid = UUID.randomUUID().toString();
            File tempFile = createTempResultFile(uuid);
            File sortedTempFile = createTempResultFile(uuid + "-sorted");
            FileUtils.writeCollectionToFile(tempFile, crawlLogLines);
            FileUtils.sortCrawlLogOnTimestamp(tempFile, sortedTempFile);
            FileUtils.remove(tempFile);
            List<String> lines = FileUtils.readListFromFile(sortedTempFile);
            //lines.forEach(System.out::println);
            assertTrue("Should have found a result, but found none", lines.size() > 0);
            StringAsserts.assertStringContains("First line should be dns", "dns:", lines.get(0));

            StringAsserts.assertStringContains("Last line should be netarkivet.dk", "netarkivet.dk",
                    lines.get(lines.size() - 1));
            assertEquals("Should have 161 lines", 161, lines.size());
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    @Test
    public void testGetCrawlLogARCMetadataFile() throws Exception {
        String outputURI = "hdfs://localhost:" + hdfsCluster.getNameNodePort() + "/" + UUID.randomUUID().toString();
        // Write the input lines to the the input file
        File jobInputFile = File.createTempFile("tmp", UUID.randomUUID().toString());
        org.apache.commons.io.FileUtils.writeStringToFile(jobInputFile, "file://" + ARC_FILE.getAbsolutePath());
        jobInputFile.deleteOnExit();
        String regex = ".*(https?:\\/\\/(www\\.)?|dns:|ftp:\\/\\/)([\\w_-]+\\.)?([\\w_-]+\\.)?([\\w_-]+\\.)?"
                + "netarkivet.dk" + "($|\\/|\\w|\\s).*";
        conf.set("regex", regex);

        // Start the job
        try {
            Tool job = new HadoopJobTool(conf, new CrawlLogExtractionMapper());
            int exitCode = ToolRunner.run(conf, job,
                    new String[] {"file://" + jobInputFile.toString(), outputURI});
            Assert.assertEquals(0, exitCode); // job success

            List<String> crawlLogLines = HadoopJobUtils.collectOutputLines(fileSystem, new Path(outputURI));
            final String uuid = UUID.randomUUID().toString();
            File tempFile = createTempResultFile(uuid);
            File sortedTempFile = createTempResultFile(uuid + "-sorted");
            FileUtils.writeCollectionToFile(tempFile, crawlLogLines);
            FileUtils.sortCrawlLogOnTimestamp(tempFile, sortedTempFile);
            FileUtils.remove(tempFile);
            List<String> lines = FileUtils.readListFromFile(sortedTempFile);
            //lines.forEach(System.out::println);
            assertTrue("Should have found a result, but found none", lines.size() > 0);
            StringAsserts.assertStringContains("First line should be dns", "dns:", lines.get(0));
            assertEquals("Should have 126 lines (2 dns, 1 netarchive.dk, 121 netarkivet.dk, "
                            + "and 2 www.netarkivet.dk)", 126, lines.size());
        } finally {
            fileSystem.delete(new Path(outputURI), true);
        }
    }

    /**
     * Helper method to create temp file for storage of result
     *
     * @param uuidSuffix Suffix of temp file.
     * @return a new temp File.
     */
    private static File createTempResultFile(String uuidSuffix) {
        File tempFile;
        try {
            tempFile = File.createTempFile("temp", uuidSuffix + ".txt");
            tempFile.deleteOnExit();
        } catch (IOException e) {
            throw new IOFailure("Unable to create temporary file", e);
        }
        return tempFile;
    }

    @AfterClass
    public static void tearDown() throws IOException {
        miniYarnCluster.stop();
        hdfsCluster.shutdown(true);
        fileSystem.close();
        mtf.tearDown();
    }
}