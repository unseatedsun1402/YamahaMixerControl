package MidiControl.Server;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import MidiControl.Routing.WebSocketEndpoint;
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
            Logger root = Logger.getLogger("");
            Formatter logFormat = new CustomColorFormatter();
            root.setLevel(Level.INFO);
            for (Handler h : root.getHandlers()) {
                h.setLevel(Level.INFO);
                h.setFormatter(logFormat);
            }
            MidiServer server = new MidiServer();
            server.run();
            WebSocketEndpoint.enableDebug();
            CONTEXT.setAttribute("midiServer", server);
        }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        MidiServer server = (MidiServer) sce.getServletContext().getAttribute("midiServer");
        if (server != null) {
            server.shutdown();
        }
    }

    public class CustomColorFormatter extends Formatter {
    @Override
        public String format(LogRecord record) {
        String color = switch (record.getLevel().getName()) {
            case "SEVERE" -> "\u001B[31m";  // Red
            case "INFO"   -> "\u001B[32m";  // Green
            case "WARNING"->"\u001B[33m";   // Yellow
            case "FINE"   -> "\u001B[36m";  // Cyan
            case "FINER"  -> "\u001B[45m";  // Bckgrnd Magenta
            default -> "\u001B[0m"; // Reset
            };
        return getContextFinal(record.getLoggerName()) + "\t" +color+record.getLevel() +
            ": " + record.getMessage() + "\u001B[0m\n";
        }
    }

    private String getContextFinal(String context){
        String[] sections = context.split("\\.");
        if (sections.length == 0) return sections.toString();
        return sections[sections.length -1].toString();
    }
}