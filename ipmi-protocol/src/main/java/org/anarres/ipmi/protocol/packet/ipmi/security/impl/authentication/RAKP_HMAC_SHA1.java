/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.packet.ipmi.security.impl.authentication;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.anarres.ipmi.protocol.packet.ipmi.security.IpmiAuthenticationAlgorithm;

/**
 * [IPMI2] Section 13.28.1, page 158.
 *
 * @author shevek
 */
public class RAKP_HMAC_SHA1 extends AbstractJCEHash {

    public RAKP_HMAC_SHA1(String key) throws NoSuchAlgorithmException, InvalidKeyException {
        super("HmacSHA1", key);
    }

    @Override
    public IpmiAuthenticationAlgorithm getName() {
        return IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA1;
    }
}