/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.client.netty;

import org.anarres.ipmi.protocol.client.IpmiClient;
import org.anarres.ipmi.protocol.client.dispatch.AbstractIpmiReceiver;
import org.anarres.ipmi.protocol.client.dispatch.IpmiReceiver;
import org.anarres.ipmi.protocol.client.dispatch.IpmiReceiverKey;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.client.visitor.IpmiHandlerContext;
import org.anarres.ipmi.protocol.packet.asf.AsfPresencePingData;
import org.anarres.ipmi.protocol.packet.ipmi.Ipmi15SessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiChannelPrivilegeLevel;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiLun;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiSessionWrapper;
import org.anarres.ipmi.protocol.packet.ipmi.command.AbstractIpmiResponse;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesResponse;
import org.anarres.ipmi.protocol.packet.ipmi.payload.IpmiPayload;
import org.anarres.ipmi.protocol.packet.rmcp.RmcpPacket;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
// import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class IpmiClientImplTest {

    @Test
    public void testClient() throws Exception {
        IpmiClientImpl client = new IpmiClientImpl();

        client.start();

        InetSocketAddress serverAddr = new InetSocketAddress("192.168.3.254", 623);
        IpmiHandlerContext context = new IpmiHandlerContext(client, serverAddr);
        IpmiSession session = client.getSessionManager().newIpmiSession();

        GetChannelAuthenticationCapabilitiesRequest request = new GetChannelAuthenticationCapabilitiesRequest();
        request.extendedCapabilities = true;
        request.channelPrivilegeLevel = IpmiChannelPrivilegeLevel.User;

        final CountDownLatch latch = new CountDownLatch(1);

        IpmiReceiver receiver = new AbstractIpmiReceiver(AbstractIpmiResponse.class) {
            @Override
            protected void doReceive(@Nonnull IpmiHandlerContext context, @Nonnull IpmiSession session, @Nonnull IpmiPayload response) {
                System.out.println("Hello, world!");
                latch.countDown();
            }

            @Override
            public void timeout(@Nonnull IpmiReceiverKey key) {
                System.out.println("Timedout!");
                latch.countDown();
            }
        };

        context.send(session, request, GetChannelAuthenticationCapabilitiesResponse.class, receiver);

        RmcpPacket packet = new RmcpPacket();
        packet.withRemoteAddress(serverAddr);
        packet.withData(new AsfPresencePingData());
        client.send(packet);

        try {
            if(latch.await(60, TimeUnit.SECONDS)) {
                System.out.println("Message processed");
            }
            else {
                Assert.assertTrue(false);
            }
        }
        catch(Exception e) {
            Assert.assertTrue(false);
        }

        Thread.sleep(5000);
        /*
        IpmiSessionWrapper data = new Ipmi15SessionWrapper();
        data.setIpmiPayload(request);

        RmcpPacket packet = new RmcpPacket();
        packet.withData(data);


        // ByteBuffer buf = ByteBuffer.allocate(packet.getWireLength(context));
        // packet.toWire(context, buf);

        client.send(packet);
        */
    }
}
