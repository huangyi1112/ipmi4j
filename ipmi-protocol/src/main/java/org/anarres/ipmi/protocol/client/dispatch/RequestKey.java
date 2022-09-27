/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.dispatch;

import com.google.common.base.MoreObjects;
import java.net.SocketAddress;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.anarres.ipmi.protocol.packet.ipmi.command.AbstractIpmiCommand;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiCommand;
import org.anarres.ipmi.protocol.packet.ipmi.payload.AbstractTaggedIpmiPayload;

/**
 * A unique key that can identify a request in a session
 */
public class RequestKey {
    private final SocketAddress remoteAddress;
    private final int rmcpSeqId;
    private final int messageId;   // for IPMI, it could either be tag (for non-session message) or seq Id (for messages in session)

    /**
     * Constructs a new IpmiReceiverKey.
     *
     * @param remoteAddress The remote system address sending the response.
     * @param messageId The {@link AbstractTaggedIpmiPayload#getMessageTag() message tag} or {@link AbstractIpmiCommand#getSequenceNumber()}
     * or {@link IpmiCommand#getSequenceNumber() sequence number} to match.
     */
    public RequestKey(@Nonnull SocketAddress remoteAddress, int rmcpSeqId, @Nonnegative int messageId) {
        this.remoteAddress = remoteAddress;
        this.rmcpSeqId = rmcpSeqId;
        this.messageId = messageId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remoteAddress, rmcpSeqId, messageId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (null == obj)
            return false;
        if (!getClass().equals(obj.getClass()))
            return false;
        RequestKey o = (RequestKey) obj;
        return Objects.equals(remoteAddress, o.remoteAddress)
                && rmcpSeqId == o.rmcpSeqId
                && messageId == o.messageId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("remoteAddress", remoteAddress)
                .add("rmcpSeqId", rmcpSeqId)
                .add("messageId", messageId)
                .toString();
    }

}
