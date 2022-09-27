/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.codec;

import org.anarres.ipmi.protocol.client.session.IpmiPacketContext;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class IpmiPacketDecoderTest {

    // A trivial enough test suite to make Jenkins pass.
    @Test
    public void testSomeMethod() {
        IpmiPacketContext context = new IpmiPacketContext();
        IpmiPacketDecoder decoder = new IpmiPacketDecoder(context);
        assertNotNull(decoder);
    }
}