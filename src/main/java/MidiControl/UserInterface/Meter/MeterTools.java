
package MidiControl.UserInterface.Meter;

import java.util.logging.Logger;

import MidiControl.SystemTools.NativeLoader;

public final class MeterTools {
    private static final Logger logger = Logger.getLogger(MeterTools.class.getName());

    private static final boolean nativeAvailable;

    static {
        nativeAvailable = NativeLoader.loadLibrary("native_meter_tools");
        if (nativeAvailable) {
            logger.info("Loaded native meter_tools");
        } else {
            logger.warning("Falling back to Java meter tools");
        }
    }

    private MeterTools() {}

    /* -------------------------------------------
       Rule: 01V96 / 01V96i = 2‑byte meters (0x1A)
             Everything else = 1‑byte meters
       ------------------------------------------- */
    public static int bytesPerForModel(int modelByte) {
        return (modelByte == 0x1A) ? 2 : 1;
    }

    /* -------------------------------------------
       Native bindings
       ------------------------------------------- */
    public static native long convertSingle(byte[] raw, int bytesPer);
    public static native void convertBlock(byte[] raw, int bytesPer, long[] out);

    /* -------------------------------------------
       Java fallback (mirrors C++)
       ------------------------------------------- */

    private static final float[] YAMAHA_7BIT_DB = new float[] {
        -95,-94,-93,-92,-91,-90,-89,-89,
        -88,-87,-86,-85,-84,-83,-82,-81,
        -80,-79,-78,-78,-77,-76,-75,-74,
        -73,-73,-72,-72,-71,-70,-69,-68,
        -67,-67,-66,-65,-64,-63,-62,-62,
        -61,-60,-59,-58,-57,-56,-55,-55,
        -54,-53,-52,-51,-50,-50,-49,-48,
        -48,-47,-46,-45,-44,-43,-42,-42,
        -41,-40,-39,-38,-37,-36,-35,-34,
        -33,-32,-31,-30,-29,-28,-27,-26,
        -25,-24,-23,-22,-21,-20,-19,-18,
        -17,-16,-15,-14,-13,-12,-11,-10,
        -9,-8,-7,-6,-5,-4,-3,-2,
        -1,0,1,2,3,4,5,6
    };

    private static long convertSingleJava(byte[] raw, int bytesPer) {
        if (raw == null || bytesPer < 1 || bytesPer > 2 || raw.length < bytesPer)
            return Long.MIN_VALUE;
        if (bytesPer == 1) {
            int v7 = raw[0] & 0x7F;
            if (v7 < 0) v7 = 0;
            if (v7 >= 112) v7 = 111;
            return Math.round(YAMAHA_7BIT_DB[v7] * 100.0);
        }

        // 2-byte "14-bit" Yamaha meter value (01V96i etc.)
        int hi = raw[0] & 0x7F;
        int lo = raw[1] & 0x7F;
        int v14 = (hi << 7) | lo;
        int v7  = (v14 *127) / 4096;
        if (v7 < 0) v7 = 0;
        if (v7 >= 112) v7 = 111;
        return Math.round(YAMAHA_7BIT_DB[v7] * 100.0);
    }

    private static void convertBlockJava(byte[] raw, int bytesPer, long[] out) {
        if (raw == null || out == null || bytesPer < 1 || bytesPer > 2) return;

        int count = Math.min(out.length, raw.length / bytesPer);
        int idx = 0;

        for (int i = 0; i < count; i++) {
            if (bytesPer == 1) {
                out[i] = convertSingleJava(new byte[]{ raw[idx] }, 1);
            } else {
                out[i] = convertSingleJava(new byte[]{ raw[idx], raw[idx+1] }, 2);
            }
            idx += bytesPer;
        }
    }

    public static long toCentiDb(byte[] raw, int bytesPer) {
        if (nativeAvailable) {
            try {
                return convertSingle(raw, bytesPer);
            } catch (Throwable t) {
                logger.severe("Native convertSingle failed: " + t);
            }
        }
        return convertSingleJava(raw, bytesPer);
    }

    public static long toCentiDb(byte[] raw, byte modelByte) {
        int bytesPer = bytesPerForModel(modelByte);
        return toCentiDb(raw, bytesPer);
    }

    public static long[] toCentiDbBlock(byte[] raw, int bytesPer) {
        if (raw == null || bytesPer < 1 || bytesPer > 2)
            return new long[0];

        long[] out = new long[raw.length / bytesPer];

        if (nativeAvailable) {
            try {
                convertBlock(raw, bytesPer, out);
                return out;
            } catch (Throwable t) {
                logger.severe("Native convertBlock failed: " + t);
            }
        }

        convertBlockJava(raw, bytesPer, out);
        return out;
    }

    public static long[] toCentiDbBlock(byte[] raw, int modelByte, boolean auto) {
        int bytesPer = bytesPerForModel(modelByte);
        return toCentiDbBlock(raw, bytesPer);
    }

    /* -------------------------------------------
       Formatting
       ------------------------------------------- */
    public static String formatCentiDb(long centi) {
        if (centi == Long.MIN_VALUE) return "-inf dB";
        long abs = Math.abs(centi);
        return String.format(
            "%s%d.%02d dB",
            (centi < 0 ? "-" : ""),
            abs / 100,
            abs % 100
        );
    }
}