package MidiControl.Server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.pattern.ClassicConverter;

public class ColoredMessageConverter extends ClassicConverter {

    private static final String RESET = "\u001B[0m";

    @Override
    public String convert(ILoggingEvent event) {
        String color = switch (event.getLevel().toInt()) {
            case Level.ERROR_INT -> "\u001B[31m";
            case Level.WARN_INT -> "\u001B[33m";
            case Level.INFO_INT -> "\u001B[36m";
            case Level.DEBUG_INT -> "\u001B[32m";
            case Level.TRACE_INT -> "\u001B[45m";
            default -> RESET;
        };

        return color + event.getLevel() + ": " + event.getFormattedMessage() + RESET;
    }
}
