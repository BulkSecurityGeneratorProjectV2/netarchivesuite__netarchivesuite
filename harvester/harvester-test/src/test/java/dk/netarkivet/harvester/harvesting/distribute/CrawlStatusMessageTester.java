
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.IOException;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.testutils.Serial;

/**
 * Unit tests for CrawlStatusMessage.
 */
public class CrawlStatusMessageTester extends TestCase {
    
    /**
     * Test that we can call the constructor for finished jobs
     */
    public void testJobFinishedCTOR() {
        CrawlStatusMessage csm;
        csm = new CrawlStatusMessage(
                12l, JobStatus.DONE, null);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.DONE,
                csm.getStatusCode());
        csm = new CrawlStatusMessage(
                12l, JobStatus.FAILED, null);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.FAILED,
                csm.getStatusCode());
    }

    /**
     * CTOR should fail if not given a null JobStatus, or a negative JobId
     */
    public void testJobFinishedCTORFails() {
        try {
            new CrawlStatusMessage(
                    12L, null, null);
            fail("CTOR with RemoteFile should fail if JobStatus is null") ;
        } catch (ArgumentNotValid e ) {
            //expected
        }
        try {
            new CrawlStatusMessage(
                    -1L, JobStatus.NEW, null);
            fail("CTOR with RemoteFile should fail if jobid < 0");
        } catch (ArgumentNotValid e ) {
            //expected
        }
    }

    /**
     * Test that we can call the constructor for unfinished jobs
     */
    public void testJobNotFinishedCTOR() {
        CrawlStatusMessage csm;
        csm = new CrawlStatusMessage(
                12l, JobStatus.NEW);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.NEW,
                csm.getStatusCode());
        csm = new CrawlStatusMessage(
                12l, JobStatus.STARTED);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.STARTED,
                csm.getStatusCode());
        csm = new CrawlStatusMessage(
                12l, JobStatus.SUBMITTED);
        assertEquals("Don't get back the id we entered", 12l, csm.getJobID());
        assertEquals("Don't get back the status we entered", JobStatus.SUBMITTED,
                csm.getStatusCode());
    }

    public void testJobNotFinishedCTORFails() {
        try {
            new CrawlStatusMessage(
                    -1L, JobStatus.DONE);
            fail("CTOR without RemoteFile should fail if jobid is negative") ;
        } catch (ArgumentNotValid e ) {
            //expected
        }
        try {
            new CrawlStatusMessage(
                    12l, null);
            fail("CTOR without RemoteFile should fail if JobStatus is null") ;
        } catch (ArgumentNotValid e ) {
            //expected
        }

    }

    /**
     * Test that class is serializable
     */
    public void testSerializable() throws IOException, ClassNotFoundException {
        CrawlStatusMessage csm = new CrawlStatusMessage(
                0l, JobStatus.DONE, null);
        CrawlStatusMessage csm2 = (CrawlStatusMessage) Serial.serial(csm);
        assertEquals("Deserialization error for CrawlStatusMessage",
                relevantState(csm), relevantState(csm2));
    }

    /**
     * Returns a string representation of the information to be serialized in
     * a CrawlStatusMessage
     * @param csm the CrawlstatusMessage
     * @return  the string representation
     */
    public String relevantState(CrawlStatusMessage csm){
        return "" + csm.getJobID() + " " + csm.getStatusCode();
    }

}
