package MidiControl.UserInterface.Meter;

public final class MeterDTO {
    public final int value;
    public final int offset;
    public final byte model;
    public final String category;
    public final String source;
    public final long timestamp;
    public final String dB; // centi-dB, e.g. -9500 == -95.00 dB
    public boolean is14bit;

    public MeterDTO(int value, int offset, int dB, byte model,
                    MeterCategory category, MeterSource source, boolean is14bit, long timestamp) {
        this.value = value;
        this.offset = offset;
        this.model = model;
        this.category = category.name();
        this.source = source.name();
        this.timestamp = timestamp;
        this.is14bit = is14bit;
        this.dB = formatCentiDbNumeric(dB);
    }

    private static final ThreadLocal<StringBuilder> TL_SB =
        ThreadLocal.withInitial(() -> new StringBuilder(256));

    public String toJson() {
        StringBuilder sb = TL_SB.get();
        sb.setLength(0);

        sb.append("{\"type\":\"meter-update\",\"payload\":{");
        sb.append("\"category\":\"").append(category).append("\",");
        sb.append("\"source\":\"").append(source).append("\",");
        sb.append("\"offset\":").append(offset).append(",");
        sb.append("\"value\":").append(value).append(",");
        sb.append("\"dB\":\"").append(dB).append("\",");
        sb.append("\"is14Bit\":\"").append(is14bit).append("\",");
        sb.append("\"ts\":").append(timestamp);
        sb.append("}}");

        return sb.toString();
    }

    /**
     * Return a numeric representation of centi-dB as a decimal string with two digits.
     * Example: -9500 -> "-95.00"
     * Handles the sentinel value Integer.MIN_VALUE as "-inf".
     */
    private static String formatCentiDbNumeric(int centi) {
        if (centi == Integer.MIN_VALUE) return "\"-inf\""; // keep as string token for -inf
        boolean negative = centi < 0;
        int abs = negative ? -centi : centi;
        int whole = abs / 100;
        int frac = abs % 100;
        // build into a small StringBuilder to avoid allocations
        StringBuilder t = new StringBuilder(8);
        if (negative) t.append('-');
        t.append(whole).append('.');
        if (frac < 10) t.append('0');
        t.append(frac);
        return t.toString();
    }
}