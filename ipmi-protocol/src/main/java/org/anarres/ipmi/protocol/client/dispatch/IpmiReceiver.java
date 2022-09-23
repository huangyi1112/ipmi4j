/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.dispatch;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.client.visitor.IpmiHandlerContext;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author shevek
 */
public interface IpmiReceiver {
    /**
     * Receive response
     */
    public void receive(@Nonnull IpmiHandlerContext context, @CheckForNull IpmiSession session, @Nonnull IpmiPayload response);

    /**
     * Timeout response
     */
    public void timeout(@Nonnull IpmiReceiverKey key);

    /**
     * Wait till a response got or ...
     * @param timeout
     * @param unit
     */
    // public void await(long timeout, TimeUnit unit);
}
