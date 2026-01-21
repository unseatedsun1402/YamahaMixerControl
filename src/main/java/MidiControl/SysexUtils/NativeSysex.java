package MidiControl.SysexUtils;

public final class NativeSysex {

    private static final boolean nativeAvailable;

    static {
        boolean loaded = false;
        String path = "G:\\WorkingDir\\YamahaMixerControl\\src\\main\\cpp\\MidiControl\\SysexUtils\\native_sysex.dll";
        try {
            System.out.println("[NativeSysex] Attempting to load: " + path);
            System.load(path);
            System.out.println("[NativeSysex] Loaded successfully");
            loaded = true;
        } catch (Throwable t) {
            System.err.println("[NativeSysex] Native library not loaded " + path + ", falling back to Java resolver");
            t.printStackTrace();
        }
        nativeAvailable = loaded;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static native void updateMappings(long[] keys, int[] indexes, int[] address_bytes);
    public static native int resolve(byte[] msg);
}