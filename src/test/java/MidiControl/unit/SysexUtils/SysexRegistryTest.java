package MidiControl.unit.SysexUtils;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SysexRegistryTest {

    private SysexMapping makeMapping(
            String group,
            long key,
            int[] addressBytes,
            int[] indexBytes,
            int maxChannels
    ) {
        return new SysexMapping(
                group,              // control group
                0,                  // control_id
                maxChannels,        // max channels
                "testSub",          // sub_control
                0,                  // channel_index
                key,                // key
                addressBytes,       // address_bytes
                indexBytes,         // index_bytes
                0,                  // value
                0,                  // min_value
                1023,               // max_value
                0,                  // default_value
                "test mapping",     // comment
                List.of("F0","43","dd","F7"), // minimal valid pattern
                List.of("F0","43","cc","F7"),
                1                   // priority
        );
    }

    private byte[] sampleM7CL() {
        return new byte[]{
                (byte) 0xF0, 0x43, 0x10, 0x4C,
                0x12, 0x00, 0x7F, 0x1A, 0x23, 0x00,
                0x00, 0x02,
                0x00, 0x00, 0x00, 0x00,
                (byte) 0xF7
        };
    }

    private long m7clKey() {
        byte[] msg = sampleM7CL();
        long k = 0;
        int[] addr = {4,5,6,7,8,9};
        for (int b : addr) k = (k << 8) | (msg[b] & 0x7F);
        return k;
    }

    private byte[] sample01V96i() {
        return new byte[]{
                (byte) 0xF0, 0x43, 0x10, 0x3E,
                0x7F, 0x01, 0x1C, 0x00,
                0x09,
                0x00, 0x00, 0x03, 0x14,
                (byte) 0xF7
        };
    }

    private long v96Key() {
        byte[] msg = sample01V96i();
        long k = 0;
        int[] addr = {4,5,6,7};
        for (int b : addr) k = (k << 8) | (msg[b] & 0x7F);
        return k;
    }

    @Test
    public void testResolve_M7CL() {
        SysexMapping map = makeMapping(
                "m7clGroup",
                m7clKey(),
                new int[]{4,5,6,7,8,9},
                new int[]{10,11},
                56
        );
        SysexRegistry reg = new SysexRegistry(List.of(map));

        SysexMapping out = reg.resolve(sampleM7CL());
        assertNotNull(out);
        assertEquals("m7clGroup", out.getControlGroup());
    }

    @Test
    public void testResolve_01V96i() {
        SysexMapping map = makeMapping(
                "v96Group",
                v96Key(),
                new int[]{4,5,6,7},
                new int[]{8},
                40
        );
        SysexRegistry reg = new SysexRegistry(List.of(map));

        SysexMapping out = reg.resolve(sample01V96i());
        assertNotNull(out);
        assertEquals("v96Group", out.getControlGroup());
    }

    @Test
    public void testReject_InvalidManufacturer() {
        byte[] msg = sample01V96i();
        msg[1] = 0x00; // break Yamaha ID

        SysexMapping map = makeMapping("x", v96Key(), new int[]{4,5,6,7}, new int[]{8}, 40);
        SysexRegistry reg = new SysexRegistry(List.of(map));

        assertNull(reg.resolve(msg));
    }

    @Test
    public void testReject_IndexOutOfRange() {
        byte[] msg = sample01V96i();
        msg[8] = 0x7F; // channel 127

        SysexMapping map = makeMapping("x", v96Key(), new int[]{4,5,6,7}, new int[]{8}, 40);
        SysexRegistry reg = new SysexRegistry(List.of(map));

        assertNull(reg.resolve(msg));
    }
}