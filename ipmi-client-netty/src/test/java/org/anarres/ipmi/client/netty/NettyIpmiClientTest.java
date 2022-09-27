/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.client.netty;

import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.anarres.ipmi.protocol.client.IpmiEndpoint;
import org.anarres.ipmi.protocol.packet.asf.AsfPresencePingData;
import org.anarres.ipmi.protocol.packet.asf.AsfPresencePongData;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiChannelPrivilegeLevel;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesResponse;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;
// import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class NettyIpmiClientTest {
    @Test
    public void testClient() throws Exception {
        NettyIpmiClient client = new NettyIpmiClient();

        client.start();

        IpmiEndpoint context = client.getEndpoint(new InetSocketAddress("192.168.3.254", 623));

        Future<AsfPresencePongData> pongFuture2 = context.sendAsfRequest(new AsfPresencePingData());

        Future<AsfPresencePongData> pongFuture = context.sendAsfRequest(new AsfPresencePingData());
        AsfPresencePongData pong = pongFuture.get();

        System.out.println("Got PONG data: \n" + pong);


        IpmiSession session = context.newSession();

        // todo:
        //   session shall be opened before using

        GetChannelAuthenticationCapabilitiesRequest request = new GetChannelAuthenticationCapabilitiesRequest();
        request.extendedCapabilities = true;
        request.channelPrivilegeLevel = IpmiChannelPrivilegeLevel.User;

        Future<GetChannelAuthenticationCapabilitiesResponse> future = session.send(request);
        GetChannelAuthenticationCapabilitiesResponse resp = future.get();



        /*
        final CountDownLatch latch = new CountDownLatch(1);

        ResponseProcessor receiver = new ResponseProcessor<GetChannelAuthenticationCapabilitiesResponse>() {
            @Override
            public void receive(@Nonnull IpmiEndpoint context, @Nonnull IpmiSession session, @Nonnull GetChannelAuthenticationCapabilitiesResponse response) {
                System.out.println("Hello, world!");
                latch.countDown();
            }

            @Override
            public void timeout(@Nonnull RequestKey key) {
                System.out.println("Timedout!");
                latch.countDown();
            }
        };

        context.queueIpmiRequest(session, request, GetChannelAuthenticationCapabilitiesResponse.class, receiver);
        // context.queueIpmiRequest(session, request, receiver);

        RmcpPacket packet = new RmcpPacket();
        packet.withRemoteAddress(context.getSystemAddress());
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
        */

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
