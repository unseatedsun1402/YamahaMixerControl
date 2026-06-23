package MidiControl.unit.Control;

import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.ControlListener;
import MidiControl.NrpnUtils.NrpnMapping;
import MidiControl.SysexUtils.SysexMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ControlInstanceTest {
    @Test
    void testCreateAnInstance(){
        String testID = "Group.Test.1";
        assertDoesNotThrow(() -> new ControlInstance(testID, 0, null, null));
    }

    @Test
    void testAddAListener(){
        String testID = "Group.Test.1";
        ControlInstance testInstance = new ControlInstance(testID, 0, null, null);
        ControlListener testListener = new ControlListener() {
            @Override
            public void onControlChanged(ControlInstance instance, int newValue) {
                System.out.println("Fired listener");
                return;
            }
        };

        assertDoesNotThrow(() -> testInstance.addListener(testListener));
    }

    @Test
    void testGetCanonicalId(){
        String testID = "Group.Test.1";
        ControlInstance testInstance = new ControlInstance(testID, 0, new SysexMapping(), new NrpnMapping("1","2",testID,"CC6_ONLY"));
        List<byte[]> message = testInstance.buildNrpnChange(0);
        
        byte[] messagepart1 = {-80,99,0x01}; // status, cc ch, val
        byte[] messagepart2 = {-80,98,0x02};
        byte[] messagepart3 = {-80,6,0x00};

        assertArrayEquals(message.get(0), messagepart1);
        assertArrayEquals(message.get(1), messagepart2);
        assertArrayEquals(message.get(2), messagepart3);
    }

    @Test
    void testEnableDebug(){
        assertDoesNotThrow(() -> ControlInstance.enableDebug());
    }
}