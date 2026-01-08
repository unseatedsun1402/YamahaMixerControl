package MidiControl.MidiDeviceManager.Configs;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.File;
import java.util.logging.Logger;

@WebListener
public class ChannelNameStoreListener implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(ChannelNameStoreListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            File file = ChannelNameStoreInitializer.initialize();
            ChannelNameStore.initialize(file);
            logger.info("ChannelNameStore initialized with " + file.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ChannelNameStore", e);
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("ChannelNameStore shutting down");
    }
}