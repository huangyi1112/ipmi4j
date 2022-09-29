package org.anarres.ipmi.protocol.packet.ipmi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Represents a cipher suite as retrieved by GetChannelCipherSuites request
 */
public class CipherSuite {
    private static final Logger LOG = LoggerFactory.getLogger(CipherSuite.class);
    public static final int SUITE_SIZE = 8;

    public static final byte IPMI_LANPLUS_CIPHER_SUITE_0 = (byte)0,
            IPMI_LANPLUS_CIPHER_SUITE_1 = (byte)1,
            IPMI_LANPLUS_CIPHER_SUITE_2 = (byte)2,
            IPMI_LANPLUS_CIPHER_SUITE_3 = (byte)3,   // always supported by IPMI
            IPMI_LANPLUS_CIPHER_SUITE_4 = (byte)4,
            IPMI_LANPLUS_CIPHER_SUITE_5 = (byte)5,
            IPMI_LANPLUS_CIPHER_SUITE_6 = (byte)6,
            IPMI_LANPLUS_CIPHER_SUITE_7 = (byte)7,
            IPMI_LANPLUS_CIPHER_SUITE_8 = (byte)8,
            IPMI_LANPLUS_CIPHER_SUITE_9 = (byte)9,
            IPMI_LANPLUS_CIPHER_SUITE_10 = (byte)10,
            IPMI_LANPLUS_CIPHER_SUITE_11 = (byte)11,
            IPMI_LANPLUS_CIPHER_SUITE_12 = (byte)12,
            IPMI_LANPLUS_CIPHER_SUITE_13 = (byte)13,
            IPMI_LANPLUS_CIPHER_SUITE_14 = (byte)14,
            IPMI_LANPLUS_CIPHER_SUITE_15 = (byte)15,   // only when enable CRYPTO_SHA256
            IPMI_LANPLUS_CIPHER_SUITE_16 = (byte)16,   // only when enable CRYPTO_SHA256
            IPMI_LANPLUS_CIPHER_SUITE_17 = (byte)17,   // only when enable CRYPTO_SHA256
            IPMI_LANPLUS_CIPHER_SUITE_RESERVED = (byte)0xff;

    /* From table 13-17 of the IPMI v2 specification */
    public static final byte IPMI_AUTH_RAKP_NONE = 0x00,
            IPMI_AUTH_RAKP_HMAC_SHA1 = 0x01,
            IPMI_AUTH_RAKP_HMAC_MD5  = 0x02,
            IPMI_AUTH_RAKP_HMAC_SHA256 = 0x03;

            /* From table 13-18 of the IPMI v2 specification */
    public static final byte IPMI_INTEGRITY_NONE = 0x00,
            IPMI_INTEGRITY_HMAC_SHA1_96 = 0x01,
            IPMI_INTEGRITY_HMAC_MD5_128 = 0x02,
            IPMI_INTEGRITY_MD5_128      = 0x03,
            IPMI_INTEGRITY_HMAC_SHA256_128 = 0x04;

            /* From table 13-19 of the IPMI v2 specification */
    public static final byte IPMI_CRYPT_NONE = 0x00,
            IPMI_CRYPT_AES_CBC_128 = 0x01,
            IPMI_CRYPT_XRC4_128    = 0x02,
            IPMI_CRYPT_XRC4_40     = 0x03;

    public byte startOfRecord = 0;
    public byte cipherSuiteId = 0;
    public final byte[] iana = new byte[3];
    public byte authAlg = 0;        // 0 means NONE
    public byte integrityAlg = 0;
    public byte cryptAlg = 0;

    public boolean isStandardSuite() {
        return startOfRecord == STANDARD_CIPHER_SUITE;
    }

    public boolean isOEMSuite() {
        return startOfRecord == OEM_CIPHER_SUITE;
    }

    /**
     * Decode one CipherSuite from buffer
     */
    private static final byte STANDARD_CIPHER_SUITE = (byte)0xc0;
    private static final byte OEM_CIPHER_SUITE = (byte)0xc1;
    public static CipherSuite decode(ByteBuffer buffer) {
        byte b = buffer.get();
        if(b == STANDARD_CIPHER_SUITE) {
            CipherSuite suite = new CipherSuite();
            suite.startOfRecord = b;
            /**
             * struct std_cipher_suite_record_t {
             * 	uint8_t start_of_record;
             * 	uint8_t cipher_suite_id;
             * 	uint8_t auth_alg;
             * 	uint8_t integrity_alg;
             * 	uint8_t crypt_alg;
             * } ATTRIBUTE_PACKING;
             */

            suite.cipherSuiteId = buffer.get();
            suite.authAlg = buffer.get();
            suite.integrityAlg = buffer.get();
            suite.cryptAlg = buffer.get();
            return suite;
        }
        else if(b == OEM_CIPHER_SUITE) {
            CipherSuite suite = new CipherSuite();
            suite.startOfRecord = b;
            /**
             * struct oem_cipher_suite_record_t {
             * 	uint8_t start_of_record;
             * 	uint8_t cipher_suite_id;
             * 	uint8_t iana[3];
             * 	uint8_t auth_alg;
             * 	uint8_t integrity_alg;
             * 	uint8_t crypt_alg;
             * } ATTRIBUTE_PACKING;
             */
            suite.cipherSuiteId = buffer.get();
            buffer.get(suite.iana);
            suite.authAlg = buffer.get();
            suite.integrityAlg = buffer.get();
            suite.cryptAlg = buffer.get();

            return suite;
        }

        LOG.error("Ignore non-start byte for CipherSuite - " + b);

        return null;
    }
}
