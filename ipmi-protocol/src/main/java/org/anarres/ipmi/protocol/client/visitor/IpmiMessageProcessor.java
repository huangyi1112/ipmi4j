/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.visitor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.anarres.ipmi.protocol.client.IpmiEndpoint;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiCommand;
import org.anarres.ipmi.protocol.packet.ipmi.payload.AbstractTaggedIpmiPayload;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiOpenSessionRequest;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiOpenSessionResponse;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiRAKPMessage1;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiRAKPMessage2;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiRAKPMessage3;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiRAKPMessage4;
import org.anarres.ipmi.protocol.packet.ipmi.payload.OemExplicit;
import org.anarres.ipmi.protocol.packet.ipmi.payload.SOLMessage;

/**
 *
 * @author shevek
 */
public interface IpmiMessageProcessor {
    default void handleDefault(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiPayload payload, int tagOrSeq) {
    }

    default void handleTagged(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull AbstractTaggedIpmiPayload message) {
        handleDefault(context, session, message, message.getMessageTag());
    }

    default public void handleOpenSessionRequest(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiOpenSessionRequest message) {
        handleTagged(context, session, message);
    }

    default public void handleOpenSessionResponse(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiOpenSessionResponse message) {
        handleTagged(context, session, message);
    }

    default public void handleRAKPMessage1(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiRAKPMessage1 message) {
        handleTagged(context, session, message);
    }

    default public void handleRAKPMessage2(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiRAKPMessage2 message) {
        handleTagged(context, session, message);
    }

    default public void handleRAKPMessage3(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiRAKPMessage3 message) {
        handleTagged(context, session, message);
    }

    default public void handleRAKPMessage4(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiRAKPMessage4 message) {
        handleTagged(context, session, message);
    }

    default public void handleCommand(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull IpmiCommand message) {
        handleDefault(context, session, message, message.getSequenceNumber());
    }

    default public void handleSOL(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull SOLMessage message) {
        // todo: how to set SOLMessage seq?
        handleDefault(context, session, message, 0);
    }

    default public void handleOemExplicit(@Nonnull IpmiEndpoint context, @CheckForNull IpmiSession session, @Nonnull OemExplicit message) {
        // todo: how to set seqOrTag
        handleDefault(context, session, message, 0);
    }

    // /* Does not distinguish between {@link IpmiPayloadType#OEM0} through {@link IpmiPayloadType#OEM7}. */
    // public void handleOEM(@Nonnull IpmiHandlerContext context, @CheckForNull IpmiSession session, @Nonnull OEMMessage message)
}
