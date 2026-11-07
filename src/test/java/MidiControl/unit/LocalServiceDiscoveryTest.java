package MidiControl.unit;

import org.junit.jupiter.api.Test;

import MidiControl.Services.LocalServiceDiscovery;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.argThat;

class LocalServiceDiscoveryTest {

    @Test
    void registersHttpServiceAndClosesOnlyOnce() throws Exception {
        JmDNS jmDNS = mock(JmDNS.class);
        when(jmDNS.getHostName()).thenReturn("YamahaMixerControl.local.");

        LocalServiceDiscovery discovery = new LocalServiceDiscovery(jmDNS, 8123);

        assertEquals("YamahaMixerControl.local", discovery.getHostName());
        verify(jmDNS).getHostName();
        verify(jmDNS).registerService(argThat(service ->
                "_http._tcp.local.".equals(service.getType())
                        && "YamahaMixerControl".equals(service.getName())
                        && service.getPort() == 8123
                        && "/MidiControl/".equals(service.getPropertyString("path"))));

        discovery.close();
        discovery.close();

        verify(jmDNS).close();
        verifyNoMoreInteractions(jmDNS);
    }

    @Test
    void closesDiscoveryWhenServiceRegistrationFails() throws Exception {
        JmDNS jmDNS = mock(JmDNS.class);
        IOException registrationFailure = new IOException("Registration failed");
        doThrow(registrationFailure).when(jmDNS).registerService(any(ServiceInfo.class));

        IOException thrown = assertThrows(
                IOException.class,
                () -> new LocalServiceDiscovery(jmDNS, 8123)
        );

        assertSame(registrationFailure, thrown);
        verify(jmDNS).close();
    }
}
