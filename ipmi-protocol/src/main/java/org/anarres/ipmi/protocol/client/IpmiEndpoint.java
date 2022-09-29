/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

import org.anarres.ipmi.protocol.IpmiUtils;
import org.anarres.ipmi.protocol.client.dispatch.RequestKey;
import org.anarres.ipmi.protocol.client.dispatch.ResponseProcessor;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.packet.asf.AbstractAsfData;
import org.anarres.ipmi.protocol.packet.asf.AsfPresencePingData;
import org.anarres.ipmi.protocol.packet.asf.AsfPresencePongData;
import org.anarres.ipmi.protocol.packet.ipmi.*;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiResponse;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesResponse;
import org.anarres.ipmi.protocol.packet.ipmi.payload.AbstractTaggedIpmiPayload;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;
import org.anarres.ipmi.protocol.packet.ipmi.security.CipherSuite;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpData;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * An IPMI endpoint, represents a IPMI target to talk with. On the endpoint, we could have one or more
 * IPMI sessions.
 *
 * We can send to / receive from following data from the target:
 *   IPMI command / request, which when sending on the link, will be wrapped as RcmpData
 *   Rmcp command / request
 */
public class IpmiEndpoint implements ResponseProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(IpmiEndpoint.class);

    private final IpmiClient client;
    private final SocketAddress systemAddress;
    private final RequestQueue queue = new RequestQueue();

    // allocated consoles
    private final Set<Integer> consoles = new HashSet<>();

    // active sessions, i.e. sessions successfully opened
    private final ConcurrentMap<Integer /*server systemSessionId */, IpmiSession> serverSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer /*client consoleSessionId */, IpmiSession> clientSessions = new ConcurrentHashMap<>();

    // pendign reuqests, timeout handling!
    private final ConcurrentMap<RequestKey, RequestContext> pending = new ConcurrentHashMap<>();

    private boolean supportIPMIv2 = false;
    private final List<CipherSuite> cipherSuits = new ArrayList<>();

    public IpmiEndpoint(@Nonnull IpmiClient client, @Nonnull SocketAddress systemAddress) {
        this.client = client;
        this.systemAddress = systemAddress;
    }

    public boolean isIPMIv2Supported() {
        return supportIPMIv2;
    }

    public List<CipherSuite> getCipherSuits() {
        return cipherSuits;
    }

    /**
     * To make our session more recognizable, we create session by fixing higher 16 bits as FEFC,
     * with lower 16 bits randomly generated
     * @return
     */
    @Nonnull
    public IpmiSession newSession(String username, String password) throws Exception {
        int id = IpmiUtils.RANDOM.nextInt();
        int consoleSessionId = 0xFEFC0000 | (id & 0xFFFF);

        synchronized (consoles) {
            while(consoles.contains(consoleSessionId)) {
                id = IpmiUtils.RANDOM.nextInt();
                consoleSessionId = 0xFEFC0000 | (id & 0xFFFF);
            }

            consoles.add(consoleSessionId);
        }

        LOG.info(String.format("Allocate session with console id %#08x",  consoleSessionId));

        IpmiSession session = new IpmiSession(this, consoleSessionId);
        if(!session.open("ADMIN", "ADMIN")) {
            System.out.println("Cannot create session");
        }

        LOG.info(String.format("Session with console id %#08x opened with session id %#08x",  consoleSessionId, session.getSystemSessionId()));
        serverSessions.put(session.getSystemSessionId(), session);
        clientSessions.put(consoleSessionId, session);

        return session;
    }

    public IpmiSession getSessionById(int sessionId, boolean isRequest) {
        if (sessionId == 0) {
            return null;
        }

        if(isRequest) {
            return serverSessions.get(sessionId);
        }
        else {
            return clientSessions.get(sessionId);
        }
    }

    @Nonnull
    public SocketAddress getSystemAddress() {
        return systemAddress;
    }

    /**
     * Send RMCP packet.
     *    Note: not use for now. Where shall we use RMCP???
     *
     * @param data: RMCP request payload
     * @param expectResponse - if the RMCP requests a response? According to IPMI v2, if the request
     *                       not expect a response, we shall set the seq # to 0xFF (also used for
     *                       IPMI payload over RMCP)
     * @param <T>
     * @return
     */
    public <T extends RmcpData> Future<T> sendRmcpRequest(RmcpData data, boolean expectResponse) {
        RequestContext context = null;
        byte nextSeq = (byte) 0xff;
        if(expectResponse) {
            nextSeq = queue.getNextRmcpSeq();
            context = new RequestContext();
            SocketAddress addr = this.getSystemAddress();
            RequestKey key = new RequestKey(addr, nextSeq, 0);
            pending.put(key, context);
            LOG.info("Queue RMCP request with key: " + key);
        }

        RmcpPacket packet = new RmcpPacket();
        packet.withRemoteAddress(systemAddress);
        packet.withData(data);
        packet.withSequenceNumber(nextSeq);

        client.send(packet);

        return context == null ? null : new RequestFuture(context);
    }

    public <T extends AbstractAsfData> Future<T> sendAsfRequest(AbstractAsfData message) {
        byte nextMessageTag = queue.getNextAsfTag();

        RequestContext context = new RequestContext();
        SocketAddress addr = this.getSystemAddress();
        RequestKey key = new RequestKey(addr, 0, nextMessageTag);
        pending.put(key, context);
        LOG.info("Queue ASF request with key: " + key);

        message.setMessageTag(nextMessageTag);

        RmcpPacket packet = new RmcpPacket();
        packet.withRemoteAddress(systemAddress);
        packet.withData(message);
        // packet.withSequenceNumber(0);

        client.send(packet);

        return new RequestFuture<T>(context);
    }

    /**
     * Send IPMI message.
     *
     * IPMI message could be tagged message (when session is not ready) or session message
     */
    public <T extends AbstractTaggedIpmiPayload> Future<T> sendIpmiRequest(AbstractTaggedIpmiPayload message) {
        byte nextMessageTag = queue.getNextIpmiTag();
        message.setMessageTag(nextMessageTag);

        return _send(null, message, nextMessageTag);
    }

    public <T extends IpmiResponse> Future<T> sendIpmiRequest(IpmiSession session, IpmiRequest request) {
        byte sequenceNumber = queue.getNextIpmiSeq();
        request.setSequenceNumber(sequenceNumber);

        return _send(session, request, sequenceNumber);
    }

    private <T extends IpmiPayload> Future<T> _send(IpmiSession session, IpmiPayload payload, byte seq) {
        RequestContext context = new RequestContext();
        SocketAddress addr = this.getSystemAddress();
        // for IPMI, RMCP SeqId is 0xFF, but here let's set to 0 as it is not important
        RequestKey key = new RequestKey(addr, 255, seq);
        pending.put(key, context);
        LOG.info("Queue IPMI request with key: " + key);

        IpmiSessionWrapper wrapper;
        if(supportIPMIv2) {
            wrapper =new Ipmi20SessionWrapper();
        }
        else {
            wrapper = new Ipmi15SessionWrapper();
        }

        wrapper.setSocketAddress(getSystemAddress());
        wrapper.setIpmiSessionId(session == null ? 0 : session.getSystemSessionId());
        wrapper.setIpmiPayload(payload);

        if (session != null) {
            if (wrapper.isEncrypted())
                wrapper.setIpmiSessionSequenceNumber(session.nextEncryptedSequenceNumber());
            else
                wrapper.setIpmiSessionSequenceNumber(session.nextUnencryptedSequenceNumber());
        } else {
            wrapper.setIpmiSessionSequenceNumber(0);
        }

        RmcpPacket packet = new RmcpPacket();
        packet.withRemoteAddress(systemAddress);
        packet.withData(wrapper);

        client.send(packet);

        if(supportIPMIv2) {
            LOG.info("Send IPMI request with v2 wrapper: " + payload);
        }
        else {
            LOG.info("Send IPMI request with v1.5 wrapper: " + payload);
        }

        return new RequestFuture<T>(context);
    }

    private void _receive(RequestKey key, Object response) {
        LOG.info("Receive response for request key: " + key);

        RequestContext context = pending.remove(key);
        if(context == null) {
            LOG.info("Got unexpected response payload - " + response);
        }
        else {
            LOG.info("Got response payload - " + response);
            context.response = response;
            context.done(REQ_SUCC);
        }
    }

    //
    // ResponseProcessor interface
    @Override
    public void handleIpmiPayload(@Nonnull IpmiEndpoint endpoint, IpmiSession session, @Nonnull IpmiPayload response, int seqOrTag) {
        RequestKey key = new RequestKey(endpoint.getSystemAddress(), 255, seqOrTag);
        _receive(key, response);
    }

    @Override
    public void handleRmcpData(@Nonnull IpmiEndpoint endpoint, @Nonnull RmcpData response, int seq) {
        if(response instanceof AbstractAsfData) {
            AbstractAsfData data = (AbstractAsfData)response;
            RequestKey key = new RequestKey(endpoint.getSystemAddress(), 0, data.getMessageTag());
            _receive(key, response);
        }
        else {
            RequestKey key = new RequestKey(endpoint.getSystemAddress(), seq, 0);
            _receive(key, response);
        }
    }

    /**
     * Timeout response
     */
    @Override
    public void timeout(@Nonnull RequestKey key) {
        RequestContext context = pending.get(key);
        context.done(REQ_EXPR);
    }

    //
    // make tests to understand the capability of the target
    public boolean contact() {
        try {
            Future<AsfPresencePongData> pongFuture = sendAsfRequest(new AsfPresencePingData());
            AsfPresencePongData pong = pongFuture.get();
            if(pong.getSupportedEntities().contains(AsfPresencePongData.SupportedEntity.IPMI_SUPPORTED)) {
                System.out.println("IPMI Supported");
            }

            if(pong.getSupportedEntities().contains(AsfPresencePongData.SupportedEntity.SUPPORT_ASF_V1)) {
                System.out.println("Support ASF v1");
            }

            // Now get capabilities. We are monitoring, so always get User privilege only
            GetChannelAuthenticationCapabilitiesRequest request = new GetChannelAuthenticationCapabilitiesRequest();
            request.extendedCapabilities = true;
            request.channelPrivilegeLevel = IpmiChannelPrivilegeLevel.User;

            Future<GetChannelAuthenticationCapabilitiesResponse> future = sendIpmiRequest(null, request);
            GetChannelAuthenticationCapabilitiesResponse resp = future.get();

            System.out.println("Supported auth types: " + resp.authenticationTypes);


            if(resp.extendedCapabilities.contains(GetChannelAuthenticationCapabilitiesResponse.ExtendedCapabilities.IPMI15_CONNECTIONS_SUPPORTED)) {
                System.out.println("IPMIv1.5 supported");
            }

            if(resp.extendedCapabilities.contains(GetChannelAuthenticationCapabilitiesResponse.ExtendedCapabilities.IPMI20_CONNECTIONS_SUPPORTED)) {
                System.out.println("IPMIv2.0 supported");
                supportIPMIv2 = true;
            }

            // now cyphers, only for IPMI v2
            /* TODO: not support or now
            GetChannelCipherSuitesRequest cipherReq = new GetChannelCipherSuitesRequest();
            cipherReq.channelNumber = IpmiChannelNumber.CURRENT;
            cipherReq.payloadType = IpmiPayloadType.IPMI;
            cipherReq.listType = GetChannelCipherSuitesRequest.ListType.Supported;

            ByteBuffer buffer = ByteBuffer.allocate(GetChannelCipherSuitesRequest.MAX_LIST_INDEX * GetChannelCipherSuitesRequest.MAX_LIST_CYPHER_SUITES);
            for(int i = 0; i < GetChannelCipherSuitesRequest.MAX_LIST_INDEX; i++) {
                cipherReq.listIndex = 0;  // get first 16 suites
                Future<GetChannelCipherSuitesResponse> cipherFuture = sendIpmiRequest(null, cipherReq);
                GetChannelCipherSuitesResponse cipherResp = cipherFuture.get();

                if(cipherResp.getDataLength() > GetChannelCipherSuitesRequest.MAX_LIST_CYPHER_SUITES) {
                    throw new RuntimeException("Invalid CipherSuite response with too large response: " + cipherResp.getDataLength());
                }

                buffer.put(cipherResp.getDataBytes());  // write the returned bytes, which is packed records for ciphers
                if(cipherResp.getDataLength() < GetChannelCipherSuitesRequest.MAX_LIST_CYPHER_SUITES) {
                    break;
                }
            }

            buffer.flip();

            while(buffer.hasRemaining()) {
                CipherSuite suite = CipherSuite.decode(buffer);
                if(suite != null) {
                    System.out.println("Decode one cipher suite - " + suite);
                    cipherSuits.add(suite);
                }
            }
            */
            return true;
        }
        catch(Exception e) {
            LOG.error("Cannot contact remote host", e);
        }

        return false;
    }

    private static final int REQ_PEND = 0;  // pending
    private static final int REQ_SUCC = 1;  // success
    private static final int REQ_EXPR = 2;  // expired or  timedout
    private static final int REQ_CANC = 3;  // cancelled

    private static class RequestContext {
        public AtomicInteger status = new AtomicInteger(REQ_PEND);   // 0 - pending, 1 response got, 2 timedout, 3 canceled
        public Object response = null;
        public final CountDownLatch latch = new CountDownLatch(1);

        public boolean done(int s) {
            if(status.compareAndSet(REQ_PEND, s)) {
                latch.countDown();
                return true;
            }

            return false;
        }
    }

    //
    //
    static class RequestFuture<T> implements Future<T> {
        private final RequestContext context;
        RequestFuture(RequestContext context) {
            this.context = context;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return context.done(REQ_CANC);
        }

        @Override
        public boolean isCancelled() {
            return context.status.get() == REQ_CANC;
        }

        @Override
        public boolean isDone() {
            return context.status.get() != REQ_PEND;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            context.latch.await();

            return _return();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if(!context.latch.await(timeout, unit)) {
                throw new TimeoutException();
            }

            return _return();
        }

        private T _return() throws ExecutionException {
            if(context.status.get() == REQ_SUCC) {
                return (T) context.response;
            }

            if(context.status.get() == REQ_CANC) {
                throw new CancellationException();
            }

            throw new ExecutionException("Status - " + context.status, null);
        }
    }
}
