package MidiControl.Services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import MidiControl.Access.PasswordManager;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SetSystemPasswordServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(SetSystemPasswordServlet.class.getName());
    private static final int MAX_PASSWORD_LENGTH = 16;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        if (!isLoopbackAddress(request.getRemoteAddr())) {
            writeResponse(response, HttpServletResponse.SC_FORBIDDEN, "Only localhost may set the system password");
            return;
        }

        String contentType = request.getContentType();
        String mediaType = contentType == null ? "" : contentType.split(";", 2)[0].trim();
        if (!"application/json".equals(mediaType.toLowerCase(Locale.ROOT))) {
            writeResponse(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Expected a JSON request");
            return;
        }

        JsonElement body;
        try {
            body = JsonParser.parseReader(request.getReader());
        } catch (JsonParseException exception) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        if (!body.isJsonObject()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
            return;
        }

        JsonElement passwordElement = body.getAsJsonObject().get("password");
        if (passwordElement == null
                || !passwordElement.isJsonPrimitive()
                || !passwordElement.getAsJsonPrimitive().isString()) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Password is required");
            return;
        }

        String password = passwordElement.getAsString();
        if (password.isBlank() || password.length() > MAX_PASSWORD_LENGTH) {
            writeResponse(response, HttpServletResponse.SC_BAD_REQUEST, String.format("Password must be between 1 and %d characters", MAX_PASSWORD_LENGTH));
            return;
        }

        try {
            storePassword(password);
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Unable to store system password", exception);
            writeResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to store system password");
            return;
        }

        writeResponse(response, HttpServletResponse.SC_OK, "System password stored");
    }

    protected void storePassword(String password) throws IOException {
        PasswordManager.storePassword(password);
    }

    private static boolean isLoopbackAddress(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (UnknownHostException exception) {
            return false;
        }
    }

    private static void writeResponse(HttpServletResponse response, int status, String message) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("message", message);
        response.setStatus(status);
        response.getWriter().write(body.toString());
    }
}
