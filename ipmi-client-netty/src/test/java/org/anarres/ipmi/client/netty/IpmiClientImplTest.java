/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.client.netty;

import org.anarres.ipmi.protocol.client.IpmiClient;
import org.anarres.ipmi.protocol.client.session.IpmiSession;
import org.junit.Test;
// import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class IpmiClientImplTest {

    @Test
    public void testClient() {
        IpmiClient client = new IpmiClientImpl();
        IpmiSession session = client.getSessionManager().newIpmiSession();
        session.setConfidentialityAlgorithmState();
        client.queue(context, session, );
    }
}
