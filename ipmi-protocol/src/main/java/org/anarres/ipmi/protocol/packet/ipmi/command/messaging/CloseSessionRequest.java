/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.packet.ipmi.command.messaging;

import com.google.common.primitives.UnsignedBytes;
import java.nio.ByteBuffer;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiCommandName;
import org.anarres.ipmi.protocol.packet.ipmi.command.AbstractIpmiSessionRequest;

/**
 * [IPMI2] Section 22.19, table 22-24, page 297.
 *
 * @author shevek
 */
public class CloseSessionRequest extends AbstractIpmiSessionRequest {

    public int sessionId;
    public byte sessionHandle;

    @Override
    public IpmiCommandName getCommandName() {
        return IpmiCommandName.CloseSession;
    }

    @Override
    public int getDataWireLength() {
        return (sessionId == 0) ? 5 : 4;
    }

    @Override
    protected void toWireData(ByteBuffer buffer) {
        toWireIntLE(buffer, sessionId);
        if (sessionId == 0)
            buffer.put(sessionHandle);
    }

    @Override
    protected void fromWireData(ByteBuffer buffer) {
        sessionId = fromWireIntLE(buffer);
        if (sessionId == 0)
            sessionHandle = buffer.get();
    }

    @Override
    public void toStringBuilder(StringBuilder buf, int depth) {
        super.toStringBuilder(buf, depth);
        appendValue(buf, depth, "SessionId", "0x" + Integer.toHexString(sessionId));
        appendValue(buf, depth, "SessionHandle", "0x" + UnsignedBytes.toString(sessionHandle, 16));
    }
}