/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
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


/**
 *
 * @author ethanblood
 */
public class GetDevices extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            
            try 
            {
                //out.println("<form method=\"put\" action=\"/device>");
                out.println("<select id=\"midi-select\">");
                out.println("<label for=\"midi-select\">Select Midi Device</label>");
                var devices = MidiInterface.discoverDevices();
                
                int count = 0;
                for(MidiDevice.Info info : devices)
                {   
                    //System.out.println(info);
                    int isout = javax.sound.midi.MidiSystem.getMidiDevice(info).getMaxReceivers();
                    if(!(isout == 0)){
                        out.println("<option value=\""+count+"\">"+info.getName()+" - MIDI OUT</option>");
                    }
                    
                    else{
                            out.println("<option value=\""+count+"\">"+info.getName()+" - MIDI IN</option>");}
                    count ++;
                }
                out.println("</select>");
                out.println("<button onclick=doSet()>Set Device</button>");
                //out.println("</form>");
                
                System.out.println("Called and done!");
            } catch (MidiUnavailableException ex) 
            {
                Logger.getLogger(GetDevices.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}