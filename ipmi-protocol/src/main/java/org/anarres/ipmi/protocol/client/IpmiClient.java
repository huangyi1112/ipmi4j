/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client;

import org.anarres.ipmi.protocol.client.dispatch.IpmiPayloadTransmitQueue;
import org.anarres.ipmi.protocol.client.session.IpmiSessionManager;
import org.anarres.ipmi.protocol.client.visitor.IpmiHandlerContext;

import java.io.IOException;

/**
 *
 * @author shevek
 */
public interface IpmiClient extends IpmiHandlerContext.IpmiPacketQueue, IpmiPayloadTransmitQueue.IpmiPacketSender {
    public void start() throws IOException, InterruptedException;

    public void stop() throws IOException, InterruptedException;

    public IpmiSessionManager getSessionManager();
}
