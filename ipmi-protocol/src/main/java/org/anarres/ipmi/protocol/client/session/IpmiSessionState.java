/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.client.session;

/**
 *
 * @author shevek
 *
 * enum LANPLUS_SESSION_STATE {
 * 	LANPLUS_STATE_PRESESSION = 0,
 * 	LANPLUS_STATE_OPEN_SESSION_SENT,
 * 	LANPLUS_STATE_OPEN_SESSION_RECEIEVED,
 * 	LANPLUS_STATE_RAKP_1_SENT,
 * 	LANPLUS_STATE_RAKP_2_RECEIVED,
 * 	LANPLUS_STATE_RAKP_3_SENT,
 * 	LANPLUS_STATE_ACTIVE,
 * 	LANPLUS_STATE_CLOSE_SENT,
 * };
 */
public enum IpmiSessionState {
    UNKNOWN,
    AUTHCAP,
    OPENSESSION,
    RAKP1,
    RAKP3,
    SETPRIVILEGE,
    VALID;
}
