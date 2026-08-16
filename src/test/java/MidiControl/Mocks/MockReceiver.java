package MidiControl.Mocks;

import java.util.function.Consumer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

public class MockReceiver implements Receiver {

    public Consumer<byte[]> inboundHandler;

    public void setInboundHandler(Consumer<byte[]> handler) {
        this.inboundHandler = handler;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {

        byte[] data = message.getMessage();

        if (isProbe(data)) {
            byte[] response = buildDeskResponse(data);
            inboundHandler.accept(response);
        }
    }

    @Override
    public void close() {}

    private boolean isProbe(byte[] msg) {
        return msg.length > 3 &&
               msg[0] == (byte)0xF0 &&
               msg[1] == (byte)0x43 &&
               (msg[2] & 0xF0) == 0x30; // Yamaha request 3n
    }

    private byte[] buildDeskResponse(byte[] probe) {
        int channel = probe[2] & 0x0F;

        return new byte[] {
            (byte)0xF0,
            (byte)0x43,
            (byte)(0x20 | channel), // Yamaha response 2n
            0x62, 0x01, 0x00, 0x00, 0x00,
            (byte)0xF7
        };
    }
}
