package MidiControl.Services;

import MidiControl.MidiDeviceManager.Configs.ChannelNameStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** @author ethanblood */
public class ChangeJSONNames extends HttpServlet {

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
   *
   * @param request servlet request
   * @param response servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {

      String name = req.getParameter("name");
      String channelStr = req.getParameter("channel");

      if (name == null || channelStr == null) {
          resp.sendError(400, "Missing parameters");
          return;
      }

      int channel;
      try {
          channel = Integer.parseInt(channelStr);
      } catch (NumberFormatException e) {
          resp.sendError(400, "Invalid channel number");
          return;
      }

      try {
          ChannelNameStore.setName(channel, name);
          resp.getWriter().println("Updated channel " + channel + " to " + name);
      } catch (IllegalArgumentException e) {
          resp.sendError(400, e.getMessage());
      } catch (Exception e) {
          resp.sendError(500, "Internal error");
      }
  }
}
