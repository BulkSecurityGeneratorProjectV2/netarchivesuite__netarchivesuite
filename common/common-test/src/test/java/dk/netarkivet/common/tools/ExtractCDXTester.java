/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.LogbackRecorder.DenyFilter;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

/**
 * We do not test behaviour on bad ARC files, relying on the unit test of ExtractCDXJob.
 */
public class ExtractCDXTester {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR, TestInfo.WORKING_DIR);

    private LogbackRecorder lr;

    @Before
    public void setUp() {
        lr = LogbackRecorder.startRecorder();
        lr.addFilter(new DenyFilter(), ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }

    @After
    public void tearDown() {
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        lr.clearAllFilters(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        lr.stopRecorder();
    }

    /**
     * Verify that indexing a single ARC file works as expected.
     */
    @Test
    public void testMainOneFile() {
        File arcfile = TestInfo.ARC1;
        ExtractCDX.main(new String[] {arcfile.getAbsolutePath()});
        List<CDXRecord> rList = getRecords();
        assertEquals("Output CDX records should be 1-1 with input ARC file records", 1, rList.size());
        assertMatches(rList, 0, TestInfo.ARC1_URI, TestInfo.ARC1_MIME, TestInfo.ARC1_CONTENT);
    }

    /**
     * Verify that indexing more than one ARC file works as expected.
     */
    @Test
    public void testMainSeveralFiles() {
        ExtractCDX.main(new String[] {TestInfo.ARC1.getAbsolutePath(), TestInfo.ARC2.getAbsolutePath()});
        List<CDXRecord> rList = getRecords();
        assertEquals("Output CDX records should be 1-1 with input ARC file records", 2, rList.size());
        assertMatches(rList, 0, TestInfo.ARC1_URI, TestInfo.ARC1_MIME, TestInfo.ARC1_CONTENT);
        assertMatches(rList, 1, TestInfo.ARC2_URI, TestInfo.ARC2_MIME, TestInfo.ARC2_CONTENT);
    }

    /**
     * Verify that non-ARC files are rejected and execution fails.
     */
    @Test
    public void testMainNonArc() {
        try {
            ExtractCDX.main(new String[] {TestInfo.ARC1.getAbsolutePath(), TestInfo.INDEX_FILE.getAbsolutePath()});
            fail("Calling ExtractCDX with non-arc file should System.exit");
        } catch (SecurityException e) {
            // Expected
            assertEquals("No output should be sent to stdout when ExtraqctCDX fails", "", pss.getOut());
        }
    }

    /**
     * Verifies that calling ExtractCDX without arguments fails.
     */
    @Test
    public void testNoArguments() {
        try {
            ExtractCDX.main(new String[] {});
            fail("Calling ExtractCDX without arguments should System.exit");
        } catch (SecurityException e) {
            // Expected
            assertEquals("No output should be sent to stdout when ExtraqctCDX fails", "", pss.getOut());
        }
    }

    /**
     * Parses output from stdOut as a cdx file.
     *
     * @return All records from the output cdx file, as a List.
     */
    private List<CDXRecord> getRecords() {
        List<CDXRecord> result = new ArrayList<CDXRecord>();
        for (String cdxLine : pss.getOut().split("\n")) {
            result.add(new CDXRecord(cdxLine.split("\\s+")));
        }
        return result;
    }

    /**
     * Asserts that the nth record in the given list has the specified uri and mimetype, and the the length field
     * matches the length of the given content.
     */
    private void assertMatches(List<CDXRecord> rList, int index, String uri, String mime, String content) {
        CDXRecord rec = rList.get(index);
        assertEquals("Output CDX records should be 1-1 with input ARC file records", uri, rec.getURL());
        assertEquals("Output CDX records should be 1-1 with input ARC file records", mime, rec.getMimetype());
        assertEquals("Output CDX records should be 1-1 with input ARC file records", content.length(), rec.getLength());
    }
}
