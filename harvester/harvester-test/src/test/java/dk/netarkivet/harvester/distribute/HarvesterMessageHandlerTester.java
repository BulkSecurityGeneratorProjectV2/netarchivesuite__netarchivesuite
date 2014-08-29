/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobUtils;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.harvesting.metadata.PersistentJobData.HarvestDefinitionInfo;
import dk.netarkivet.harvester.indexserver.distribute.IndexRequestMessage;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import org.mockito.Mockito;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class HarvesterMessageHandlerTester {
    private HarvesterMessageHandler harvesterMessageHandlerUT = new TestMessageHandler();

    @Test
    public final void testOnMessage() throws JMSException {
        ObjectMessage jmsMessage = mock(ObjectMessage.class);
        HarvesterMessage harvesterMessage = mock(HarvesterMessage.class);
        when(jmsMessage.getObject()).thenReturn(harvesterMessage);
        harvesterMessageHandlerUT.onMessage(jmsMessage);
        verify(harvesterMessage).accept(any(HarvesterMessageVisitor.class));
    }

    /*
     * Class under test for void visit(CrawlStatusMessage)
     */
    @Test
    public final void testVisitCrawlStatusMessage() {
        try {
            harvesterMessageHandlerUT.visit(mock(CrawlStatusMessage.class));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
        }
    }

    /*
     * Class under test for void visit(DoOneCrawlMessage)
     */
    @Test
    public final void testVisitDoOneCrawlMessage() {
        try {
            harvesterMessageHandlerUT.visit(mock(DoOneCrawlMessage.class));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
        }
    }

    /*
     * Class under test for void visit(IndexRequestMessage)
     */
    @Test
    public final void testVisitIndexRequestMessage() {
        try {
            harvesterMessageHandlerUT.visit(mock(IndexRequestMessage.class));
            fail("Should have thrown a permission denied.");
        } catch (PermissionDenied e) {
        }
    }

    private static class TestMessageHandler extends HarvesterMessageHandler {
        public TestMessageHandler() {
        }
    }
}
