package MidiControl.SysexUtils;

import MidiControl.SystemTools.NativeLoader;

public final class NativeSysex {

    private static final boolean nativeAvailable;

    static {
        nativeAvailable = NativeLoader.loadLibrary("native_sysex");
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static native void updateMappings(long[] keys, int[] indexes, int[] address_bytes);
    public static native int resolve(byte[] msg);
}
