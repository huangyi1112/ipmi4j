/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.ipmi.protocol.packet.asf;

import com.google.common.collect.ImmutableMap;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.ipmi.protocol.packet.common.Bits;
import org.anarres.ipmi.protocol.packet.rmcp.Packet;

/**
 * RMCP Packet Header.
 * 
 * http://www.dmtf.org/sites/default/files/standards/documents/DSP0136.pdf
 * http://www.dmtf.org/standards/asf
 * Section 3.2.4.1 pages 32-34.
 * 
 * @author shevek
 */
public class BootAsfData extends AbstractAsfData {

    /** Section 3.2.4.1 page 33. */
    public static class Command {

        public static final byte NOP = 0x00;
        public static final byte FORCE_BOOT_PXE = 0x01;
        public static final byte FORCE_BOOT_DRIVE = 0x02;
        public static final byte FORCE_BOOT_DRIVE_SAFE = 0x03;
        public static final byte FORCE_BOOT_DIAGNOSTIC = 0x04;
        public static final byte FORCE_BOOT_OPTICAL = 0x05;
    }

    public static enum Option implements Bits.Wrapper {

        LOCK_POWER_BUTTON(0, 1),
        LOCK_RESET_BUTTON(0, 2),
        LOCK_KEYBOARD(0, 5),
        LOCK_SLEEP_BUTTON(0, 6),
        USER_PASSWORD_BYPASS(1, 3),
        FORCE_PROGRESS_EVENTS(1, 4),
        FIRMWARE_VERBOSITY_DEFAULT(1, ImmutableMap.of(5, false, 6, false)), // No bits.
        FIRMWARE_VERBOSITY_QUIET(1, ImmutableMap.of(5, true, 6, false)),
        FIRMWARE_VERBOSITY_VERBOSE(1, ImmutableMap.of(5, false, 6, true)),
        FIRMWARE_VERBOSITY_SCREEN_BLANK(1, ImmutableMap.of(5, true, 6, true)),
        CONFIGURATION_DATA_RESET(1, 7);
        private final Bits bit;

        private Option(int byteIndex, int bitIndex) {
            this.bit = Bits.forBitIndex(byteIndex, bitIndex);
        }

        private Option(@Nonnegative int byteIndex, @Nonnull Map<Integer, Boolean> bitValues) {
            this.bit = Bits.forBitValues(byteIndex, bitValues);
        }

        @Override
        public Bits getBits() {
            return bit;
        }
    }
    private byte command;
    private Set<Option> options = EnumSet.noneOf(Option.class);

    @Override
    public int getWireLength() {
        return 11;
    }

    @Override
    public void toWire(ByteBuffer buffer) {
        buffer.putInt(AsfRcmpData.IANA_ENTERPRISE_NUMBER);
        buffer.put(command);
        buffer.put(new byte[2]);    // TODO: Command params. :-(
        buffer.put(Bits.toBytes(2, options));
        buffer.put(new byte[2]);    // TODO: OEM parameters.
    }
}