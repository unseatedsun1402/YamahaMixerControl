package MidiControl.MidiDeviceManager.Simulator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Transmitter;

import MidiControl.UserInterface.Meter.MeterCategory;
import MidiControl.UserInterface.Meter.MeterSource;

public final class O1V96iSimulatorMidiDevice implements MidiDevice {
    public static final String DEVICE_NAME = "01V96i SIL Simulator";
    private static final Logger logger = Logger.getLogger(O1V96iSimulatorMidiDevice.class.getName());
    private static final DeviceInfo INFO = new DeviceInfo();
    private static final List<Receiver> inputs = new CopyOnWriteArrayList<>();
    private static final MeterStream meterStream = new MeterStream();
    private final List<Receiver> receivers = new CopyOnWriteArrayList<>();
    private final List<Transmitter> transmitters = new CopyOnWriteArrayList<>();
    private volatile boolean open;

    public static MidiDevice.Info getSimulatorInfo() { return INFO; }
    public static boolean isSimulatorInfo(MidiDevice.Info info) { return info == INFO; }
    @Override public MidiDevice.Info getDeviceInfo() { return INFO; }
    @Override public void open() { open = true; }
    @Override public boolean isOpen() { return open; }
    @Override public long getMicrosecondPosition() { return -1; }
    @Override public int getMaxReceivers() { return -1; }
    @Override public int getMaxTransmitters() { return -1; }
    @Override public List<Receiver> getReceivers() { return List.copyOf(receivers); }
    @Override public List<Transmitter> getTransmitters() { return List.copyOf(transmitters); }

    @Override
    public void close() {
        transmitters.forEach(Transmitter::close);
        receivers.forEach(Receiver::close);
        open = false;
    }

    @Override
    public Receiver getReceiver() {
        Receiver receiver = new Receiver() {
            private boolean closed;
            @Override public void send(MidiMessage message, long timeStamp) {
                if (closed) throw new IllegalStateException("01V96i simulator receiver is closed");
                if (message instanceof SysexMessage) respond(message.getMessage());
            }
            @Override public void close() {
                closed = true;
                receivers.remove(this);
            }
        };
        receivers.add(receiver);
        return receiver;
    }

    @Override
    public Transmitter getTransmitter() {
        Transmitter transmitter = new Transmitter() {
            private Receiver receiver;
            private boolean closed;
            @Override public synchronized void setReceiver(Receiver replacement) {
                if (closed) throw new IllegalStateException("01V96i simulator transmitter is closed");
                if (receiver != null) inputs.remove(receiver);
                receiver = replacement;
                if (receiver != null) inputs.add(receiver);
                if (inputs.isEmpty()) meterStream.stop();
            }
            @Override public synchronized Receiver getReceiver() { return receiver; }
            @Override public synchronized void close() {
                if (receiver != null) inputs.remove(receiver);
                receiver = null;
                closed = true;
                transmitters.remove(this);
                if (inputs.isEmpty()) meterStream.stop();
            }
        };
        transmitters.add(transmitter);
        return transmitter;
    }

    private static void respond(byte[] request) {
        if (request.length == 10 && (request[0] & 0xFF) == 0xF0
                && request[1] == 0x43 && (request[2] & 0xF0) == 0x30
                && request[3] == 0x3E && request[4] == 0x7F && request[5] == 0x01
                && request[6] == 0x1C && request[7] == 0x00
                && (request[9] & 0xFF) == 0xF7) {
            byte[] response = new byte[14];
            System.arraycopy(request, 0, response, 0, 9);
            response[2] = (byte) (0x10 | (request[2] & 0x0F));
            response[13] = (byte) 0xF7;
            publishSysex(response);
            return;
        }
        if (request.length != 12 || (request[0] & 0xFF) != 0xF0
                || request[1] != 0x43 || (request[2] & 0xF0) != 0x30
                || request[3] != 0x3E || request[4] != 0x1A || request[5] != 0x21
                || (request[11] & 0xFF) != 0xF7) return;
        int category = request[6] & 0x7F;
        int source = request[7] & 0x7F;
        int start = request[8] & 0x7F;
        int count = ((request[9] & 0x7F) << 7) | (request[10] & 0x7F);
        if (!MeterCategory.isSupportedForModel(0x1A, category)
                || !MeterSource.isSupportedForRequest(0x1A, category, source)) return;
        if (source == 0x7F) {
            logger.warning("01V96i all-point response ordering is not implemented");
            return;
        }
        int channels = switch (category) {
            case 0 -> source == 2 || source == 3 ? 32 : 40;
            case 1, 2 -> 8;
            case 4 -> 2;
            case 5 -> 10;
            case 6 -> 4;
            default -> 0;
        };
        if (start == 0x7F && count == 0) {
            start = 0;
            count = channels;
        } else if (count < 1 || start >= channels || count > channels - start) {
            return;
        }
        int selectedStart = start;
        int selectedCount = count;
        meterStream.start(() -> publishMeters(request, selectedStart, selectedCount));
    }

    private static void publishMeters(byte[] request, int start, int count) {
        byte[] response = new byte[10 + count * 2];
        System.arraycopy(request, 0, response, 0, 9);
        response[2] = (byte) (0x10 | (request[2] & 0x0F));
        response[8] = (byte) start;
        long phase = System.nanoTime() / TimeUnit.MILLISECONDS.toNanos(100);
        for (int i = 0; i < count; i++) {
            int value = (int) Math.round((Math.sin((phase + i * 7) * 0.08) + 1) * 2184);
            response[9 + i * 2] = (byte) (value >> 7);
            response[10 + i * 2] = (byte) (value & 0x7F);
        }
        response[response.length - 1] = (byte) 0xF7;
        publishSysex(response);
    }

    private static void publishSysex(byte[] response) {
        try {
            SysexMessage message = new SysexMessage();
            message.setMessage(response, response.length);
            for (Receiver input : inputs) {
                try {
                    input.send(message, -1);
                } catch (RuntimeException exception) {
                    logger.log(Level.WARNING, "01V96i simulator input receiver failed", exception);
                }
            }
        } catch (InvalidMidiDataException exception) {
            logger.log(Level.SEVERE, "Could not create simulated 01V96i SysEx", exception);
        }
    }

    private static final class DeviceInfo extends MidiDevice.Info {
        private DeviceInfo() {
            super(DEVICE_NAME, "Yamaha", "Virtual 01V96i SysEx meter endpoint", "1.0");
        }
    }
}
