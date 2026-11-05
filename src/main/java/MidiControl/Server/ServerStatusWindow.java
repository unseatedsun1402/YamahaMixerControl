package MidiControl.Server;

import org.springframework.context.ConfigurableApplicationContext;

import MidiControl.Services.LocalServiceDiscovery;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class ServerStatusWindow extends JFrame {
    private final ServerShutdown serverShutdown;

    public ServerStatusWindow(
            ConfigurableApplicationContext context,
            String ipAddress,
            int port,
            LocalServiceDiscovery discovery,
            String discoveryError
    ) {
        super("Yamaha Mixer Control");
        this.serverShutdown = new ServerShutdown(context, discovery);
        ServerStatus status = ServerStatus.running(
                ipAddress,
                port,
                discovery == null ? null : discovery.getHostName(),
                discoveryError
        );

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));

        JPanel details = new JPanel(new GridLayout(0, 2, 8, 8));
        details.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        details.add(new JLabel("Server status:"));
        details.add(new JLabel(status.state()));
        details.add(new JLabel("Network address:"));
        details.add(addressField(status.networkAddress()));
        details.add(new JLabel("Local address:"));
        if (discovery != null) {
            details.add(addressField(status.localAddress()));
        } else {
            details.add(new JLabel(status.localAddress()));
        }
        add(details, BorderLayout.CENTER);

        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(event -> shutdownApplication());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        actions.add(exitButton);
        add(actions, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                shutdownApplication();
            }
        });

        pack();
        setMinimumSize(new Dimension(440, getHeight()));
        setLocationRelativeTo(null);
    }

    private JTextField addressField(String address) {
        JTextField field = new JTextField(address);
        field.setEditable(false);
        field.setBorder(null);
        field.setBackground(getBackground());
        return field;
    }

    private void shutdownApplication() {
        IOException discoveryFailure = serverShutdown.shutdown();
        if (discoveryFailure != null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not stop local hostname discovery: " + discoveryFailure.getMessage(),
                    "Shutdown warning",
                    JOptionPane.WARNING_MESSAGE
            );
        }
        dispose();
    }
}
