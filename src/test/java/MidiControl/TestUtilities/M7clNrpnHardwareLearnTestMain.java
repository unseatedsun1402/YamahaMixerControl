package MidiControl.TestUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlInstance;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.NrpnUtils.NrpnMappingLoader;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;

public class M7clNrpnHardwareLearnTestMain {

    private static final String SYSEX_RESOURCE = "MidiControl/m7cl_sysex_mappings.json";
    private static final String NRPN_RESOURCE  = "MidiControl/nrpn/m7cl_nrpn_mappings.json";
    private static final String CANONICAL_ID   = "kInputFader.kFader.1";

    private static final int MIDI_CHANNEL = 0;

    // Set these to skip prompts.
    private static final Integer MIDI_OUT_DEVICE_INDEX = null;
    private static final Integer MIDI_IN_DEVICE_INDEX  = null;

    public void learnFaderNrpnFromHardware() throws Exception {
        List<SysexMapping> sysexMappings =
                SysexMappingLoader.loadMappingsFromResource(SYSEX_RESOURCE);

        List<NrpnMapping> nrpnMappings =
                NrpnMappingLoader.loadFromResource(NRPN_RESOURCE);

        CanonicalRegistry registry =
                new CanonicalRegistry(sysexMappings, new SysexParser(sysexMappings));

        registry.attachNrpnMappings(nrpnMappings);

        ControlInstance ci = registry.resolve(CANONICAL_ID);

        NrpnMapping nrpn = ci.getNrpn().get();

        printControl(ci, nrpn);

        MidiDevice outDev = selectDevice(true, false, MIDI_OUT_DEVICE_INDEX);
        MidiDevice inDev  = selectDevice(false, true, MIDI_IN_DEVICE_INDEX);

        outDev.open();
        inDev.open();

        Receiver out = outDev.getReceiver();
        Transmitter in = inDev.getTransmitter();

        CaptureReceiver capture = new CaptureReceiver();
        in.setReceiver(capture);

        System.out.println();
        System.out.println("Sending value 0 using current production NRPN builder...");
        List<byte[]> zero = nrpn.buildNrpnBytes(ci.getMin());
        printMessages(zero);
        send(out, zero);

        System.out.println();
        System.out.println("Move the desk fader to MAX.");
        System.out.println("Press SPACE or ENTER when done capturing.");

        waitForKey();

        List<Cc> events = capture.snapshot();

        System.out.println();
        System.out.println("Captured CC:");
        for (Cc e : events) {
            System.out.println("  " + e);
        }

        Learned learnt = learnLastMatchingNrpn(events, nrpn);

        System.out.println();
        System.out.println("Learned:");
        System.out.println("  NRPN MSB      = " + learnt.nrpnMsb);
        System.out.println("  NRPN LSB      = " + learnt.nrpnLsb);
        System.out.println("  Data MSB CC6  = " + valueOrMissing(learnt.dataMsb));
        System.out.println("  Data LSB CC38 = " + valueOrMissing(learnt.dataLsb));

        printInterpretations(ci, learnt);

        out.close();
        in.close();
        outDev.close();
        inDev.close();
    }

    private static void printControl(ControlInstance ci, NrpnMapping nrpn) {
        System.out.println("Control:");
        System.out.println("  canonicalId = " + ci.getCanonicalId());
        System.out.println("  group       = " + ci.getGroup());
        System.out.println("  subcontrol  = " + ci.getSubcontrol());
        System.out.println("  index       = " + ci.getIndex());
        System.out.println("  min         = " + ci.getMin());
        System.out.println("  max         = " + ci.getMax());
        System.out.println("  resolution  = 0x" + String.format("%02X", ci.getResolution() & 0xFF));
        System.out.println("  nrpn msb    = " + nrpn.msbInt());
        System.out.println("  nrpn lsb    = " + nrpn.lsbInt());
    }

    private static MidiDevice selectDevice(
            boolean needReceiver,
            boolean needTransmitter,
            Integer fixedIndex
    ) throws Exception {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        System.out.println();
        System.out.println(needReceiver ? "MIDI OUT devices:" : "MIDI IN devices:");

        for (int i = 0; i < infos.length; i++) {
            MidiDevice d = MidiSystem.getMidiDevice(infos[i]);

            boolean ok = true;

            if (needReceiver) {
                ok = ok && d.getMaxReceivers() != 0;
            }

            if (needTransmitter) {
                ok = ok && d.getMaxTransmitters() != 0;
            }

            if (ok) {
                System.out.println(
                        "  [" + i + "] " +
                        infos[i].getName() + " | " +
                        infos[i].getDescription()
                );
            }
        }

        int index;

        if (fixedIndex != null) {
            index = fixedIndex;
        } else {
            System.out.print("Select device index: ");
            Scanner scanner = new Scanner(System.in);
            index = Integer.parseInt(scanner.nextLine().trim());
        }

        return MidiSystem.getMidiDevice(infos[index]);
    }

    private static void send(Receiver out, List<byte[]> messages) throws Exception {
        for (byte[] b : messages) {
            ShortMessage sm = new ShortMessage();

            sm.setMessage(
                    ShortMessage.CONTROL_CHANGE,
                    MIDI_CHANNEL,
                    b[1] & 0x7F,
                    b[2] & 0x7F
            );

            System.out.println("  OUT " + hex(b));
            out.send(sm, -1);

            Thread.sleep(2);
        }
    }

    private static void waitForKey() throws Exception {
        int ch;

        do {
            ch = System.in.read();
        } while (ch != ' ' && ch != '\n' && ch != '\r');
    }

    private static Learned learnLastMatchingNrpn(List<Cc> events, NrpnMapping nrpn) {
        int currentMsb = -1;
        int currentLsb = -1;

        Learned learnt = new Learned();

        for (Cc e : events) {
            if (e.controller == 99) {
                currentMsb = e.value;
            } else if (e.controller == 98) {
                currentLsb = e.value;
            } else if (e.controller == 6) {
                if (currentMsb == nrpn.msbInt() && currentLsb == nrpn.lsbInt()) {
                    learnt.nrpnMsb = currentMsb;
                    learnt.nrpnLsb = currentLsb;
                    learnt.dataMsb = e.value;
                    learnt.dataLsb = null;
                }
            } else if (e.controller == 38) {
                if (learnt.dataMsb != null &&
                    currentMsb == nrpn.msbInt() &&
                    currentLsb == nrpn.lsbInt()) {
                    learnt.dataLsb = e.value;
                }
            }
        }

        return learnt;
    }

    private static void printInterpretations(ControlInstance ci, Learned learnt) {
        if (learnt.dataMsb == null) {
            System.out.println();
            System.out.println("No matching NRPN data-entry CC6 was captured.");
            return;
        }

        int expected = ci.getMax();

        int sevenBitAsControl =
                scale(learnt.dataMsb, 0, 127, ci.getMin(), ci.getMax());

        Integer rawCombined = null;
        Integer fourteenBitAsControl = null;

        if (learnt.dataLsb != null) {
            rawCombined = ((learnt.dataMsb & 0x7F) << 7) | (learnt.dataLsb & 0x7F);

            fourteenBitAsControl =
                    scale(rawCombined, 0, 16383, ci.getMin(), ci.getMax());
        }

        System.out.println();
        System.out.println("Interpretations against expected max " + expected + ":");

        printScore("7-bit CC6 scaled to control range", sevenBitAsControl, expected);

        if (rawCombined != null) {
            printScore("Raw combined CC6/CC38 value", rawCombined, expected);
            printScore("14-bit CC6/CC38 scaled to control range", fourteenBitAsControl, expected);
        }

        String best = bestFit(expected, sevenBitAsControl, rawCombined, fourteenBitAsControl);

        System.out.println();
        System.out.println("Best fit: " + best);
    }

    private static void printScore(String label, int value, int expected) {
        int delta = Math.abs(expected - value);
        double pct = expected == 0 ? 0.0 : (delta * 100.0) / expected;

        System.out.printf(
                "  %-42s = %5d   delta=%5d   %.2f%%%n",
                label,
                value,
                delta,
                pct
        );
    }

    private static String bestFit(
            int expected,
            Integer seven,
            Integer raw,
            Integer fourteen
    ) {
        String bestName = "7-bit CC6 scaled";
        int bestDelta = Math.abs(expected - seven);

        if (raw != null) {
            int d = Math.abs(expected - raw);
            if (d < bestDelta) {
                bestDelta = d;
                bestName = "raw combined CC6/CC38";
            }
        }

        if (fourteen != null) {
            int d = Math.abs(expected - fourteen);
            if (d < bestDelta) {
                bestName = "14-bit CC6/CC38 scaled";
            }
        }

        return bestName;
    }

    private static int scale(int value, int inMin, int inMax, int outMin, int outMax) {
        if (value <= inMin) return outMin;
        if (value >= inMax) return outMax;

        double n = (value - inMin) / (double) (inMax - inMin);

        return (int) Math.round(outMin + n * (outMax - outMin));
    }

    private static void printMessages(List<byte[]> messages) {
        for (byte[] b : messages) {
            System.out.println("  " + hex(b));
        }
    }

    private static String valueOrMissing(Integer value) {
        return value == null ? "<missing>" : String.valueOf(value);
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < b.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", b[i] & 0xFF));
        }

        return sb.toString();
    }

    private static final class CaptureReceiver implements Receiver {
        private final CopyOnWriteArrayList<Cc> events = new CopyOnWriteArrayList<>();

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!(message instanceof ShortMessage sm)) {
                return;
            }

            if (sm.getCommand() != ShortMessage.CONTROL_CHANGE) {
                return;
            }

            Cc cc = new Cc(sm.getChannel(), sm.getData1(), sm.getData2());
            events.add(cc);

            System.out.println("  IN  " + cc);
        }

        @Override
        public void close() {
            events.clear();
        }

        List<Cc> snapshot() {
            return new ArrayList<>(events);
        }
    }

    private static final class Cc {
        final int channel;
        final int controller;
        final int value;

        Cc(int channel, int controller, int value) {
            this.channel = channel;
            this.controller = controller;
            this.value = value;
        }

        @Override
        public String toString() {
            return "ch=" + channel +
                   " cc=" + controller +
                   " value=" + value +
                   " hex=" + hex(new byte[] {
                           (byte) (0xB0 | (channel & 0x0F)),
                           (byte) (controller & 0x7F),
                           (byte) (value & 0x7F)
                   });
        }
    }

    private static final class Learned {
        int nrpnMsb = -1;
        int nrpnLsb = -1;
        Integer dataMsb;
        Integer dataLsb;
    }

    
    public static void main(String[] args) throws Exception {
        new M7clNrpnHardwareLearnTestMain().learnFaderNrpnFromHardware();
    }

}