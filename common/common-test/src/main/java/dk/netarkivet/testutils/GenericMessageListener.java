/*
 * #%L
 * Netarchivesuite - common - test
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

package dk.netarkivet.testutils;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;

/**
 * A bare bones MessageListener used for unit testing.
 */
public class GenericMessageListener implements MessageListener {

    /** An ordered list of all messages received by this listener */
    public List<NetarkivetMessage> messagesReceived = new ArrayList<NetarkivetMessage>();

    /**
     *
     * @param message
     */
    public void onMessage(Message message) {
        NetarkivetMessage naMsg = JMSConnection.unpack(message);
        messagesReceived.add(naMsg);
    }
}
