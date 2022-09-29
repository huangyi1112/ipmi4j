/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.session;

import com.google.common.primitives.UnsignedBytes;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import org.anarres.ipmi.protocol.client.IpmiEndpoint;
import org.anarres.ipmi.protocol.packet.asf.AsfRsspSessionStatus;
import org.anarres.ipmi.protocol.packet.ipmi.IpmiSessionAuthenticationType;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiRequest;
import org.anarres.ipmi.protocol.packet.ipmi.command.IpmiResponse;
import org.anarres.ipmi.protocol.packet.ipmi.command.messaging.GetChannelAuthenticationCapabilitiesResponse;
import org.anarres.ipmi.protocol.packet.ipmi.payload.*;
import org.anarres.ipmi.protocol.packet.ipmi.security.IpmiAuthenticationAlgorithm;
import org.anarres.ipmi.protocol.packet.ipmi.security.IpmiConfidentialityAlgorithm;
import org.anarres.ipmi.protocol.packet.ipmi.security.IpmiIntegrityAlgorithm;

import static org.anarres.ipmi.protocol.packet.ipmi.security.CipherSuite.*;

/**
 * Server Client talk in session using two session IDs:
 * 1. consoleSessionId: this is sent to server when send Session Open request and will be used by
 *                      server to identify the session (filled in responses' session ID field)
 * 2. systemSessionId: this is received as Session Open Response, and is used by client to identify
 *                      session (fill in requests' session ID field)
 * @author shevek
 */
public class IpmiSession {

    private IpmiSessionState state = IpmiSessionState.UNKNOWN;
    private final IpmiEndpoint endpoint;
    private final int consoleSessionId;
    public GetChannelAuthenticationCapabilitiesResponse channelAuthenticationCapabilities;
    private int systemSessionId;
    private AtomicInteger encryptedSequenceNumber = new AtomicInteger(1);
    private AtomicInteger unencryptedSequenceNumber = new AtomicInteger(1);
    private IpmiSessionAuthenticationType authenticationType = IpmiSessionAuthenticationType.RMCPP;
    private IpmiAuthenticationAlgorithm authenticationAlgorithm;
    private IpmiConfidentialityAlgorithm confidentialityAlgorithm;
    private IpmiConfidentialityAlgorithm.State confidentialityAlgorithmState;
    private IpmiIntegrityAlgorithm integrityAlgorithm;

    private byte[] sessionIntegrationKey;
    private byte[] additionalKey1;
    private byte[] additionalKey2;

    private String username;
    private String password;

    public IpmiSession(IpmiEndpoint endpoint, int consoleSessionId) {
        this.endpoint = endpoint;
        this.consoleSessionId = consoleSessionId;
    }

    public int getConsoleSessionId() {
        return consoleSessionId;
    }

    public int getSystemSessionId() {
        return systemSessionId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setSystemSessionId(int systemSessionId) {
        this.systemSessionId = systemSessionId;
    }

    public int nextUnencryptedSequenceNumber() {
        return unencryptedSequenceNumber.getAndIncrement();
    }

    public int nextEncryptedSequenceNumber() {
        return encryptedSequenceNumber.getAndIncrement();
    }

    @Nonnull
    public byte[] newRandomSeed(int length) {
        SecureRandom r = new SecureRandom();
        return r.generateSeed(length);
    }

    @Nonnull
    public IpmiSessionAuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public IpmiAuthenticationAlgorithm getAuthenticationAlgorithm() {
        return authenticationAlgorithm;
    }

    public void setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm authenticationAlgorithm) {
        this.authenticationAlgorithm = authenticationAlgorithm;
    }

    public IpmiConfidentialityAlgorithm getConfidentialityAlgorithm() {
        return confidentialityAlgorithm;
    }

    public void setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm confidentialityAlgorithm) {
        this.confidentialityAlgorithm = confidentialityAlgorithm;
    }

    public IpmiConfidentialityAlgorithm.State getConfidentialityAlgorithmState() {
        return confidentialityAlgorithmState;
    }

    public void setConfidentialityAlgorithmState(IpmiConfidentialityAlgorithm.State confidentialityAlgorithmState) {
        this.confidentialityAlgorithmState = confidentialityAlgorithmState;
    }

    public IpmiIntegrityAlgorithm getIntegrityAlgorithm() {
        return integrityAlgorithm;
    }

    public void setIntegrityAlgorithm(IpmiIntegrityAlgorithm integrityAlgorithm) {
        this.integrityAlgorithm = integrityAlgorithm;
    }

    /**
     * Session integration key
     */
    public byte[] getSessionIntegrationKey() {
        return sessionIntegrationKey;
    }

    // open session. Session can be used after open
    private static final int RETRY = 4;
    public boolean open(String username, String password) throws Exception {
        this.username = username;
        this.password = password;

        // TODO: decide the best ciper suite, but for now let's fix to SUITE 3
        state = IpmiSessionState.AUTHCAP;
        initSessionAlgorithms(IPMI_LANPLUS_CIPHER_SUITE_3);


        /*
         * If the open/rakp1/rakp3 sequence encounters a timeout, the whole
         * sequence needs to restart. The individual messages are not
         * individually retryable, as the session state is advancing.
         */
        // for (int retry = 0; retry < RETRY; retry++) {
        {
            state = IpmiSessionState.OPENSESSION;
            /*
            session->v2_data.session_state = LANPLUS_STATE_PRESESSION;

            if ((rc = ipmi_lanplus_open_session(intf)) == 1) {
			goto fail;
            }
            if (rc == 2) {
                lprintf(LOG_DEBUG, "Retry lanplus open session, %d", retry);
                continue;
            }
            */
            IpmiOpenSessionRequest openReq = new IpmiOpenSessionRequest(this, RequestedMaximumPrivilegeLevel.USER);
            Future<IpmiOpenSessionResponse> openResFuture = endpoint.sendIpmiRequest(openReq);
            IpmiOpenSessionResponse openResp = openResFuture.get();

            // TODO: handle timeout retry
            if(openResp.statusCode != AsfRsspSessionStatus.NO_ERROR) {
                System.out.println("Open session failed with error - " + openResp.statusCode);
                return false;
            }

            /**
             * 		session->v2_data.max_priv_level =
             * 			rsp->payload.open_session_response.max_priv_level;
             * 		session->v2_data.bmc_id         =
             * 			rsp->payload.open_session_response.bmc_id;
             * 		session->v2_data.auth_alg       =
             * 			rsp->payload.open_session_response.auth_alg;
             * 		session->v2_data.integrity_alg  =
             * 			rsp->payload.open_session_response.integrity_alg;
             * 		session->v2_data.crypt_alg      =
             * 			rsp->payload.open_session_response.crypt_alg;
             * 		session->v2_data.session_state  =
             * 			LANPLUS_STATE_OPEN_SESSION_RECEIEVED;
             */
            System.out.println("Privilege: " + openResp.requestedMaximumPrivilegeLevel);
            System.out.println("BMC ID: " + openResp.systemSessionId);
            System.out.println("Auth Algo: " + openResp.authenticationAlgorithm);
            System.out.println("Integrity Algo: " + openResp.integrityAlgorithm);
            System.out.println("Crypto Algo: " + openResp.confidentialityAlgorithm);

            // save the important info
            systemSessionId = openResp.systemSessionId;

            /*
             if ((rc = ipmi_lanplus_rakp1(intf)) == 1) {
             goto fail;
             }
             if (rc == 2) {
             lprintf(LOG_DEBUG, "Retry lanplus rakp1, %d", retry);
             continue;
             }
             */
            state = IpmiSessionState.RAKP1;

            IpmiRAKPMessage1 rakp1Req = new IpmiRAKPMessage1(this);
            Future<IpmiRAKPMessage2> rakp2ResFuture = endpoint.sendIpmiRequest(rakp1Req);
            IpmiRAKPMessage2 rakp2Res = rakp2ResFuture.get();
            if(rakp2Res.statusCode != AsfRsspSessionStatus.NO_ERROR) {
                System.out.println("Open session failed with wrong RAKP message 2 status - " + rakp2Res.statusCode);
                return false;
            }

            //TODO: validate ...
            if(authenticationAlgorithm != IpmiAuthenticationAlgorithm.RAKP_NONE) {
                byte[] hash = authenticationAlgorithm.hash(password, rakp2Res.getPairedAuthData(rakp1Req));
                if(!Arrays.equals(rakp2Res.keyExchangeAuthenticationCode, hash)) {
                    System.out.println("Got RAKP message auth code invalid!");
                }

                sessionIntegrationKey = generateSIK(rakp1Req, rakp2Res);
            }
            else {
                sessionIntegrationKey = new byte[0];
            }

            additionalKey1 = getAdditionalKey(1);
            additionalKey2 = getAdditionalKey(2);

            /*
            if ((rc = ipmi_lanplus_rakp3(intf)) == 1) {
			goto fail;
            }
            if (rc == 0) break;
            lprintf(LOG_DEBUG,"Retry lanplus rakp3, %d", retry);
            */
            state = IpmiSessionState.RAKP3;
            IpmiRAKPMessage3 rakp3Req = new IpmiRAKPMessage3();
            rakp3Req.statusCode = rakp2Res.statusCode;
            rakp3Req.systemSessionId = systemSessionId;

            if(authenticationAlgorithm != IpmiAuthenticationAlgorithm.RAKP_NONE) {
                rakp3Req.keyExchangeAuthenticationCode = authenticationAlgorithm.hash(password, rakp2Res.generateKeyExchangeAuthCode(rakp1Req));
            }
            else {
                rakp3Req.keyExchangeAuthenticationCode = new byte[0];
            }

            Future<IpmiRAKPMessage4> rakp4ResFuture = endpoint.sendIpmiRequest(rakp3Req);
            IpmiRAKPMessage4 rakp4Res = rakp4ResFuture.get();
            if(rakp4Res.statusCode != AsfRsspSessionStatus.NO_ERROR) {
                System.out.println("Open session failed with wrong RAKP message 4 status - " + rakp2Res.statusCode);
                return false;
            }
        }

        // register this to context

        state = IpmiSessionState.VALID;

        return true;
    }

    public <T extends IpmiResponse> Future<T> send(IpmiRequest ipmiRequest) {
        if(state != IpmiSessionState.VALID) {
            throw new IllegalStateException("Session not opened");
        }

        return endpoint.sendIpmiRequest(this, ipmiRequest);
    }

    public byte[] getAdditionalKey1() {
        return additionalKey1;
    }

    public byte[] getAdditionalKey2() {
        return additionalKey2;
    }

    private void initSessionAlgorithms(int cipherSuiteId) {
        switch (cipherSuiteId) {
            case IPMI_LANPLUS_CIPHER_SUITE_0:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_NONE);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.NONE);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_1:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA1);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.NONE);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_2:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA1);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_SHA1_96);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_3:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA1);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_SHA1_96);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.AES_CBC_128);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_4:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA1);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_SHA1_96);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.xRC4_128);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_5:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA1);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_SHA1_96);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.xRC4_40);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_6:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.NONE);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_7:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_8:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.AES_CBC_128);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_9:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.xRC4_128);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_10:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.xRC4_40);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_11:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_12:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.AES_CBC_128);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_13:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.xRC4_128);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_14:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_MD5);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.MD5_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.xRC4_40);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_15:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA256);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.NONE);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_16:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA256);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_SHA256_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.NONE);
                break;
            case IPMI_LANPLUS_CIPHER_SUITE_17:
                setAuthenticationAlgorithm(IpmiAuthenticationAlgorithm.RAKP_HMAC_SHA256);
                setIntegrityAlgorithm(IpmiIntegrityAlgorithm.HMAC_SHA256_128);
                setConfidentialityAlgorithm(IpmiConfidentialityAlgorithm.AES_CBC_128);
                break;
        }
    }

    /**
     * Generate the SIK (Session Integration Key) for future message exchanging ...
     * @param rakp1
     * @return
     */
    private byte[] generateSIK(IpmiRAKPMessage1 rakp1, IpmiRAKPMessage2 rakp2) throws NoSuchAlgorithmException, InvalidKeyException {
        /* TODO: We will be hashing with Kg */
        /*
         * Section 13.31 of the IPMI v2 spec describes the SIK creation
         * using Kg.  It specifies that Kg should not be truncated.
         * Kg is set in ipmi_intf.
         */
        /*
        if (getBmcKey() == null || getBmcKey().length <= 0) {
            key = getPassword().getBytes();
        } else {
            key = getBmcKey();
        }
        */

        // byte[] key = password.getBytes(StandardCharsets.UTF_8);
        return authenticationAlgorithm.hash(password, rakp2.prepareSIKData(rakp1));
    }

    /** [IPMI2] Section 13.32, page 165. */
    @Nonnull
    private byte[] getAdditionalKey(@Nonnegative int idx) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] raw = new byte[20];   // Should this be 20, or the hash length?
        Arrays.fill(raw, UnsignedBytes.checkedCast(idx));

        if (authenticationAlgorithm == IpmiAuthenticationAlgorithm.RAKP_NONE) {
            return raw;
        }

        // key.length shall match authenticationAlgorithm.getLength
        return authenticationAlgorithm.hash(sessionIntegrationKey, ByteBuffer.wrap(raw));
    }
}
