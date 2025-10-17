package MidiControl.MidiDeviceManager;

import java.util.logging.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

import MidiControl.MidiInputReceiver;

public class TransmitterWrapper implements MidiInput {
    private final MidiDevice device;
    private Transmitter transmitter;
    private MidiInputReceiver inputReceiver;
    private final ConcurrentLinkedQueue<MidiMessage> inputBuffer;

    public TransmitterWrapper(MidiDevice device, ConcurrentLinkedQueue<MidiMessage> inputBuffer) throws MidiUnavailableException {
        this.device = device;
        this.inputBuffer = inputBuffer;
        setup();
    }

    private void setup() throws MidiUnavailableException {
        if (!device.isOpen()) {
            device.open();
            Logger.getLogger(TransmitterWrapper.class.getName()).log(Level.INFO, "Opened MIDI device for transmitter.");
        }

        transmitter = device.getTransmitter();

        if (inputReceiver != null) {
            inputReceiver.close();
            Logger.getLogger(TransmitterWrapper.class.getName()).log(Level.INFO, "Restarting existing MidiInputReceiver.");
        }

        inputReceiver = new MidiInputReceiver(inputBuffer);
        transmitter.setReceiver(inputReceiver);
        Logger.getLogger(TransmitterWrapper.class.getName()).log(Level.INFO, "Transmitter set and inputReceiver attached.");
    }

    @Override
    public void setReceiver(Receiver receiver) {
        transmitter.setReceiver(receiver);
    }

    @Override
    public void close() {
        if (transmitter != null) {
            transmitter.close();
        }
        if (inputReceiver != null) {
            inputReceiver.close();
        }
        if (device.isOpen()) {
            device.close();
        }
        Logger.getLogger(TransmitterWrapper.class.getName()).log(Level.INFO, "Closed transmitter and device.");
    }

    public Transmitter getRawTransmitter() {
        return this.transmitter;
    }
}
