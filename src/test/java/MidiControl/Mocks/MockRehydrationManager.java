package MidiControl.Mocks;

import MidiControl.Controls.ControlInstance;
import MidiControl.MidiDeviceManager.DeskDiscovery.ProbeCallback;
import MidiControl.Server.Rehydration.RehydrationManager;

public class MockRehydrationManager extends RehydrationManager {

    public int respondChannel = -1;   // channel that should succeed
    public ControlInstance fakeInstance = new ControlInstance("", 0, null, null); // minimal stub
    public int probeCallCount = 0;

    @Override
    public void probe(String canonicalId, long timeoutMs, int midi_channel, ProbeCallback callback) {
        probeCallCount ++;
        fakeInstance = new ControlInstance(canonicalId,0,null,null);
        // Simulate async behaviour: only respond on the chosen channel
        if (midi_channel == respondChannel) {
            callback.onProbeSuccess(fakeInstance, midi_channel);
        }
        // All other channels do nothing (timeout)
    }
}

