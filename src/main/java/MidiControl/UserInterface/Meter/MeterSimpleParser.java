package MidiControl.UserInterface.Meter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public final class MeterSimpleParser {

    private static final Logger logger = Logger.getLogger(MeterSimpleParser.class.getName());
    private static final List<MeterUpdateListener> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(MeterUpdateListener l) { listeners.add(l); }
    public static void removeListener(MeterUpdateListener l) { listeners.remove(l); }

    /**
     * Clean Yamaha meter parser with correct 7‑bit / 14‑bit support.
     */
    public static List<Integer> parse(byte[] msg) {
        try {
            if (!isMeterMessage(msg))
                return null;

            int model    = msg[4] & 0xFF;
            int category = msg[6] & 0xFF;
            int source   = msg[7] & 0xFF;
            int start    = msg[8] & 0xFF;

            // Determine Yamaha meter format based on model
            int bytesPer = MeterTools.bytesPerForModel(model);

            // Payload begins at byte 9
            int payloadStart = 9;
            int payloadEnd = msg.length - 1; // F7
            int payloadLen = payloadEnd - payloadStart;

            if (payloadLen <= 0)
                return List.of();

            // How many meters contained?
            int count = payloadLen / bytesPer;
            if (count <= 0)
                return List.of();

            List<Integer> meters = new ArrayList<>(count);
            long timestamp = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                int offset = payloadStart + (i * bytesPer);

                // Extract 1‑byte or 2‑byte raw meter value
                byte[] raw = new byte[bytesPer];
                System.arraycopy(msg, offset, raw, 0, bytesPer);

                // Raw unconverted value (for DTO)
                int rawInt;
                if (bytesPer == 1) {
                    rawInt = raw[0] & 0x7F;
                } else {
                    rawInt = ((raw[0] & 0x7F) << 7) | (raw[1] & 0x7F);
                }

                meters.add(rawInt);

                // Convert to centi‑dB (native or Java fallback)
                long centi = MeterTools.toCentiDb(raw, bytesPer);

                MeterDTO dto = new MeterDTO(
                        rawInt,
                        start + i,
                        (int) Math.min(Integer.MAX_VALUE, centi),
                        (byte) model,
                        MeterCategory.fromInt(category),
                        MeterSource.fromInt(source),
                        (bytesPer == 2),
                        timestamp
                );

                notifyListeners(dto);
            }

            return meters;

        } catch (Throwable t) {
            logger.severe("Meter parsing failed: " + t.getMessage());
            return null;
        }
    }

    private static boolean isMeterMessage(byte[] msg) {
        if (msg == null || msg.length < 10)
            return false;

        return (msg[0] & 0xFF) == 0xF0 &&
               (msg[1] & 0xFF) == 0x43 &&
               (msg[5] & 0xFF) == 0x21 &&
               (msg[msg.length - 1] & 0xFF) == 0xF7;
    }

    private static void notifyListeners(MeterDTO dto) {
        for (MeterUpdateListener l : listeners) {
            try {
                l.onMeterUpdate(dto);
            } catch (Throwable t) {
                logger.warning("Meter listener threw: " + t.getMessage());
            }
        }
    }
}