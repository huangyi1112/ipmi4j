/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.visitor;

import javax.annotation.Nonnull;

import org.anarres.ipmi.protocol.client.IpmiEndpoint;
import org.anarres.ipmi.protocol.packet.asf.AsfRmcpData;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiSessionWrapper;
import org.anarres.ipmi.protocol.packet.rmcp.OEMRmcpMessage;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpData;

/**
 * Handle message at RMCP level. IPMI messages are wrapped in a RMCP message and shall be handled by a
 * IPMI message handler
 *
 * @author shevek
 */
public interface RmcpMessageHandler {
    default public void handleRmcpData(@Nonnull IpmiEndpoint context, @Nonnull RmcpData message, int seq) {
    }

    default public void handleAsfRmcpData(@Nonnull IpmiEndpoint context, @Nonnull AsfRmcpData message, int seq) {
        handleRmcpData(context, message, seq);
    }

    default public void handleIpmiRmcpData(@Nonnull IpmiEndpoint context, @Nonnull IpmiSessionWrapper message, int seq) {
        handleRmcpData(context, message, seq);
    }

    default public void handleOemRmcpData(@Nonnull IpmiEndpoint context, @Nonnull OEMRmcpMessage message, int seq) {
        handleRmcpData(context, message, seq);
    }
}
