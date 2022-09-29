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
 * [IPMI2] Section 13.28.3, page 158.
 *
 * @author shevek
 */
public class RAKP_HMAC_MD5 extends AbstractJCEHash {

    public RAKP_HMAC_MD5(String key) throws NoSuchAlgorithmException, InvalidKeyException {
        super("HmacMD5", key);
    }

    @Override
    public IpmiAuthenticationAlgorithm getName() {
        return IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5;
    }
}