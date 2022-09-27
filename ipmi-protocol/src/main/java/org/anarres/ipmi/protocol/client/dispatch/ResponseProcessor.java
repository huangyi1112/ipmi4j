/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.dispatch;

import javax.annotation.Nonnull;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.client.IpmiEndpoint;
import org.anarres.ipmi.protocol.packet.asf.AbstractAsfData;
import org.anarres.ipmi.protocol.packet.common.Packet;
import org.anarres.ipmi.protocol.packet.ipmi.AbstractIpmiSessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpData;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpMessageClass;

import java.util.concurrent.TimeUnit;

/**
 * Receive RMCP response data.
 *
 * IPMI messages are wrapped in RMCP, so when processing we need to recover the IPMI payload out...
 */
public interface ResponseProcessor {
    public default void handleIpmiPayload(@Nonnull IpmiEndpoint context, IpmiSession session, @Nonnull IpmiPayload response, int seqOrTag) {
        // TODO: log
    }

    public default void handleRmcpData(@Nonnull IpmiEndpoint context, @Nonnull RmcpData response, int seq) {
        // TODO: log
    }

    /**
     * Timeout response
     */
    public void timeout(@Nonnull RequestKey key);
}
