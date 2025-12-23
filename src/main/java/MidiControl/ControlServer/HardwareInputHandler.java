package MidiControl.ControlServer;

import MidiControl.NrpnUtils.NrpnParser;
import MidiControl.NrpnUtils.NrpnRegistry;
import MidiControl.NrpnUtils.NrpnMessage;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import java.util.Arrays;
import java.util.logging.Logger;

public class HardwareInputHandler {

    private final Logger logger = Logger.getLogger(HardwareInputHandler.class.getName());
    private final NrpnParser nrpnParser;
    private final NrpnRegistry nrpnRegistry;
    private static Boolean DEBUG = false;

    public static void enableDebug(){
        DEBUG = true;
    }

    public HardwareInputHandler(NrpnParser parser, NrpnRegistry registry) {
        this.nrpnParser = parser;
        this.nrpnRegistry = registry;
    }

    public CanonicalInputEvent handle(MidiMessage msg) {

        if (msg instanceof SysexMessage sysex) {
            byte[] full = sysex.getMessage();

            if (isYamahaHeartbeat(full)) {
                if(DEBUG){logger.fine("Ignoring Yamaha heartbeat SysEx.");}
                return null;
            }

            logger.info("Received SysEx: " + Arrays.toString(full));
            return CanonicalInputEvent.fromSysex(full);
        }

        if (msg instanceof ShortMessage sm) {

            int status = sm.getStatus() & 0xF0;

            if (status == ShortMessage.CONTROL_CHANGE) {

                NrpnMessage nrpn = nrpnParser.parse(sm);

                if (nrpn != null) {
                    if(DEBUG){logger.fine("Assembled NRPN: MSB=" + nrpn.msb + " LSB=" + nrpn.lsb + " value=" + nrpn.value);}
                    return CanonicalInputEvent.fromNrpn(nrpn);
                }

                return CanonicalInputEvent.fromCc(sm);
            }

            if(DEBUG){logger.warning("Unhandled ShortMessage: " + sm.getCommand());}
            return CanonicalInputEvent.fromCc(sm);
        }

        logger.warning("Unhandled MIDI message type: " + msg.getClass().getName());
        return null;
    }

    private boolean isYamahaHeartbeat(byte[] msg) {
        return msg.length == 7 &&
                (msg[0] & 0xFF) == 0xF0 &&
                (msg[1] & 0xFF) == 0x43 &&
                (msg[2] & 0xF0) == 0x10 &&
                (msg[3] & 0xF0) == 0x30 &&
                (msg[4] & 0xFF) == 0x1A &&
                (msg[5] & 0xFF) == 0x7F &&
                (msg[6] & 0xFF) == 0xF7;
    }
}