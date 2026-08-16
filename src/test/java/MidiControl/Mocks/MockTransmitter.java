package MidiControl.Mocks;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

public class MockTransmitter implements Transmitter {

    private Receiver receiver;

    public MockTransmitter(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public Receiver getReceiver() {
        return receiver;
    }

    public void send(MidiMessage message, long timeStamp) {
        receiver.send(message, timeStamp);
    }

    @Override
    public void close() {}
}
