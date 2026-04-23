package MidiControl.UserInterface.Meter;

import java.util.logging.Logger;
import MidiControl.SystemTools.NativeLoader;

public final class MeterTools {

    private static final Logger logger = Logger.getLogger(MeterTools.class.getName());
    private static volatile boolean nativeAvailable;

    private static volatile boolean nativeSingleFailed;
    private static volatile boolean nativeBlockFailed;

    static {
        nativeAvailable = NativeLoader.loadLibrary("native_meter_tools");
        if (nativeAvailable) {
            logger.fine("Loaded native meter_tools");
        }
    }

    private MeterTools() {}

    public static void disableNativeForTests(){
        nativeAvailable = false;
    }

    public static int bytesPerForModel(int modelByte) {
        return ((modelByte & 0xFF) == 0x1A) ? 2 : 1;
    }

    public static native long convertSingle(byte[] raw, int bytesPer);
    public static native void convertBlock(byte[] raw, int bytesPer, long[] out);

    private static final float[] YAMAHA_7BIT_DB = {
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
        if (raw == null || raw.length < bytesPer || bytesPer < 1 || bytesPer > 2)
            return Long.MIN_VALUE;

        int v7;

        if (bytesPer == 1) {
            v7 = raw[0] & 0x7F;
        } else {
            int hi = raw[0] & 0x7F;
            int lo = raw[1] & 0x7F;
            int v14 = (hi << 7) | lo;

            if (v14 >= 0x1FFF) {
                v7 = 127;
            } else {
                if (v14 > 4368) v14 = 4368;
                v7 = Math.round(v14 * (127.0f / 4368.0f));
            }
        }

        if (v7 < 0) v7 = 0;
        if (v7 >= 112) v7 = 111;

        return Math.round(YAMAHA_7BIT_DB[v7] * 100.0);
    }

    private static void convertBlockJava(byte[] raw, int bytesPer, long[] out) {
        int count = Math.min(out.length, raw.length / bytesPer);
        int idx = 0;

        for (int i = 0; i < count; i++) {
            int v7;

            if (bytesPer == 1) {
                v7 = raw[idx] & 0x7F;
            } else {
                int hi = raw[idx] & 0x7F;
                int lo = raw[idx + 1] & 0x7F;
                int v14 = (hi << 7) | lo;

                if (v14 >= 0x1FFF) {
                    v7 = 127;
                } else {
                    if (v14 > 4368) v14 = 4368;
                    v7 = Math.round(v14 * (127.0f / 4368.0f));
                }
            }

            if (v7 < 0) v7 = 0;
            if (v7 >= 112) v7 = 111;

            out[i] = Math.round(YAMAHA_7BIT_DB[v7] * 100.0);
            idx += bytesPer;
        }
    }

    public static long toCentiDb(byte[] raw, int bytesPer) {
        if (raw == null || raw.length < bytesPer || bytesPer < 1 || bytesPer > 2)
            return Long.MIN_VALUE;

        if (nativeAvailable && !nativeSingleFailed) {
            try {
                return convertSingle(raw, bytesPer);
            } catch (Throwable t) {
                nativeSingleFailed = true;
                logger.warning("Native convertSingle failed, using Java path: " + t);
            }
        }

        return convertSingleJava(raw, bytesPer);
    }

    public static long toCentiDb(byte[] raw, byte modelByte) {
        return toCentiDb(raw, bytesPerForModel(modelByte));
    }

    public static long[] toCentiDbBlock(byte[] raw, int bytesPer) {
        if (raw == null || bytesPer < 1 || bytesPer > 2)
            return new long[0];

        long[] out = new long[raw.length / bytesPer];

        if (nativeAvailable && !nativeBlockFailed) {
            try {
                convertBlock(raw, bytesPer, out);
                return out;
            } catch (Throwable t) {
                nativeBlockFailed = true;
                logger.warning("Native convertBlock failed, using Java path: " + t);
            }
        }

        convertBlockJava(raw, bytesPer, out);
        return out;
    }

    public static long[] toCentiDbBlock(byte[] raw, int modelByte, boolean auto) {
        return toCentiDbBlock(raw, bytesPerForModel(modelByte));
    }

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