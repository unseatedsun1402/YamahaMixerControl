package MidiControl.TestUtilities;

import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;

import javax.sound.midi.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class M7clNameSysexLearnRawMain {

    private static final String SYSEX_RESOURCE =
            "MidiControl/m7cl_sysex_mappings.json";

    private static final String CONTROL_GROUP = "kNameInputChannel";
    private static final String SHORT1 = "kNameShort1";
    private static final String SHORT2 = "kNameShort2";

    private static final int CHANNEL_INDEX = 0;

    private static final Integer MIDI_OUT_DEVICE_INDEX = null;
    private static final Integer MIDI_IN_DEVICE_INDEX  = null;

    public static void main(String[] args) throws Exception {
        new M7clNameSysexLearnRawMain().run();
    }

    public void run() throws Exception {

        List<SysexMapping> mappings =
                SysexMappingLoader.loadMappingsFromResource(SYSEX_RESOURCE);

        SysexMapping short1 = findMapping(mappings, CONTROL_GROUP, SHORT1);
        SysexMapping short2 = findMapping(mappings, CONTROL_GROUP, SHORT2);

        printMapping("SHORT1", short1);
        printMapping("SHORT2", short2);

        MidiDevice outDev = selectDevice(true, false, MIDI_OUT_DEVICE_INDEX);
        MidiDevice inDev  = selectDevice(false, true, MIDI_IN_DEVICE_INDEX);

        outDev.open();
        inDev.open();

        Receiver out = outDev.getReceiver();
        Transmitter in = inDev.getTransmitter();

        CaptureReceiver capture = new CaptureReceiver();
        in.setReceiver(capture);

        System.out.println("\nSending SysEx requests...");

        send(out, buildRequest(short1));
        Thread.sleep(50);
        send(out, buildRequest(short2));

        System.out.println("\nWait for response, then press ENTER");
        System.in.read();

        List<byte[]> responses = capture.snapshot();

        System.out.println("\nCaptured responses: " + responses.size());

        for (int i = 0; i < responses.size(); i++) {
            System.out.println("\n==== RESPONSE " + (i + 1) + " ====");
            dumpRawAsAscii(responses.get(i));
        }

        out.close();
        in.close();
        outDev.close();
        inDev.close();
    }

    private static SysexMapping findMapping(List<SysexMapping> mappings,
                                            String group,
                                            String sub) {
        return mappings.stream()
                .filter(m -> group.equals(m.getControlGroup()))
                .filter(m -> sub.equals(m.getSubControl()))
                .findFirst()
                .orElseThrow();
    }

    private static void printMapping(String label, SysexMapping m) {
        System.out.println("\n" + label + ":");
        System.out.println("  group=" + m.getControlGroup());
        System.out.println("  sub=" + m.getSubControl());
        System.out.println("  value=" + m.getValue());
    }

    private static byte[] buildRequest(SysexMapping mapping) {

        List<?> fmt = mapping.getParameter_request_format();

        List<Byte> out = new ArrayList<>();

        int ccCounter = 0;

        for (Object obj : fmt) {

            String token = String.valueOf(obj).trim();

            Integer num = parseNumber(token);

            if (num != null) {
                out.add((byte)(num & 0xFF));
                continue;
            }

            switch (token) {

                case "3n" -> out.add((byte) 0x30);

                case "cc" -> {
                    if (ccCounter == 0) {
                        out.add((byte)0x00);
                    } else {
                        out.add((byte)(CHANNEL_INDEX & 0x7F));
                    }
                    ccCounter++;
                }

                default -> throw new RuntimeException("Unknown token: " + token);
            }
        }

        byte[] arr = new byte[out.size()];
        for (int i = 0; i < out.size(); i++) arr[i] = out.get(i);

        return arr;
    }

    private static Integer parseNumber(String s) {
        try {
            if (s.startsWith("0x")) {
                return Integer.parseInt(s.substring(2), 16);
            }
            if (s.matches("\\d+")) {
                return Integer.parseInt(s);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void send(Receiver out, byte[] data) throws Exception {

        System.out.println("\nOUT: " + hex(data));

        SysexMessage msg = new SysexMessage();
        msg.setMessage(data, data.length);

        out.send(msg, -1);
    }

    /* ========================================================= */
    /* ====================== DECODE ============================ */
    /* ========================================================= */

    private static void dumpRawAsAscii(byte[] response) {

        System.out.println("RAW HEX:");
        System.out.println("  " + hex(response));

        if (response.length < 3) return;

        byte[] payload = Arrays.copyOfRange(response, 1, response.length - 1);

        System.out.println("PAYLOAD HEX:");
        System.out.println("  " + hex(payload));

        System.out.println("PRINTABLE ASCII:");
        System.out.println("  [" + printableAscii(payload) + "]");

        System.out.println("UTF-8:");
        System.out.println("  [" + new String(payload, StandardCharsets.UTF_8) + "]");

        if (payload.length >= 5) {

            byte[] tail = Arrays.copyOfRange(payload, payload.length - 5, payload.length);

            System.out.println("TAIL(5) HEX:");
            System.out.println("  " + hex(tail));

            System.out.println("TAIL ASCII:");
            System.out.println("  [" + printableAscii(tail) + "]");

            System.out.println("TAIL +32 shift:");
            System.out.println("  [" + shiftAscii(tail, 32) + "]");

            System.out.println("TAIL +64 shift:");
            System.out.println("  [" + shiftAscii(tail, 64) + "]");

            System.out.print("TAIL DECIMAL: [");
            for (int i = 0; i < tail.length; i++) {
                if (i > 0) System.out.print(", ");
                System.out.print(tail[i] & 0xFF);
            }
            System.out.println("]");
        }
    }

    private static String printableAscii(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte v : b) {
            int x = v & 0xFF;
            sb.append((x >= 32 && x <= 126) ? (char)x : '.');
        }
        return sb.toString();
    }

    private static String shiftAscii(byte[] b, int offset) {
        StringBuilder sb = new StringBuilder();
        for (byte v : b) {
            int x = (v & 0xFF) + offset;
            sb.append((x >= 32 && x <= 126) ? (char)x : '.');
        }
        return sb.toString();
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(String.format("%02X", b[i] & 0xFF));
        }
        return sb.toString();
    }

    /* ========================================================= */

    private static MidiDevice selectDevice(boolean needOut,
                                           boolean needIn,
                                           Integer fixed) throws Exception {

        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (int i = 0; i < infos.length; i++) {
            MidiDevice d = MidiSystem.getMidiDevice(infos[i]);

            boolean ok = true;

            if (needOut) ok &= d.getMaxReceivers() != 0;
            if (needIn)  ok &= d.getMaxTransmitters() != 0;

            if (ok) {
                System.out.println("[" + i + "] "
                        + infos[i].getName());
            }
        }

        int idx = (fixed != null)
                ? fixed
                : Integer.parseInt(new Scanner(System.in).nextLine());

        return MidiSystem.getMidiDevice(infos[idx]);
    }

    private static final class CaptureReceiver implements Receiver {

        private final CopyOnWriteArrayList<byte[]> data =
                new CopyOnWriteArrayList<>();

        @Override
        public void send(MidiMessage msg, long timeStamp) {

            byte[] raw = msg.getMessage();

            if ((raw[0] & 0xFF) == 0xF0) {
                data.add(raw.clone());
                System.out.println("IN: " + hex(raw));
            }
        }

        @Override
        public void close() {}

        List<byte[]> snapshot() {
            return new ArrayList<>(data);
        }
    }
}