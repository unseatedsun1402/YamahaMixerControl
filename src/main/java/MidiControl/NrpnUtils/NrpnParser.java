package MidiControl.NrpnUtils;

import javax.sound.midi.ShortMessage;

public class NrpnParser {

    private int currentMsb = -1;
    private int currentLsb = -1;
    private int currentValueMsb = -1;
    private int currentValueLsb = -1;

    private boolean hasMsb = false;
    private boolean hasLsb = false;
    private boolean hasValueMsb = false;
    private boolean hasValueLsb = false;

    /**
     * Parse a CC ShortMessage and return a complete NRPN event if available.
     * Otherwise return null.
     */
    public NrpnMessage parse(ShortMessage msg) {

        int cc = msg.getData1();
        int data = msg.getData2();

        switch (cc) {

            case 99 -> { // NRPN MSB
                // If we already had a pending NRPN, emit it before starting a new one
                if (hasMsb && hasLsb && hasValueMsb) {
                    NrpnMessage result = buildMessage();
                    reset();
                    // Start new NRPN
                    currentMsb = data;
                    hasMsb = true;
                    return result;
                }
                currentMsb = data;
                hasMsb = true;
            }

            case 98 -> { // NRPN LSB
                currentLsb = data;
                hasLsb = true;
            }

            case 6 -> { // Data Entry MSB
                currentValueMsb = data;
                hasValueMsb = true;
            }

            case 38 -> { // Data Entry LSB
                currentValueLsb = data;
                hasValueLsb = true;

                // Full 14-bit NRPN complete
                NrpnMessage result = buildMessage();
                reset();
                return result;
            }

            default -> {
                // If we receive ANY other CC and we have a pending NRPN (MSB+LSB+valueMSB),
                // emit it immediately as a 7-bit NRPN.
                if (hasMsb && hasLsb && hasValueMsb) {
                    NrpnMessage result = buildMessage();
                    reset();
                    return result;
                }
            }
        }

        // If we have MSB+LSB+valueMSB but no LSB yet, do NOT emit yet.
        // We wait to see if CC 38 arrives next.
        return null;
    }

    private NrpnMessage buildMessage() {
        int value = (currentValueMsb << 7) | (hasValueLsb ? currentValueLsb : 0);
        return new NrpnMessage(currentMsb, currentLsb, value);
    }

    private void reset() {
        hasMsb = hasLsb = hasValueMsb = hasValueLsb = false;
        currentMsb = currentLsb = currentValueMsb = currentValueLsb = -1;
    }
}