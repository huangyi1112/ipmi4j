/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.packet.rmcp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import org.anarres.ipmi.protocol.client.visitor.RmcpMessageHandler;
import org.anarres.ipmi.protocol.packet.common.AbstractPacket;
import org.anarres.ipmi.protocol.packet.common.AbstractWireable;
import org.anarres.ipmi.protocol.client.session.IpmiPacketContext;
import org.anarres.ipmi.protocol.client.IpmiEndpoint;

/**
 *
 * @author shevek
 */
public class OEMRmcpMessage extends AbstractWireable implements RmcpData {

    private byte[] data;

    @Override
    public RmcpMessageClass getMessageClass() {
        return RmcpMessageClass.OEM;
    }

    @Override
    public void apply(RmcpMessageHandler handler, IpmiEndpoint context, AbstractPacket packet) {
        handler.handleOemRmcpData(context, this, packet.getSequenceNumber());
    }

    @Override
    public int getWireLength(IpmiPacketContext context) {
        return data.length;
    }

    @Override
    protected void toWireUnchecked(IpmiPacketContext context, ByteBuffer buffer) {
        buffer.put(data);
    }

    @Override
    protected void fromWireUnchecked(SocketAddress address, IpmiPacketContext context, ByteBuffer buffer) {
        data = readBytes(buffer, buffer.remaining());
    }
}
