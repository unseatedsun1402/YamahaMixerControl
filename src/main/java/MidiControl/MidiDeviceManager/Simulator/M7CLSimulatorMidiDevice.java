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

public final class M7CLSimulatorMidiDevice implements MidiDevice {

    public static final String DEVICE_NAME = "M7CL SIL Simulator";
    private static final int MODEL_NUMBER = 0x11;
    private static final int INPUT_CHANNELS = 56;
    private static final Logger logger =
            Logger.getLogger(M7CLSimulatorMidiDevice.class.getName());
    private static final DeviceInfo DEVICE_INFO = new DeviceInfo();
    private static final List<Receiver> inputReceivers = new CopyOnWriteArrayList<>();
    private static final MeterStream meterStream = new MeterStream();

    private final List<Receiver> deviceReceivers = new CopyOnWriteArrayList<>();
    private final List<Transmitter> deviceTransmitters = new CopyOnWriteArrayList<>();
    private volatile boolean open;

    public static MidiDevice.Info getSimulatorInfo() {
        return DEVICE_INFO;
    }

    public static boolean isSimulatorInfo(MidiDevice.Info info) {
        return info == DEVICE_INFO;
    }

    public M7CLSimulatorMidiDevice() {}

    @Override
    public MidiDevice.Info getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Override
    public void open() {
        open = true;
    }

    @Override
    public void close() {
        for (Transmitter transmitter : deviceTransmitters) {
            transmitter.close();
        }
        for (Receiver receiver : deviceReceivers) {
            receiver.close();
        }
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return -1;
    }

    @Override
    public Receiver getReceiver() {
        DeviceReceiver receiver = new DeviceReceiver();
        deviceReceivers.add(receiver);
        return receiver;
    }

    @Override
    public List<Receiver> getReceivers() {
        return List.copyOf(deviceReceivers);
    }

    @Override
    public Transmitter getTransmitter() {
        DeviceTransmitter transmitter = new DeviceTransmitter();
        deviceTransmitters.add(transmitter);
        return transmitter;
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return List.copyOf(deviceTransmitters);
    }

    private static void processOutput(MidiMessage message) {
        if (!(message instanceof SysexMessage)) {
            return;
        }

        byte[] bytes = message.getMessage();
        if (isM7clFaderRequest(bytes)) {
            publishM7clFaderResponse(bytes);
            return;
        }

        if (bytes.length != 12
                || (bytes[0] & 0xFF) != 0xF0
                || (bytes[1] & 0xFF) != 0x43
                || (bytes[2] & 0xF0) != 0x30
                || (bytes[3] & 0xFF) != 0x3E
                || (bytes[4] & 0x7F) != MODEL_NUMBER
                || (bytes[5] & 0x7F) != 0x21
                || (bytes[bytes.length - 1] & 0xFF) != 0xF7) {
            return;
        }

        int start = bytes[8] & 0x7F;
        int count = ((bytes[9] & 0x7F) << 7) | (bytes[10] & 0x7F);
        int category = bytes[6] & 0x7F;
        int source = bytes[7] & 0x7F;
        int values;
        int responseStart = start;
        if (category == 0) {
            if (start == 0x7F && count == 0) {
                responseStart = 0;
                count = INPUT_CHANNELS;
            } else if (count < 1 || start >= INPUT_CHANNELS || count > INPUT_CHANNELS - start) {
                return;
            }
            if (source > 8 && source != 0x7F) {
                return;
            }
            values = count * (source == 0x7F ? 9 : 1);
        } else if (category == 0x10 && start == 0 && count == 1
                && (source == 0 || source == 0x7F)) {
            values = source == 0x7F ? 20 : 4;
        } else {
            return;
        }
        int channel = bytes[2] & 0x0F;
        int selectedStart = responseStart;
        meterStream.start(() -> publishMeters(channel, category, source, selectedStart, values));
    }

    private static boolean isM7clFaderRequest(byte[] bytes) {
        return bytes.length == 13
                && (bytes[0] & 0xFF) == 0xF0
                && (bytes[1] & 0xFF) == 0x43
                && (bytes[2] & 0xF0) == 0x30
                && (bytes[3] & 0xFF) == 0x3E
                && (bytes[4] & 0x7F) == MODEL_NUMBER
                && (bytes[5] & 0x7F) == 0x01
                && (bytes[6] & 0x7F) == 0x00
                && (bytes[7] & 0x7F) == 0x32
                && (bytes[8] & 0x7F) == 0x00
                && (bytes[9] & 0x7F) == 0x00
                && (bytes[12] & 0xFF) == 0xF7;
    }

    private static void publishM7clFaderResponse(byte[] request) {
        byte[] response = new byte[request.length + 5];
        System.arraycopy(request, 0, response, 0, request.length - 1);
        response[2] = (byte) (0x10 | (request[2] & 0x0F));
        response[response.length - 1] = (byte) 0xF7;
        publishSysex(response);
    }

    private static void publishMeters(int channel, int category, int source, int start, int count) {
        byte[] response = new byte[10 + count];
        response[0] = (byte) 0xF0;
        response[1] = 0x43;
        response[2] = (byte) (0x10 | channel);
        response[3] = 0x3E;
        response[4] = MODEL_NUMBER;
        response[5] = 0x21;
        response[6] = (byte) category;
        response[7] = (byte) source;
        response[8] = (byte) start;

        long phase = System.nanoTime() / TimeUnit.MILLISECONDS.toNanos(100);
        for (int i = 0; i < count; i++) {
            double level = Math.sin((phase + i * 7) * 0.08);
            response[9 + i] = category == 0x10 ? 0 : (byte) Math.round((level + 1.0) * 55.0);
        }
        response[response.length - 1] = (byte) 0xF7;

        publishSysex(response);
    }

    private static void publishSysex(byte[] bytes) {
        try {
            SysexMessage message = new SysexMessage();
            message.setMessage(bytes, bytes.length);
            for (Receiver receiver : inputReceivers) {
                try {
                    receiver.send(message, -1);
                } catch (RuntimeException exception) {
                    logger.log(Level.WARNING, "M7CL simulator input receiver failed", exception);
                }
            }
        } catch (InvalidMidiDataException exception) {
            logger.log(Level.SEVERE, "Could not create valid simulated M7CL SysEx", exception);
        }
    }

    private final class DeviceReceiver implements Receiver {
        private volatile boolean closed;

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (closed) {
                throw new IllegalStateException("M7CL simulator receiver is closed");
            }
            processOutput(message);
        }

        @Override
        public void close() {
            closed = true;
            deviceReceivers.remove(this);
        }
    }

    private final class DeviceTransmitter implements Transmitter {
        private volatile Receiver receiver;
        private volatile boolean closed;

        @Override
        public void setReceiver(Receiver receiver) {
            if (closed) {
                throw new IllegalStateException("M7CL simulator transmitter is closed");
            }
            Receiver previous = this.receiver;
            if (previous != null) {
                inputReceivers.remove(previous);
            }
            this.receiver = receiver;
            if (receiver != null) {
                inputReceivers.add(receiver);
            }
            if (inputReceivers.isEmpty()) meterStream.stop();
        }

        @Override
        public Receiver getReceiver() {
            return receiver;
        }

        @Override
        public void close() {
            closed = true;
            Receiver previous = receiver;
            receiver = null;
            if (previous != null) {
                inputReceivers.remove(previous);
            }
            deviceTransmitters.remove(this);
            if (inputReceivers.isEmpty()) meterStream.stop();
        }
    }

    private static final class DeviceInfo extends MidiDevice.Info {
        private DeviceInfo() {
            super(DEVICE_NAME, "Yamaha", "Virtual M7CL SysEx meter endpoint", "1.0");
        }
    }
}
