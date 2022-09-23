/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.dispatch;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.net.SocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.client.visitor.IpmiClientIpmiPayloadHandler;
import org.anarres.ipmi.protocol.client.visitor.IpmiHandlerContext;
import org.anarres.ipmi.protocol.packet.ipmi.Ipmi15SessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiSessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiCommand;
import org.anarres.ipmi.protocol.packet.ipmi.payload.AbstractTaggedIpmiPayload;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;
import org.anarres.ipmi.protocol.packet.common.Packet;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpPacket;

/**
 *
 * @author shevek
 */
public class IpmiPayloadTransmitQueue implements IpmiHandlerContext.IpmiPacketQueue {

    public static interface IpmiPacketSender {

        public void send(@Nonnull Packet packet);
    }

    // -> Queue<QueueItem>
    private static class Queue extends LinkedBlockingQueue<QueueItem> {

        // private final BitSet messageTags = new BitSet(256);
        /** @see AbstractTaggedIpmiPayload#getMessageTag() */
        private int nextMessageTag;
        // private final BitSet sequenceNumbers = new BitSet(IpmiCommand.SEQUENCE_NUMBER_MASK + 1);
        /** @see IpmiCommand#getSequenceNumber() */
        private int nextSequenceNumber;
        private int outstandingRequests;
    }

    private static class QueueItem {

        private final IpmiSession session;
        private final IpmiPayload payload;
        private final IpmiReceiverKey key;
        private final IpmiReceiver receiver;

        public QueueItem(@CheckForNull IpmiSession session, @Nonnull IpmiPayload payload,
                @Nonnull IpmiReceiverKey key, @Nonnull IpmiReceiver receiver) {
            this.session = session;
            this.payload = payload;
            this.key = key;
            this.receiver = receiver;
        }
    }

    private static class QueueFactory extends CacheLoader<SocketAddress, Queue> {

        @Override
        public Queue load(SocketAddress key) throws Exception {
            return new Queue();
        }
    };
    private final LoadingCache<SocketAddress, Queue> ipmiQueues = CacheBuilder.newBuilder()
            // .weakKeys() // Discard queues for closed connections?
            .expireAfterAccess(1, TimeUnit.HOURS)
            .recordStats()
            .build(new QueueFactory());
    private final IpmiClientIpmiPayloadHandler ipmiPayloadSequencer = new IpmiClientIpmiPayloadHandler.TaggedAdapter() {

        @Override
        protected void handleDefault(IpmiHandlerContext context, IpmiSession session, IpmiPayload payload) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void handleTagged(IpmiHandlerContext context, IpmiSession session, AbstractTaggedIpmiPayload message) {
            Queue queue = getState(context);
            synchronized (queue) {
                byte nextMessageTag = (byte) queue.nextMessageTag++;
                message.setMessageTag(nextMessageTag);

                _queue(queue, context, session, message, nextMessageTag);
            }
        }

        @Override
        public void handleCommand(IpmiHandlerContext context, IpmiSession session, IpmiCommand message) {
            Queue queue = getState(context);
            synchronized (queue) {
                byte sequenceNumber = (byte) queue.nextSequenceNumber++;
                message.setSequenceNumber(sequenceNumber);

                _queue(queue, context, session, message, sequenceNumber);
            }
        }
    };
    private final IpmiReceiverRepository receiver;
    private final IpmiPacketSender sender;

    //
    // TODO: to avoid passing the receiver in the interfaces, so let's just create thread local to do this
    private final ThreadLocal<IpmiReceiver> responseReceiver = new ThreadLocal<>();
    private final ThreadLocal<Class<? extends IpmiPayload>> responseType = new ThreadLocal<>();

    public IpmiPayloadTransmitQueue(@Nonnull IpmiReceiverRepository receiver, @Nonnull IpmiPacketSender sender) {
        this.receiver = receiver;
        this.sender = sender;
    }

    @Nonnull
    private Queue getState(@Nonnull IpmiHandlerContext context) {
        return ipmiQueues.getUnchecked(context.getSystemAddress());
    }

    @Override
    public void queue(IpmiHandlerContext context, IpmiSession session, IpmiPayload message,
            Class<? extends IpmiPayload> type, IpmiReceiver receiver) {
        // todo: set message receiver, it will be used in the message handlers ...
        responseReceiver.set(receiver);
        responseType.set(type);

        message.apply(ipmiPayloadSequencer, context, session);
    }

    /**
     * Queue a message into given queue
     */
    private void _queue(Queue queue, IpmiHandlerContext context, IpmiSession session, IpmiPayload message, byte seq) {
        IpmiReceiverKey key = new IpmiReceiverKey(context.getSystemAddress(), responseType.get(), seq);
        QueueItem item = new QueueItem(session, message, key, responseReceiver.get());

        queue.add(item);
        queue.outstandingRequests ++;
        doSend(context.getSystemAddress(), item);
    }

    private void doSend(@Nonnull SocketAddress systemAddress, @Nonnull QueueItem item) {
        @CheckForNull
        IpmiSession session = item.session;
        @Nonnull
        IpmiPayload payload = item.payload;

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

        receiver.setReceiver(item.key, item.receiver);
        sender.send(packet);
    }
}
