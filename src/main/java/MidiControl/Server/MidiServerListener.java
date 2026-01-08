package MidiControl.Server;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class MidiServerListener implements ServletContextListener {

    public static ServletContext CONTEXT;

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            CONTEXT = sce.getServletContext();

            MidiServer server = new MidiServer();

            server.run();
            Logger root = Logger.getLogger("");
            root.setLevel(Level.FINE);
            for (Handler h : root.getHandlers()) {
                h.setLevel(Level.INFO);
            }
            CONTEXT.setAttribute("midiServer", server);
        }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        MidiServer server = (MidiServer) sce.getServletContext().getAttribute("midiServer");
        if (server != null) {
            server.shutdown();
        }
    }
}