package MidiControl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet to build mix view pages
 *
 * @author ethanblood
 */
public class BuildPage extends HttpServlet {

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
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
        int raw = fine + i;
        int msb = coarse + (raw / 128); // MIDI MSB is 7-bit, so divide by 128
        int lsb = raw % 128; // MIDI LSB is 7-bit, so modulo 128

        int channelIndex = fine + i;
        String channelName = chnames.get(i);

        if (i % 8 == 0) {
          out.println(
              "<div class=\"fader-bank\" style=\"background-color: #4d4d4dff;\">"); // Start new
                                                                                    // fader bank: 8
                                                                                    // faders
        }

        out.println(
            "<div class=\"fader-block\" data-type=\"input\" data-channel=\""
                + (i + 1)
                + "\">"); // fader block wraps the range and labels

        out.println(
            "<div class=\"channel-index\">Input "
                + (i + 1)
                + "</div>"); // channel index is a numeric label to map the ch name to a patched
                             // input
        // todo floating bank location
        out.println("<div class=\"fader-wrapper\">"); // Fader + value display

        out.println(
            "<input type=\"range\" max=\"127\" id=\"ch"
                + channelIndex
                + "\" "
                + "data-msb=\""
                + msb
                + "\" data-lsb=\""
                + lsb
                + "\" "
                + "oninput=\"sendMessage(this)\" class=\"fader\" title=\""
                + channelName
                + "\" value=60/>"); // Fader input

        out.println(
            "<span class=\"fader-value\" id=\"f"
                + channelIndex
                + "-value\" "
                + "data-msb=\""
                + msb
                + "\" data-lsb=\""
                + lsb
                + "\">-18 dB</span>"); // Fader value display

        out.println("</div>"); // End fader-wrapper

        out.println(
            "<label class=\"fader-label\" id=\"f"
                + channelIndex
                + "\">"
                + channelName
                + "</label>"); // Channel label
        out.println("</div>"); // End fader-block

        if (i % 8 == 7) {
          out.println("</div>"); // End fader-bank
        }
      }

      out.println("</div>"); // End grid container
    } catch (Exception e) {
      System.out.println(e);
    }
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the
  // left to edit the code.">
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
  } // </editor-fold>
}
