package MidiControl.ControlServer;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import MidiControl.NrpnUtils.NrpnParser;

import java.util.logging.*;

public class InputHandler {
    Logger logger = Logger.getLogger(InputHandler.class.getName());
    private static NrpnParser nrpnParser;

    public InputHandler(NrpnParser nrpnParser) {
        InputHandler.nrpnParser = nrpnParser;
    }
    
    public Object handle(MidiMessage msg){
        if (nrpnParser == null) {
            throw new IllegalStateException("NrpnParser not initialized");
        }

        if (msg instanceof ShortMessage sm) {
            logger.info(String.format(
                "MIDI Short Message: cmd=%d, data1=%d, data2=%d",
                sm.getCommand(), sm.getData1(), sm.getData2()
            ));
            return sm;
        }

        if (msg.getMessage()[0] == ShortMessage.CONTROL_CHANGE){
            ShortMessage controlChange = new ShortMessage();
            if (msg.getMessage().length < 3) {
                logger.warning("Control Change message too short: " + Arrays.toString(msg.getMessage()));
                return null;
            }
            try {
                controlChange.setMessage(msg.getMessage()[0], msg.getMessage()[1], msg.getMessage()[2]);
            } catch (InvalidMidiDataException e) {
                logger.log(Level.SEVERE, "Invalid MIDI data in Control Change message: " + Arrays.toString(msg.getMessage()), e);
                return null;
            }
            logger.info("Valid Control Message "+controlChange.getData1() + " " + controlChange.getData2());
            return controlChange;
        }

        if (msg instanceof SysexMessage sysex) {
            byte[] full = sysex.getMessage();   // <-- FIXED
            if (isYamahaHeartbeat(sysex.getData())) {
                logger.info("Received Yamaha Heartbeat Sysex message, ignoring.");
                return null;
            }
            logger.info("Sysex Raw Data: " + Arrays.toString(full));
            return full;
        }

        logger.warning("Unhandled MIDI message type: " + msg.getClass().getName());
        return null;
    }
    
    boolean isYamahaHeartbeat(byte[] msg) {
        // Expect F0 43 10 3E 1A 7F F7
        return msg.length == 7 &&
            (msg[0] & 0xFF) == 0xF0 &&
            (msg[1] & 0xFF) == 0x43 &&
            (msg[2] & 0xFF) == 0x10 &&
            (msg[3] & 0xFF) == 0x3E &&
            (msg[4] & 0xFF) == 0x1A &&
            (msg[5] & 0xFF) == 0x7F &&
            (msg[6] & 0xFF) == 0xF7;
    }
}
    