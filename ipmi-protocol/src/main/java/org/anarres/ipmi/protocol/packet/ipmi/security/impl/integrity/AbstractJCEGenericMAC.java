/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.packet.ipmi.security.impl.integrity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author shevek
 */
public abstract class AbstractJCEGenericMAC implements GenericMAC {

    private final Mac mac;

    protected AbstractJCEGenericMAC(@Nonnull String algorithm, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        mac = Mac.getInstance(algorithm);
        if(key != null) {
            init(key.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nonnull
    protected Mac getMac() {
        return mac;
    }

    @Override
    public void init(byte[] key) throws InvalidKeyException {
        SecretKeySpec skey = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(skey);
    }

    @Override
    public void update(ByteBuffer input) {
        mac.update(input);
    }

    @Override
    public byte[] doFinal() {
        return mac.doFinal();
    }
}