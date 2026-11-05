package MidiControl.unit;

import org.junit.jupiter.api.Test;

import MidiControl.Server.StartupFailureMessage;

import java.net.BindException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartupFailureMessageTest {

    @Test
    void identifiesPortConflictsFromNestedBindExceptions() {
        RuntimeException failure = new RuntimeException(
                "Web server failed",
                new BindException("Address already in use: bind")
        );

        assertEquals(
                "Port 8080 is in use by another app. Close that app and try again.",
                StartupFailureMessage.create(failure, 8080)
        );
    }

    @Test
    void usesPortReportedBySpringWhenAvailable() {
        RuntimeException failure = new RuntimeException("Port 8123 was already in use.");

        assertEquals(
                "Port 8123 is in use by another app. Close that app and try again.",
                StartupFailureMessage.create(failure, 8080)
        );
    }

    @Test
    void keepsOtherStartupErrorsExplicit() {
        RuntimeException failure = new RuntimeException("Configuration is invalid");

        assertEquals(
                "Yamaha Mixer Control could not start: Configuration is invalid",
                StartupFailureMessage.create(failure, 8080)
        );
    }
}
