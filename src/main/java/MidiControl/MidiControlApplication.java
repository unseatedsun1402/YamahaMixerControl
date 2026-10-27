package MidiControl;

import MidiControl.Access.Authenticate.Role;
import MidiControl.Access.RoleCredentialStore;
import MidiControl.ContextModel.PatchViewBuilder;
import MidiControl.Routing.WebSocketEndpoint;
import MidiControl.Server.MidiServerListener;
import MidiControl.Services.GetServerIP;
import jakarta.servlet.http.HttpServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.io.Console;
import java.util.Arrays;
import java.util.Locale;

@SpringBootApplication
public class MidiControlApplication {

    public static void main(String[] args) {
        if (args.length == 2 && "--generate-role-password".equals(args[0])) {
            generateRolePassword(args[1]);
            return;
        }
        SpringApplication.run(MidiControlApplication.class, args);
    }

    private static void generateRolePassword(String roleName) {
        Role role = Role.parse(roleName)
                .filter(parsed -> parsed == Role.OPERATOR || parsed == Role.ADMIN)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role must be 'operator' or 'admin'."));
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException(
                    "Run this command from a terminal that supports hidden password entry.");
        }

        char[] password = console.readPassword("New %s password: ", role.name().toLowerCase(Locale.ROOT));
        char[] confirmation = console.readPassword("Confirm password: ");
        try {
            if (password == null || password.length == 0 || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords must be non-empty and match.");
            }
            System.out.println(RoleCredentialStore.toJson(
                    RoleCredentialStore.createCredential(password)));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
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
    FilterRegistrationBean<MidiControl.Access.ClientMetadataFilter> clientMetadataFilter() {
        FilterRegistrationBean<MidiControl.Access.ClientMetadataFilter> registration =
                new FilterRegistrationBean<>(new MidiControl.Access.ClientMetadataFilter());
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        exporter.setAnnotatedEndpointClasses(WebSocketEndpoint.class);
        return exporter;
    }
}
