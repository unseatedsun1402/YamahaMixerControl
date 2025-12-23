package MidiControl.Server;

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