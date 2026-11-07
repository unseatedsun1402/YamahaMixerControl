package MidiControl.Services;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalServiceDiscovery implements AutoCloseable {
    private static final String HOST_NAME = "YamahaMixerControl";
    private static final String SERVICE_TYPE = "_http._tcp.local.";

    private final JmDNS jmDNS;
    private final String hostName;
    private final AtomicBoolean closed = new AtomicBoolean();

    public LocalServiceDiscovery(InetAddress address, int port) throws IOException {
        this(JmDNS.create(address, HOST_NAME), port);
    }

    public LocalServiceDiscovery(JmDNS instance, int port) throws IOException {
        try {
            instance.registerService(ServiceInfo.create(
                    SERVICE_TYPE,
                    HOST_NAME,
                    port,
                    "path=/MidiControl/"
            ));
            this.jmDNS = instance;
            String advertisedName = instance.getHostName();
            this.hostName = advertisedName.endsWith(".")
                    ? advertisedName.substring(0, advertisedName.length() - 1)
                    : advertisedName;
        } catch (IOException | RuntimeException exception) {
            try {
                instance.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    public String getHostName() {
        return hostName;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            jmDNS.close();
        }
    }
}
