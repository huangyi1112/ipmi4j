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
import org.anarres.ipmi.protocol.packet.ipmi.command.chassis.ChassisControlRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.chassis.GetChassisStatusRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.chassis.GetChassisStatusResponse;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesResponse;
import org.junit.Assert;
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
        if(!context.contact()) {
            System.out.println("Cannot contact server");
            Assert.assertTrue(false);
        }

        IpmiSession session = context.newSession("ADMIN", "ADMIN");

        Future<GetChassisStatusResponse> future = session.send(new GetChassisStatusRequest());
        GetChassisStatusResponse resp = future.get();
        System.out.println("Got Chassis status: " + resp);
    }
}
