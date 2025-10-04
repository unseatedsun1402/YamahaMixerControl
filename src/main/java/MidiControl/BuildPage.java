/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package MidiControl;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Servlet to build mix view pages
 * @author ethanblood
 */
public class BuildPage extends HttpServlet {

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
        List<String> chnames = ReadChannelTable.getChannelnames();

        // Debug: print the size and contents of chnames
        System.out.println("chnames.size() = " + chnames.size());
        for (int i = 0; i < chnames.size(); i++) {
            System.out.println("chnames[" + i + "] = " + chnames.get(i));
        }

        try (PrintWriter out = response.getWriter()) {
            int coarse = Integer.parseInt(request.getParameter("coarse"));
            int fine = Integer.parseInt(request.getParameter("fine"));

            out.println("<div class=\"fader-grid\">"); // Start grid container

            for (int i = 0; i < 48; i++) {
                int channelIndex = fine + i;
                String channelName = chnames.get(i);

                out.println("<div class=\"fader-block\">");

                // Fader + value display
                out.println("<div class=\"fader-wrapper\">");
                out.println("<input type=\"range\" max=\"127\" id=\"ch" + channelIndex +"\" oninput=\"sendMessage(this)\" class=\"fader\" title=\"" + channelName + "\" style=\"writing-mode: bt-lr; -webkit-appearance: slider-vertical; appearance: slider-vertical; height: 200px; width: 30px;\"/>");
                out.println("<span class=\"fader-value\" id=\"f" + channelIndex + "-value\">0</span>");
                out.println("</div>");

                // Channel label
                out.println("<label class=\"fader-label\" id=\"f" + channelIndex + "\">" + channelName + "</label>");

                out.println("</div>");
            }

            out.println("</div>"); // End grid container
        }
        catch (Exception e){
            System.out.println(e);
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
