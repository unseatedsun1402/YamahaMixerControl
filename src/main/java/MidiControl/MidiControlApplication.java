package MidiControl;

import MidiControl.ContextModel.PatchViewBuilder;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Routing.WebSocketPeerAddressFilter;
import MidiControl.Server.MidiServerListener;
import MidiControl.Server.ServerStatusWindow;
import MidiControl.Server.StartupFailureMessage;
import MidiControl.Services.GetServerIP;
import MidiControl.Services.LocalServiceDiscovery;
import MidiControl.Services.SetSystemPasswordServlet;
import jakarta.servlet.http.HttpServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.InetAddress;

@SpringBootApplication
public class MidiControlApplication {

    public static void main(String[] args) {
    ConfigurableApplicationContext context;
    try {
        context = SpringApplication.run(MidiControlApplication.class, args);
    } catch (RuntimeException exception) {
        if (GraphicsEnvironment.isHeadless()) {
            throw exception;
        }
        int port = configuredPort(args);
        JOptionPane.showMessageDialog(
                null,
                StartupFailureMessage.create(exception, port),
                "Yamaha Mixer Control - Startup Error",
                JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
    String ipAddress = new GetServerIP().getServerIpAddress();

        LocalServiceDiscovery discovery = null;
        String discoveryError = null;
        try {
            discovery = new LocalServiceDiscovery(InetAddress.getByName(ipAddress), port);
            LocalServiceDiscovery shutdownDiscovery = discovery;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    shutdownDiscovery.close();
                } catch (IOException exception) {
                    System.err.println("Unable to stop local service discovery: " + exception.getMessage());
                }
            }, "local-service-discovery-shutdown"));
        } catch (IOException exception) {
            discoveryError = exception.getMessage() == null
                    ? exception.toString()
                    : exception.getMessage();
            System.err.println("Unable to advertise the local hostname: " + discoveryError);
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("No desktop is available; the status window cannot be displayed.");
            return;
        }

        String finalDiscoveryError = discoveryError;
        LocalServiceDiscovery finalDiscovery = discovery;
        SwingUtilities.invokeLater(() -> new ServerStatusWindow(
                context,
                ipAddress,
                port,
                finalDiscovery,
                finalDiscoveryError
        ).setVisible(true));
    }

    private static int configuredPort(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--server.port=")) {
                try {
                    return Integer.parseInt(arg.substring("--server.port=".length()));
                } catch (NumberFormatException ignored) {
                    return 8080;
                }
            }
        }
        return 8080;
    }

    @Bean
    ServletListenerRegistrationBean<MidiServerListener> midiServerListener() {
        return new ServletListenerRegistrationBean<>(new MidiServerListener());
    }

    @Bean
    ServletRegistrationBean<GetServerIP> getServerIpServlet() {
        return new ServletRegistrationBean<>(new GetServerIP(), "/get-server-ip");
    }

    @Bean
    ServletRegistrationBean<SetSystemPasswordServlet> setSystemPasswordServlet() {
        return new ServletRegistrationBean<>(new SetSystemPasswordServlet(), "/set-system-password");
    }

    @Bean
    FilterRegistrationBean<WebSocketPeerAddressFilter> webSocketPeerAddressFilter() {
        FilterRegistrationBean<WebSocketPeerAddressFilter> registration =
                new FilterRegistrationBean<>(new WebSocketPeerAddressFilter());
        registration.addUrlPatterns("/endpoint");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    ServletRegistrationBean<HttpServlet> sourceMapServlet() {
        return new ServletRegistrationBean<>(
                new PatchViewBuilder.SourceMapServlet(),
                "/source-map");
    }

    @Bean
    ServletRegistrationBean<HttpServlet> destinationMapServlet() {
        return new ServletRegistrationBean<>(
                new PatchViewBuilder.DestinationMapServlet(),
                "/dest-map");
    }

    @Bean
    ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        exporter.setAnnotatedEndpointClasses(WebSocketEndpoint.class);
        return exporter;
    }
}
