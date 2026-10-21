package MidiControl.UserInterface.Meter;

import java.util.List;
import java.util.logging.Logger;

public interface MeterUpdateListener {
    Logger logger = Logger.getLogger(MeterUpdateListener.class.getName());

    void onMeterUpdate(MeterDTO dto);

    default void onMeterUpdates(List<MeterDTO> updates) {
        for (MeterDTO update : updates) {
            try {
                onMeterUpdate(update);
            } catch (Throwable t) {
                logger.warning("Meter listener threw: " + t.getMessage());
            }
        }
    }
}
