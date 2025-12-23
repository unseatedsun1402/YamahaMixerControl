package MidiControl.ControlMappings;

import java.util.HashMap;
import java.util.Map;

public class FaderControl {
  // Example: You can load this from Excel or hardcode key points
  private static final Map<Integer, Double> midiToDbMap = new HashMap<>();

  static {
    midiToDbMap.put(0, Double.NEGATIVE_INFINITY); // -Inf dB
    midiToDbMap.put(1023, 10.00); // +10.00 dB
    // You can load the full map from Excel or interpolate
  }

  public static double midiToDb(int midiValue) {
    if (midiToDbMap.containsKey(midiValue)) {
      return midiToDbMap.get(midiValue);
    }

    // Optional: interpolate between known values
    return interpolateDb(midiValue);
  }

  private static double interpolateDb(int midiValue) {
    // Simple linear approximation: scale 0–1023 to -138dB to +10dB
    double minDb = -138.0;
    double maxDb = 10.0;
    return minDb + ((maxDb - minDb) * midiValue / 1023.0);
  }

  public static String formatDb(double db) {
    if (Double.isInfinite(db)) return "-Inf dB";
    return String.format("%.2f dB", db);
  }
}
