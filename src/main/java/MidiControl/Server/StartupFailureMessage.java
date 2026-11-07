package MidiControl.Server;

import java.net.BindException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StartupFailureMessage {
    private static final Pattern PORT_IN_USE = Pattern.compile(
            "(?i)port\\s+(\\d+)\\s+(?:was\\s+)?already in use"
    );

    private StartupFailureMessage() {
    }

    public static String create(Throwable failure, int configuredPort) {
        if (isPortInUse(failure)) {
            int port = findPort(failure, configuredPort);
            return "Port " + port + " is in use by another app. Close that app and try again.";
        }

        String detail = failure.getMessage();
        return detail == null || detail.isBlank()
                ? "Yamaha Mixer Control could not start."
                : "Yamaha Mixer Control could not start: " + detail;
    }

    private static boolean isPortInUse(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof BindException) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null
                    && (message.contains("Address already in use")
                    || PORT_IN_USE.matcher(message).find())) {
                return true;
            }
        }
        return false;
    }

    private static int findPort(Throwable failure, int fallbackPort) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message == null) {
                continue;
            }
            Matcher matcher = PORT_IN_USE.matcher(message);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }
        return fallbackPort;
    }
}
