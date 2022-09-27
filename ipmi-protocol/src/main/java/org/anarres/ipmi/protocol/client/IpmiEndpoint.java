/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client;

import java.net.SocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

import org.anarres.ipmi.protocol.IpmiUtils;
import org.anarres.ipmi.protocol.client.dispatch.RequestKey;
import org.anarres.ipmi.protocol.client.dispatch.ResponseProcessor;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.packet.asf.AbstractAsfData;
import org.anarres.ipmi.protocol.packet.ipmi.Ipmi15SessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiSessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiResponse;
import org.anarres.ipmi.protocol.packet.ipmi.payload.AbstractTaggedIpmiPayload;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;
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
    private final ConcurrentMap<Integer, IpmiSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<RequestKey, RequestContext> pending = new ConcurrentHashMap<>();

    public IpmiEndpoint(@Nonnull IpmiClient client, @Nonnull SocketAddress systemAddress) {
        this.client = client;
        this.systemAddress = systemAddress;
    }

    @Nonnull
    public IpmiSession newSession() {
        for (;;) {
            int id = IpmiUtils.RANDOM.nextInt();
            if (id == 0)
                continue;
            IpmiSession session = new IpmiSession(this, id);
            if (sessions.putIfAbsent(id, session) == null)
                return session;
        }
    }

    public IpmiSession getSession(int id) {
        if (id == 0) {
            return null;
        }

        return sessions.get(id);
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

        IpmiSessionWrapper wrapper = new Ipmi15SessionWrapper();
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

        return new RequestFuture<T>(context);
    }

    private void _receive(RequestKey key, Object response) {
        LOG.info("Receive response for request key: " + key);

        RequestContext context = pending.get(key);
        if(context == null) {
            LOG.info("Got unexpected response payload - " + response);
        }
        else {
            context.response = response;
            context.done(REQ_SUCC);
            LOG.info("Got response payload - " + response);
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
