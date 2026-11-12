package MidiControl.UserInterface.Meter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import MidiControl.SysexUtils.ModelNumbers;

public final class MeterSimpleParser {

    private static final Logger logger = Logger.getLogger(MeterSimpleParser.class.getName());
    private static final List<MeterUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private static final long UPDATE_INTERVAL_MS = 333;
    private static final Map<String, MeterDTO> eventBuffer = new LinkedHashMap<>(256);
    private static final ScheduledExecutorService meterProcessor =
            Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "MeterUpdateProcessor");
                thread.setDaemon(true);
                return thread;
            });

    static {
        meterProcessor.scheduleWithFixedDelay(
                MeterSimpleParser::processQueue,
                UPDATE_INTERVAL_MS,
                UPDATE_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
    }

    public static void addListener(MeterUpdateListener l) { listeners.add(l); }
    public static void removeListener(MeterUpdateListener l) { listeners.remove(l); }

    public static void reset() {
        synchronized (eventBuffer) {
            eventBuffer.clear();
        }
    }

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
            int responseStart = msg[8] & 0x7F;
            // Determine Yamaha meter format based on model
            int bytesPer = MeterTools.bytesPerForModel(model);

            // Payload begins at byte 9
            int payloadStart = 9;
            int payloadEnd = msg.length - 1; // F7
            int payloadLen = payloadEnd - payloadStart;

            if (payloadLen <= 0)
                return List.of();

            if (payloadLen % bytesPer != 0) {
                logger.warning("Ignoring incomplete meter value");
                return null;
            }
            if (ModelNumbers.isM7clMeterResponse(model) && category == 0x10) {
                logger.warning("M7CL sigma clip bitmaps are not level meter values");
                return null;
            }
            if (source == 0x7F && model == 0x1A) {
                logger.warning("01V96i all-point meter responses are not supported");
                return null;
            }
            int pointsPerChannel = ModelNumbers.isM7clMeterResponse(model) && category == 0 && source == 0x7F ? 9 : 1;
            int count = payloadLen / bytesPer;
            if (count % pointsPerChannel != 0) {
                logger.warning("Ignoring incomplete all-point meter response");
                return null;
            }
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
                        responseStart + i / pointsPerChannel,
                        toCentiDbInt(centi),
                        (byte) model,
                        MeterCategory.fromInt(category),
                        MeterSource.fromInt(pointsPerChannel == 1 ? source : i % pointsPerChannel, model),
                        (bytesPer == 2),
                        timestamp
                );

                enqueueMeterEvent(dto);
            }

            MeterDiagnostics.parsed(msg, meters);
            return meters;

        } catch (Throwable t) {
            logger.severe("Meter parsing failed: " + t.getMessage());
            return null;
        }
    }

    private static boolean isMeterMessage(byte[] msg) {
        if (msg == null || msg.length < 11)
            return false;

        return (msg[0] & 0xFF) == 0xF0 &&
               (msg[1] & 0xFF) == 0x43 &&
               (msg[2] & 0xF0) == 0x10 &&
               (msg[3] & 0xFF) == 0x3E &&
               ((msg[4] & 0xFF) == 0x1A || ModelNumbers.isM7clMeterResponse(msg[4])) &&
               (msg[5] & 0xFF) == 0x21 &&
               (msg[msg.length - 1] & 0xFF) == 0xF7;
    }

    private static int toCentiDbInt(long centi) {
        if (centi == Long.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) Math.max(Integer.MIN_VALUE + 1L,
                Math.min(Integer.MAX_VALUE, centi));
    }

    private static void enqueueMeterEvent(MeterDTO dto){
        String key = dto.category + ":" + dto.source + ":" + dto.offset;
        synchronized (eventBuffer) {
            eventBuffer.remove(key);
            eventBuffer.put(key, dto);
        }
    }

    private static void processQueue(){
        List<MeterDTO> pending;
        synchronized (eventBuffer) {
            if (eventBuffer.isEmpty()) {
                return;
            }
            pending = new ArrayList<>(eventBuffer.values());
            eventBuffer.clear();
        }

        for (MeterUpdateListener listener : listeners) {
            try {
                listener.onMeterUpdates(pending);
            } catch (Throwable t) {
                logger.warning("Meter listener threw: " + t.getMessage());
            }
        }
    }

}
