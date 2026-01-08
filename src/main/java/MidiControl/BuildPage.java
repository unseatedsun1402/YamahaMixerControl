package MidiControl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import MidiControl.Controls.CanonicalRegistry;
import MidiControl.Controls.ControlGroup;
import MidiControl.Controls.ControlInstance;
import MidiControl.Controls.SubControl;
import MidiControl.Server.MidiServer;
public class BuildPage extends HttpServlet {

protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        MidiServer ms = (MidiServer) getServletContext().getAttribute("midiServer");
        if (ms == null) {
            throw new IllegalStateException("MIDI server not initialized");
        }
        CanonicalRegistry registry = ms.getCanonicalRegistry();

        List<String> chnames = ReadChannelTable.getChannelnames();

        try (PrintWriter out = response.getWriter()) {

            out.println("<div class=\"fader-grid\">");

            // We only want input faders for this page
            ControlGroup group = registry.getGroup("kInput");
            SubControl faderSub = group.getSubcontrol("Fader");

            List<ControlInstance> faders = faderSub.getInstances();

            for (int i = 0; i < faders.size(); i++) {

                ControlInstance instance = faders.get(i);
                String canonicalId = instance.getCanonicalId();  // e.g., kInput.Fader.0
                String channelName = chnames.get(i);
                int channelNumber = i + 1;

                if (i % 8 == 0) {
                    out.println("<div class=\"fader-bank\">");
                }

                out.println("<div class=\"fader-block kInput-Fader\" "
                        + "data-canonical-id=\"" + canonicalId + "\">");

                out.println("<div class=\"channel-index\">Input " + channelNumber + "</div>");

                out.println("<div class=\"fader-wrapper\">");

                out.println("<input type=\"range\" max=\"127\" "
                        + "class=\"fader\" "
                        + "data-canonical-id=\"" + canonicalId + "\" "
                        + "oninput=\"sendCanonical(this)\" "
                        + "value=\"60\" "
                        + "title=\"" + channelName + "\"/>");

                out.println("<span class=\"fader-value\" "
                        + "data-canonical-id=\"" + canonicalId + "\">-18 dB</span>");

                out.println("</div>"); // fader-wrapper

                out.println("<label class=\"fader-label\">" + channelName + "</label>");

                out.println("</div>"); // fader-block

                if (i % 8 == 7) {
                    out.println("</div>"); // fader-bank
                }
            }

            out.println("</div>"); // fader-grid
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        processRequest(req, resp);
    }
}