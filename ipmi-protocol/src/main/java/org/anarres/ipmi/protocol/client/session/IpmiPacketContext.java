package org.anarres.ipmi.protocol.client.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO ...
 */
public class IpmiPacketContext {
    public static final String SESSION = "$session";

    private final Map<String, Object> contexts = new ConcurrentHashMap<>();

    public <T> T get(String name) {
        return (T) contexts.get(name);
    }

    public <T> void set(String name, T t) {
        contexts.put(name, t);
    }
}
