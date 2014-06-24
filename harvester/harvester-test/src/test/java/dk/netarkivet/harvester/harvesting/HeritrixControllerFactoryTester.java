
package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.controller.BnfHeritrixController;
import dk.netarkivet.harvester.harvesting.controller.HeritrixController;
import dk.netarkivet.harvester.harvesting.controller.HeritrixControllerFactory;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;


/**
 * Unittest for the HeritrixControllerFactory class.
 */
@SuppressWarnings({ "unused"})
public class HeritrixControllerFactoryTester extends TestCase {

    private MoveTestFiles mtf;
    private File dummyLuceneIndex;
    private String defaultController;

    public HeritrixControllerFactoryTester() {
        mtf = new MoveTestFiles(
                TestInfo.CRAWLDIR_ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }

    public void setUp() throws IOException {
        mtf.setUp();
        dummyLuceneIndex = mtf.newTmpDir();
        // Out commented to avoid reference to archive module from harvester module.
        // LuceneUtils.makeDummyIndex(dummyLuceneIndex);
        defaultController = Settings.get(
                HarvesterSettings.HERITRIX_CONTROLLER_CLASS);
    }

    public void tearDown() {
        mtf.tearDown();
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, defaultController);
    }

    /**
     * Test that we can construct a HeritrixController using the
     * setting in settings.xml
     */
    public void testGetDefaultHeritrixControllerDefaultSettings() {
         File origSeeds = TestInfo.SEEDS_FILE;
        File crawlDir = TestInfo.HERITRIX_TEMP_DIR;
        crawlDir.mkdirs();
        File orderXml = new File(crawlDir, "order.xml");
        File seedsTxt = new File(crawlDir, "seeds.txt");
        FileUtils.copyFile(TestInfo.ORDER_FILE, orderXml);
        FileUtils.copyFile(origSeeds, seedsTxt);
        HeritrixFiles files = new HeritrixFiles(
                crawlDir,
                new JobInfoTestImpl(Long.parseLong(TestInfo.ARC_JOB_ID),
                Long.parseLong(TestInfo.ARC_HARVEST_ID)));
        HeritrixController hc
            = HeritrixControllerFactory.getDefaultHeritrixController(files);
        assertTrue("Should have got a JMXHeritricController, not " + hc,
                hc instanceof BnfHeritrixController);
    }

    /**
     * Test that we can change the implementation of HeritrixController to
     * construct
     */
    public void testGetDefaultHeritrixControllerChangeDefaultSettigs() {
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.HeritrixControllerFactoryTester$DummyHeritrixController");
        HeritrixController hc = HeritrixControllerFactory.getDefaultHeritrixController("hello world");
        assertTrue("Should have got a DummyHeritrixController, not " + hc, hc instanceof DummyHeritrixController);

    }

    /**
     * Test we throw the expected exception when the signature is invalid
     */
    public void testGetDefaultHeritrixControllerWrongSignature() {
        Settings.set(HarvesterSettings.HERITRIX_CONTROLLER_CLASS, 
                "dk.netarkivet.harvester.harvesting.HeritrixControllerFactoryTester$DummyHeritrixController");
        try {
            HeritrixController hc
                = HeritrixControllerFactory.getDefaultHeritrixController(
                        "hello world", "hello world");
            fail("Expected to throw ArgumentNotValid on invlaid signature");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }
    
    public static class DummyHeritrixController implements HeritrixController {

        public DummyHeritrixController(String dummyArg) {
            System.out.println(DummyHeritrixController.class);
            //Just a dummy constructor for testing
        }

        public void initialize() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public void requestCrawlStart() throws IOFailure {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public void beginCrawlStop() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public void requestCrawlStop(String reason) {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean atFinish() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean crawlIsEnded() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public int getActiveToeCount() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public long getQueuedUriCount() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public int getCurrentProcessedKBPerSec() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getProgressStats() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public boolean isPaused() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public void cleanup() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }

        public String getHarvestInformation() {
            //TODO: implement method
            throw new RuntimeException("Not implemented");
        }
    }

}
