/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.*;

import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.client.visitor.IpmiMessageProcessor;
import org.anarres.ipmi.protocol.client.visitor.RmcpMessageHandler;
import org.anarres.ipmi.protocol.packet.asf.AbstractAsfData;
import org.anarres.ipmi.protocol.packet.asf.AsfRmcpData;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiSessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiCommand;
import org.anarres.ipmi.protocol.packet.ipmi.payload.AbstractTaggedIpmiPayload;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;
import org.anarres.ipmi.protocol.packet.common.Packet;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * IpmiClient talks with IpmiEndpoint using the IPMI protocol to send / receive packets.
 * It could also help the manage the IPMI sessions to the endpoints (for authentication purpose, etc.)
 *
 * @author shevek
 */
public abstract class IpmiClient {
    private static final Logger LOG = LoggerFactory.getLogger(IpmiClient.class);
    private final Map<SocketAddress, IpmiEndpoint> endpoints = new ConcurrentHashMap<>();

    /**
     * Layered processing. When receiving a packet, we ask Rmcp handler to process first. If the received
     * message is a IpmiPayload, then delegate to IPMI handler
     */
    /* First, for RMCP-land. */
    private final RmcpMessageHandler rmcpHandler = new RmcpMessageHandler() {
        /* And now for IPMI-land. */
        private final IpmiMessageProcessor ipmiPayloadHandler = new IpmiMessageProcessor() {
            @Override
            public void handleDefault(IpmiEndpoint context, IpmiSession session, IpmiPayload payload, int seqOrTag) {
                context.handleIpmiPayload(context, session, payload, seqOrTag);
            }

            @Override
            public void handleTagged(@Nonnull IpmiEndpoint context, @Nonnull IpmiSession session, @Nonnull AbstractTaggedIpmiPayload payload) {
                context.handleIpmiPayload(context, session, payload, payload.getMessageTag());
            }

            /** Section 6.12.8, page 58: Requests and responses are matched on the IPMI Seq field. */
            @Override
            public void handleCommand(IpmiEndpoint context, IpmiSession session, IpmiCommand message) {
                context.handleIpmiPayload(context, session, message, message.getSequenceNumber());
            }
        };

        @Override
        public void handleRmcpData(IpmiEndpoint context, RmcpData message, int seq) {
            context.handleRmcpData(context, message, seq);
        }

        @Override
        public void handleIpmiRmcpData(IpmiEndpoint context, IpmiSessionWrapper message, int rmcpSeq) {
            // Pass directly to IpmiPayload to avoid type ambiguity on 'this'.
            int sessionId = message.getIpmiSessionId();
            IpmiSession session = (sessionId == 0) ? null : context.getSession(sessionId);
            message.getIpmiPayload().apply(ipmiPayloadHandler, context, session);
        }
    };

    //
    // start the client. Used to do whatever initialization required
    public abstract void start() throws IOException, InterruptedException;

    //
    // send a packet. sub-class shall take care of the context management
    public abstract void send(@Nonnull Packet packet);

    //
    // stop the client and release all resources associated with the client
    public abstract void stop() throws IOException, InterruptedException;

    /**
     * Get an IpmiEndpoint for given address
     * @param addr
     * @return
     */
    public IpmiEndpoint getEndpoint(SocketAddress addr) {
        return endpoints.computeIfAbsent(addr, __ -> new IpmiEndpoint(this, addr));
    }

    @VisibleForTesting
    public void receive(Packet packet) throws IOException {
        // LOG.info("Receive\n" + packet);
        InetSocketAddress addr = (InetSocketAddress)packet.getRemoteAddress();
        IpmiEndpoint endpoint = endpoints.get(addr);
        if(endpoint == null) {
            LOG.error("Packet from unknown endpoint ignored - " + addr);
            return;
        }

        // dispatch to given endpoint to process the packet
        packet.apply(rmcpHandler, endpoint);
    }
}
