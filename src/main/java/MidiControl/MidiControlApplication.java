package MidiControl;

import MidiControl.ContextModel.PatchViewBuilder;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.MidiServerListener;
import MidiControl.Services.GetServerIP;
import jakarta.servlet.http.HttpServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@SpringBootApplication
public class MidiControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(MidiControlApplication.class, args);
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
