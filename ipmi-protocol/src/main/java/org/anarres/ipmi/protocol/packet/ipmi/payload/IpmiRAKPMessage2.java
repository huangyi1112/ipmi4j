/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.packet.ipmi.payload;

import com.google.common.primitives.UnsignedBytes;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.anarres.ipmi.protocol.client.visitor.IpmiMessageProcessor;
import org.anarres.ipmi.protocol.packet.asf.AsfRsspSessionStatus;
import org.anarres.ipmi.protocol.packet.common.Bits;
import org.anarres.ipmi.protocol.packet.common.Code;
import org.anarres.ipmi.protocol.client.session.IpmiPacketContext;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.client.IpmiEndpoint;

/**
 * [IPMI2] Section 13.21 page 151.
 *
 * @author shevek
 */
public class IpmiRAKPMessage2 extends AbstractTaggedIpmiPayload {

    public AsfRsspSessionStatus statusCode;
    public int consoleSessionId;
    public byte[] systemRandom;   // length = 16
    public UUID systemGuid;
    public byte[] keyExchangeAuthenticationCode;

    @Override
    public IpmiPayloadType getPayloadType() {
        return IpmiPayloadType.RAKPMessage2;
    }

    @Override
    public Class<? extends AbstractTaggedIpmiPayload> getRequestType() {
        return IpmiRAKPMessage1.class;
    }

    @Override
    public Class<? extends AbstractTaggedIpmiPayload> getResponseType() {
        return IpmiRAKPMessage2.class;
    }

    @Override
    public void apply(IpmiMessageProcessor handler, IpmiEndpoint context, IpmiSession session) {
        handler.handleRAKPMessage2(context, session, this);
    }

    @Override
    public int getWireLength(IpmiPacketContext context) {
        return 40 + keyExchangeAuthenticationCode.length;
    }

    @Override
    protected void toWireUnchecked(IpmiPacketContext context, ByteBuffer buffer) {
        buffer.put(messageTag);
        buffer.put(statusCode.getCode());
        buffer.putChar((char) 0);    // reserved
        toWireIntLE(buffer, consoleSessionId);
        buffer.put(systemRandom);
        toWireUUIDLE(buffer, systemGuid);
        buffer.put(keyExchangeAuthenticationCode);
    }

    @Override
    protected void fromWireUnchecked(SocketAddress address, IpmiPacketContext context, ByteBuffer buffer) {
        messageTag = buffer.get();
        statusCode = Code.fromBuffer(AsfRsspSessionStatus.class, buffer);
        assertWireBytesZero(buffer, 2);
        consoleSessionId = fromWireIntLE(buffer);
        systemRandom = readBytes(buffer, 16);
        systemGuid = fromWireUUIDLE(buffer);
        // IpmiSession session = context.getIpmiSession(consoleSessionId);
        // session.getAuthenticationAlgorithm().getHashLength();
        keyExchangeAuthenticationCode = readBytes(buffer, buffer.remaining());
    }

    @Override
    public void toStringBuilder(StringBuilder buf, int depth) {
        appendHeader(buf, depth, getClass().getSimpleName());
        depth++;
        appendValue(buf, depth, "MessageTag", "0x" + UnsignedBytes.toString(messageTag, 16));
        appendValue(buf, depth, "ConsoleSessionId", "0x" + Integer.toHexString(consoleSessionId));
        appendValue(buf, depth, "SystemRandom", toHexString(systemRandom));
        appendValue(buf, depth, "SystemGUID", systemGuid);
        appendValue(buf, depth, "KeyExchangeAuthenticationCode", toHexString(keyExchangeAuthenticationCode));
    }

    /**
     * Create the Little Endian byte array for verification purpose
     *
     * Note: Java ByteBuffer by default is BigEndian ...
     */
    public ByteBuffer getPairedAuthData(IpmiRAKPMessage1 rakp) {
        int length = 58;
        if (rakp.username != null) {
            length += rakp.username.length();
        }

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);   // set to LITTLE ENDIAN

        buffer.putInt(consoleSessionId);
        buffer.putInt(rakp.systemSessionId);
        buffer.put(rakp.consoleRandom);
        buffer.put(systemRandom);
        toWireUUIDLE(buffer, systemGuid);
        buffer.put(Bits.toByte(rakp.requestedMaximumPrivilegeLevel, rakp.privilegeLookupMode));

        if(rakp.username != null) {
            buffer.put((byte)rakp.username.length());
            buffer.put(rakp.username.getBytes(StandardCharsets.UTF_8));
        }
        else {
            buffer.put((byte)0);
        }

        buffer.flip();

        return buffer;
    }

    public ByteBuffer generateKeyExchangeAuthCode(IpmiRAKPMessage1 rakp1) {
        int length = 22;
        if (rakp1.username != null) {
            length += rakp1.username.length();
        }

        ByteBuffer buffer = ByteBuffer.allocate(length);

        // LITTLE ENDIAN!!!
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(systemRandom);
        buffer.putInt(consoleSessionId);
        buffer.put(Bits.toByte(rakp1.requestedMaximumPrivilegeLevel, rakp1.privilegeLookupMode));

        if (rakp1.username != null) {
            buffer.put((byte)rakp1.username.length());
            buffer.put(rakp1.username.getBytes(StandardCharsets.UTF_8));
        } else {
            buffer.put((byte)0);
        }

        buffer.flip();
        return buffer;
    }

    public ByteBuffer prepareSIKData(IpmiRAKPMessage1 rakp1) {
        int length = 34;
        if (rakp1.username != null) {
            length += rakp1.username.length();
        }

        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);  // make LE!!!

        buffer.put(rakp1.consoleRandom);
        buffer.put(systemRandom);
        buffer.put(Bits.toByte(rakp1.requestedMaximumPrivilegeLevel, rakp1.privilegeLookupMode));

        if (rakp1.username != null) {
            buffer.put((byte)rakp1.username.length());
            buffer.put(rakp1.username.getBytes(StandardCharsets.UTF_8));
        } else {
            buffer.put((byte)0);
        }

        buffer.flip();

        return buffer;
    }
}
