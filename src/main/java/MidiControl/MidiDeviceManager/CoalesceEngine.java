package MidiControl.MidiDeviceManager;

import java.util.logging.Logger;

public final class CoalesceEngine implements ConfigChangeListener{
    private static volatile DeskProfile loadedDesk = DeskProfile.UNKNOWN;
    private static Logger logger = Logger.getLogger(CoalesceEngine.class.getName());

    enum DeskProfile {
        YAMAHA_01V96I(new int[]{2,3,4,5,6,7,8}),
        YAMAHA_M7CL  (new int[]{2,3,4,5,6,7,8,9,10,11}),
        UNKNOWN      (new int[0]);

        final int[] keyBytes;
        DeskProfile(int[] keyBytes) { this.keyBytes = keyBytes; }
    }

    public long getKey(byte[] message) {
        DeskProfile p = loadedDesk;
        if (p == DeskProfile.UNKNOWN) {
            logger.warning("CoalesceEngine has no known desk profile loaded");return -1;
        }

        int[] idx = p.keyBytes;
        if (idx.length == 0) return -1L;

        long key = 0L;
        for (int i : idx) {
            if (i < 0 || i >= message.length) return -1L;
            long v = message[i] & 0x7FL;
            key = (key << 7) | v;
        }
        return key;
    }
    
    @Override
    public void onChange(String deskname) {
        try{
            loadedDesk = DeskProfile.valueOf(deskname);
            logger.info("DeskProfile updated to: "+deskname);
        }
        catch (IllegalArgumentException e){logger.severe("Failed to load new profile: "+e);}
    }
}