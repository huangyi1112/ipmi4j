package org.anarres.ipmi.protocol.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.anarres.ipmi.protocol.client.dispatch.RequestKey;
import org.anarres.ipmi.protocol.client.dispatch.ResponseProcessor;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiCommand;
import org.anarres.ipmi.protocol.packet.ipmi.payload.AbstractTaggedIpmiPayload;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * manage request sent on a context (identified by a target address)
 */
public class RequestQueue {
    /** @See AbstractPacket#getSequenceNumber */
    private AtomicInteger nextRmcpSequence = new AtomicInteger(0);
    public byte getNextRmcpSeq() {
        byte nextSeq = (byte) (nextRmcpSequence.getAndIncrement() & 0xFF);
        if (nextSeq == (byte)0xFF) {
            nextSeq = (byte) (nextRmcpSequence.getAndIncrement() & 0xFF);
        }

        return nextSeq;
    }

    /**
     *  @See AbstractAsfData#getMessageTag
     *  Let's start from 1. We reserve 0 to avoid RequestKey with zero RMCP sequence # and zero ASF tag ...
     */
    private AtomicInteger nextAsfTag = new AtomicInteger(1);
    public byte getNextAsfTag() {
        byte nextMessageTag = (byte) (nextAsfTag.getAndIncrement() & 0xFF);
        if(nextMessageTag == 0 || nextMessageTag == (byte)0xFF) {
            // 0xFF is used for unidirectional ping ...
            nextMessageTag = (byte) (nextAsfTag.getAndIncrement() & 0xFF);
        }

        return nextMessageTag;
    }

    /** @see AbstractTaggedIpmiPayload#getMessageTag() */
    private AtomicInteger nextMessageTag = new AtomicInteger(0);
    public byte getNextIpmiTag() {
        return (byte) (nextMessageTag.getAndIncrement() & 0xFF);
    }

    /** @see IpmiCommand#getSequenceNumber() */
    private AtomicInteger nextSequenceNumber = new AtomicInteger(0);
    public byte getNextIpmiSeq() {
        return (byte) (nextSequenceNumber.getAndIncrement() & 0xFF);
    }
}
