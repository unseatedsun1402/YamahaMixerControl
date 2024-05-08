/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package MidiControl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.io.Writer;
import static java.lang.String.format;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author ethanblood
 */
public class ChangeJSONNames extends HttpServlet {

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
            int channel = Integer.parseInt(request.getParameter("channel"));
            String name = request.getParameter("name");
            
            String path = new File("../").getCanonicalPath() + "/conf/channels.json";
            var obj = JsonParser.parseReader(new FileReader(path));
            List<JsonElement> jsonArray = obj.getAsJsonArray().asList();
            var elmnt = jsonArray.remove(channel-1).getAsJsonObject();
            elmnt.remove("short");
            elmnt.addProperty("short", name);
            jsonArray.add(channel-1, elmnt);
            
            Gson gson = new GsonBuilder().create();
            Writer writer = new FileWriter(path);
            JsonWriter gsonWriter = gson.newJsonWriter(writer);
            gsonWriter.beginArray();
            for(JsonElement each : jsonArray){
                gson.toJson(each, gsonWriter);
            }
            gsonWriter.endArray();
            gsonWriter.close();
            writer.close();
            System.out.println("Success written file");
            
        out.println(format("Added %s",request.getParameter("name")));
        }
        catch (Exception e)
        {
                Logger.getGlobal().warning(e.toString());
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