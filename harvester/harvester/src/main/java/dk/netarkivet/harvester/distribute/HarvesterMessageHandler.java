
package dk.netarkivet.harvester.distribute;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.distribute.FrontierReportMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationResponse;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobEndedMessage;
import dk.netarkivet.harvester.indexserver.distribute.IndexRequestMessage;

/**
 * This default message handler shields of all unimplemented methods from the
 * HarvesterMessageVisitor interface.
 *
 * Classes should not implement HarvesterMessageVisitor but extend this class.
 *
 * @see HarvesterMessageVisitor
 *
 */
public abstract class HarvesterMessageHandler
        implements HarvesterMessageVisitor, MessageListener {

    private final Log log = LogFactory.getLog(getClass().getName());

    /**
     * Creates a HarvesterMessageHandler object.
     */
    public HarvesterMessageHandler() {
    }

    /**
     * Unpacks and calls accept() on the message object.
     *
     * This method catches <b>all</b> exceptions and logs them.
     *
     * @param msg an ObjectMessage
     */
    @Override
    public void onMessage(Message msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        log.trace("Message received:\n" + msg.toString());
        try {
            NetarkivetMessage unpackedMsg = JMSConnection.unpack(msg);
            ((HarvesterMessage)unpackedMsg).accept(this);
        } catch (Throwable e) {
            log.warn("Error processing message '" + msg + "'", e);
        }
    }

    /** Handles when a handler receives a message it is not prepare to handle.
     *
     * @param msg The received message.
     * @throws PermissionDenied Always
     */
    private void deny(HarvesterMessage msg) {
        throw new PermissionDenied("'" + this + "' provides no handling for "
                + msg + " of type " + msg.getClass().getName()
                + " and should not be invoked!");
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a CrawlStatusMessage
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(CrawlStatusMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a DoOneCrawlMessage
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(DoOneCrawlMessage msg) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link CrawlProgressMessage}
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(CrawlProgressMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link FrontierReportMessage}
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(FrontierReportMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link JobEndedMessage}
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(JobEndedMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link HarvesterReadyMessage}
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(HarvesterReadyMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link IndexReadyMessage}
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(IndexReadyMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link IndexRequestMessage}
     * @throws PermissionDenied when invoked
     */
    @Override
    public void visit(IndexRequestMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link HarvesterRegistrationRequest}
     */
    @Override
    public void visit(HarvesterRegistrationRequest msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

    /**
     * This method should be overridden and implemented by a sub class if
     * message handling is wanted.
     * @param msg a {@link HarvesterRegistrationResponse}
     */
    @Override
    public void visit(HarvesterRegistrationResponse msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");
        deny(msg);
    }

}
