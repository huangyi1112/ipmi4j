package org.anarres.ipmi.protocol.client.session;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO ...
 */
public interface IpmiPacketContext {
    default IpmiSession getIpmiSession(SocketAddress address, int sessionId) {
        return null;
    }
}
