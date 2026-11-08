package MidiControl.unit.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import MidiControl.Services.SetSystemPasswordServlet;

class SetSystemPasswordServletTest {
    @Test
    void storesPasswordForLoopbackRequestAndReturnsOnlyToRequester() throws IOException, ServletException {
        CapturingServlet servlet = new CapturingServlet();
        MockHttpServletRequest request = requestFrom("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContent("{\"password\":\"local-secret\"}".getBytes(StandardCharsets.UTF_8));

        servlet.service(request, response);

        assertEquals(200, response.getStatus());
        assertEquals("local-secret", servlet.storedPassword);
        assertFalse(response.getContentAsString().contains("local-secret"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }

    @Test
    void rejectsNonLoopbackRequestWithoutStoringPassword() throws IOException, ServletException {
        CapturingServlet servlet = new CapturingServlet();
        MockHttpServletRequest request = requestFrom("192.0.2.15");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setContent("{\"password\":\"remote-secret\"}".getBytes(StandardCharsets.UTF_8));

        servlet.service(request, response);

        assertEquals(403, response.getStatus());
        assertNull(servlet.storedPassword);
        assertFalse(response.getContentAsString().contains("remote-secret"));
    }

    private static MockHttpServletRequest requestFrom(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/set-system-password");
        request.setRemoteAddr(remoteAddress);
        request.setContentType("application/json");
        return request;
    }

    private static final class CapturingServlet extends SetSystemPasswordServlet {
        private String storedPassword;

        @Override
        protected void storePassword(String password) {
            storedPassword = password;
        }
    }
}
