/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.packet.rmcp;

import javax.annotation.Nonnull;
import org.anarres.ipmi.protocol.client.visitor.RmcpMessageHandler;
import org.anarres.ipmi.protocol.client.IpmiEndpoint;
import org.anarres.ipmi.protocol.packet.common.AbstractPacket;
import org.anarres.ipmi.protocol.packet.common.Wireable;

/**
 * RMCP Data.
 *
 * [ASF2] Section 3.2.2.3 page 22.
 *
 * @author shevek
 */
public interface RmcpData extends Wireable {

    @Nonnull
    public RmcpMessageClass getMessageClass();

    public void apply(@Nonnull RmcpMessageHandler handler, @Nonnull IpmiEndpoint context, AbstractPacket packet);
}
