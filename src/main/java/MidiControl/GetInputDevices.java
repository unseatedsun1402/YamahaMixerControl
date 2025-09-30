package MidiControl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

public class GetInputDevices extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            try {
                out.println("<select id=\"midi-input-select\">");
                out.println("<label for=\"midi-input-select\">Select Midi Input Device</label>");
                var devices = MidiInterface.discoverDevices();

                int count = 0;
                for(MidiDevice.Info info : devices)
                {   
                    int isinput = javax.sound.midi.MidiSystem.getMidiDevice(info).getMaxReceivers();
                    if(isinput != 0)
                    {
                        out.println("<option value=\""+count+"\">"+info.getName()+" - MIDI IN</option>");
                    }
                    count ++;
                }
                out.println("</select>");
                out.println("<button onclick=doSetInput()>Set Input Device</button>");
                System.out.println("Loaded input devices");
            } catch (MidiUnavailableException ex) 
            {
                Logger.getLogger(GetInputDevices.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "This servlet returns a list of available midi input devices";
    }
}
