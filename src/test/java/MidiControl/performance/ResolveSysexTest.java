package MidiControl.performance;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import MidiControl.Mocks.FakeSysexMapping;
import MidiControl.SysexUtils.SysexMapping;
import MidiControl.SysexUtils.SysexMappingLoader;
import MidiControl.SysexUtils.SysexParser;
import MidiControl.SysexUtils.SysexRegistry;
import MidiControl.SystemTools.NativeLoader;

public class ResolveSysexTest {
    @Test
    void testSlowVsFastPathPerformanceM7CL() {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/m7cl_sysex_mappings.json");
        boolean NATIVE = NativeLoader.loadLibrary("native_sysex.dll");
        SysexRegistry registry = new SysexRegistry(mappings);
        SysexParser parser = new SysexParser(mappings);
        int iterations = 50_000;

        byte[] message = FakeSysexMapping.testRequest();

        Logger log = Logger.getLogger("PerfTest");

        // --- Slow path baseline ---
        long slowStart = System.nanoTime();
        SysexMapping slowResult = null;
        for (int i = 0; i < iterations; i++) {
            slowResult = registry.resolve(message);
        }
        long slowEnd = System.nanoTime();

        assertNotNull(slowResult);
        assertEquals("kInputHA", slowResult.getControlGroup());

        long slowTime = slowEnd - slowStart;
        log.info("Slow path (Java) "+iterations+ " iterations: " + slowTime / 1_000_000 + " ms");

        // --- Fast path (native) ---
        if (!NATIVE) {
            log.warning("Native resolver unavailable — skipping fast path timing");
            return;
        }

        long fastStart = System.nanoTime();
        SysexMapping fastResult = null;
        for (int i = 0; i < iterations; i++) {
            fastResult = registry.resolveFast(message);
        }
        long fastEnd = System.nanoTime();

        assertNotNull(fastResult);
        assertEquals("kInputHA", fastResult.getControlGroup());

        long fastTime = fastEnd - fastStart;
        log.info("Fast path (native) "+iterations+ " iterations: " + fastTime / 1_000_000 + " ms");

        // --- Sanity check: fast must be faster ---
        assertTrue(fastTime <= slowTime * 0.95,
            "Fast path should be significantly faster than slow path");
    }

    @Test
    void testSlowVsFastPathPerformance01V96i() {
        List<SysexMapping> mappings = SysexMappingLoader.loadMappingsFromResource("MidiControl/01v96i_sysex_mappings.json");
        SysexRegistry registry = new SysexRegistry(mappings);
        SysexParser parser = new SysexParser(mappings);
        boolean NATIVE = NativeLoader.loadLibrary("native_sysex.dll");
        int iterations = 50_000;

        byte[] message = new byte[] {
            (byte)0xF0, // 240 start
            (byte)0x43, // 67 Yamaha manufacturer
            (byte)0x10, // "1n" device/model id (use 0x10 as in your other messages)
            (byte)0x3E, // 62
            (byte)0x7F, // address byte 4  -> 127 (0x7F)
            (byte)0x01, // address byte 5  -> 1   (0x01)
            (byte)0x1C, // address byte 6  -> 28  (0x1C)
            (byte)0x00, // address byte 7  -> 0   (0x00)
            (byte)0x00, // "cc" index byte -> channel/index 0
            (byte)0x00, // "dd" data byte 1
            (byte)0x00, // "dd" data byte 2
            (byte)0x00, // "dd" data byte 3
            (byte)0x00, // "dd" data byte 4
            (byte)0xF7
        };


        Logger log = Logger.getLogger("PerfTest");

        // --- Slow path baseline ---
        long slowStart = System.nanoTime();
        SysexMapping slowResult = null;
        for (int i = 0; i < iterations; i++) {
            slowResult = registry.resolve(message);
        }
        long slowEnd = System.nanoTime();

        assertNotNull(slowResult);
        assertEquals("kInputFader", slowResult.getControlGroup());

        long slowTime = slowEnd - slowStart;
        log.info("Slow path (Java) "+iterations+ " iterations: " + slowTime / 1_000_000 + " ms");

        // --- Fast path (native) ---
       if (!NATIVE) {
            log.warning("Native resolver unavailable — skipping fast path timing");
            return;
        }

        long fastStart = System.nanoTime();
        SysexMapping fastResult = null;
        for (int i = 0; i < iterations; i++) {
            fastResult = registry.resolveFast(message);
        }
        long fastEnd = System.nanoTime();

        assertNotNull(fastResult);
        assertEquals("kInputFader", fastResult.getControlGroup());

        long fastTime = fastEnd - fastStart;
        log.info("Fast path (native) "+iterations+ " iterations: " + fastTime / 1_000_000 + " ms");

        // --- Sanity check: fast must be faster ---
        assertTrue(fastTime <= slowTime * 0.95,
            "Fast path should be significantly faster than slow path");
    }
}
