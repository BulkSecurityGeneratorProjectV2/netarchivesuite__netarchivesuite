package dk.netarkivet.archive.arcrepository;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.testutils.preconfigured.TestConfigurationIF;

public class MockupArcRepositoryClient implements TestConfigurationIF, MessageListener {
    /** Fail on all attempts to store these files */
    private List<String> failOnFiles;
    private int msgCount;
    private List<StoreMessage> storeMsgs;

    public void setUp() {
        failOnFiles = new ArrayList<String>();
        msgCount = 0;
        storeMsgs = new ArrayList<StoreMessage>();
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), this);
    }

    public void tearDown() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), this);
    }

    public void failOnFile(String file) {
        failOnFiles.add(file);
    }

    public void onMessage(Message message) {
        msgCount++;
        StoreMessage sm = (StoreMessage) JMSConnection.unpack(message);
        storeMsgs.add(sm);
        if(failOnFiles.contains(sm.getArcfileName())) {
            sm.setNotOk("Simulating store failed.");
        }
        JMSConnectionFactory.getInstance().resend(sm, sm.getReplyTo());
    }

    public int getMsgCount() {
        return msgCount;
    }

    public List<StoreMessage> getStoreMsgs() {
        return storeMsgs;
    }
}

