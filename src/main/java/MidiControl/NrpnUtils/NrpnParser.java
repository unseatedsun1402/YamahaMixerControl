package MidiControl.NrpnUtils;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.sound.midi.ShortMessage;

public class NrpnParser {
    private final NrpnRegistry registry = NrpnRegistry.getInstance();
    private final Logger logger = Logger.getLogger(NrpnParser.class.getName());
    
    private int currentMsb = -1;
    private int currentLsb = -1;
    private int currentValueMsb = -1;
    private int currentValueLsb = -1;

    private boolean hasMsb = false;
    private boolean hasLsb = false;
    private boolean hasValueMsb = false;
    private boolean hasValueLsb = false;

    public void parse(ShortMessage msg) {
        int data1 = msg.getData1();
        int data2 = msg.getData2();

        switch (data1) {
            case 99 -> {
                currentMsb = data2;
                hasMsb = true;
            }
            case 98 -> {
                currentLsb = data2;
                hasLsb = true;
            }
            case 6 -> {
                currentValueMsb = data2;
                hasValueMsb = true;
            }
            case 38 -> {
                currentValueLsb = data2;
                hasValueLsb = true;
            }
        }

        if (hasMsb && hasLsb && hasValueMsb) {
            int value = (currentValueMsb << 7) | (hasValueLsb ? currentValueLsb : 0);
            handleValue(value);

            // Reset
            hasMsb = hasLsb = hasValueMsb = hasValueLsb = false;
            currentMsb = currentLsb = currentValueMsb = currentValueLsb = -1;
        }
    }

     public void parse(byte[] msg) {
        int data1 = msg[1];
        int data2 = msg[2];

        switch (data1) {
            case 99 -> {
                currentMsb = data2;
                hasMsb = true;
            }
            case 98 -> {
                currentLsb = data2;
                hasLsb = true;
            }
            case 6 -> {
                currentValueMsb = data2;
                hasValueMsb = true;
            }
            case 38 -> {
                currentValueLsb = data2;
                hasValueLsb = true;
            }
        }

        if (hasMsb && hasLsb && hasValueMsb) {
            logger.fine("Valid Nrpn received " + currentMsb + " " + currentLsb);
            int value = (currentValueMsb << 7) | (hasValueLsb ? currentValueLsb : 0);
            handleValue(value);

            // Reset
            hasMsb = hasLsb = hasValueMsb = hasValueLsb = false;
            currentMsb = currentLsb = currentValueMsb = currentValueLsb = -1;
        }
    }


    private void handleValue(int value) {
        if (currentMsb < 0 || currentLsb < 0) return;

        var resolved = NrpnRegistry.resolve(currentMsb, currentLsb);
        if (resolved.isPresent()) {
            var mapping = resolved.get();

            if (dispatchTarget != null) {
                logger.info(String.format("Dispatching NRPN: %s, Value=%d", mapping.channelIndex(), value));
                dispatchTarget.handleResolvedNRPN(mapping, value);
            }
            else {
                logger.warning("Dispatch target not set. Cannot dispatch NRPN.");
            }

            logger.info(String.format("Resolved NRPN:Channel %s, Value=%d", mapping.channelIndex(), value));
        }
    
            
        else {
            logger.warning(String.format("Unknown NRPN: MSB=0x%02X, LSB=0x%02X, Value=%d", currentMsb, currentLsb, value));
        }

        // Reset after handling
        currentMsb = -1;
        currentLsb = -1;
    }

    private static NRPNDispatchTarget dispatchTarget;

    public static void setDispatchTarget(NRPNDispatchTarget target) {
        dispatchTarget = target;
    }

}
