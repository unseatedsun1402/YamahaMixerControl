package MidiControl.functional.Services;

import MidiControl.Services.GetServerIP;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class GetServerIPTest {

    static class CapturingResponse {
        final StringWriter body = new StringWriter();
        String contentType;

        HttpServletResponse asHttpServletResponse() {
            InvocationHandler h = (Object proxy, Method method, Object[] args) -> {
                String name = method.getName();

                if ("setContentType".equals(name)) {
                    contentType = (String) args[0];
                    return null;
                }

                if ("getContentType".equals(name)) {
                    return contentType;
                }

                if ("getWriter".equals(name)) {
                    return new PrintWriter(body);
                }

                if ("setCharacterEncoding".equals(name)) return null;
                if ("setStatus".equals(name)) return null;

                Class<?> rt = method.getReturnType();
                if (rt.equals(boolean.class)) return false;
                if (rt.equals(int.class)) return 0;
                if (rt.equals(long.class)) return 0L;

                if (rt.equals(void.class)) return null;

                throw new UnsupportedOperationException("Not implemented: " + method);
            };

            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    h
            );
        }

        String getBody() {
            return body.toString();
        }
    }

    static HttpServletRequest requestWith(String scheme, int port) {
        InvocationHandler h = (Object proxy, Method method, Object[] args) -> {
            String name = method.getName();

            if ("getScheme".equals(name)) return scheme;
            if ("getServerPort".equals(name)) return port;

            if ("isSecure".equals(name)) return false;

            Class<?> rt = method.getReturnType();
            if (rt.equals(boolean.class)) return false;
            if (rt.equals(int.class)) return 0;
            if (rt.equals(long.class)) return 0L;
            if (rt.equals(void.class)) return null;

            return null;
        };

        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                h
        );
    }

    @Test
    public void doGet_returnsValidJsonPayload() throws Exception {
        GetServerIP servlet = new GetServerIP();
        HttpServletRequest request = requestWith("http", 8080);
        CapturingResponse cap = new CapturingResponse();
        HttpServletResponse response = cap.asHttpServletResponse();

        servlet.doGet(request, response);

        String body = cap.getBody();

        assertNotNull(body);
        assertEquals("application/json", cap.contentType);
        assertTrue(body.contains("\"type\":\"loc-request\""));
        assertTrue(body.contains("http://"));
        assertTrue(body.contains(":8080/MidiControl"));
        assertTrue(body.contains("\"payload\":\""));
    }

    @Test
    public void doGet_neverThrows() {
        GetServerIP servlet = new GetServerIP();
        HttpServletRequest request = requestWith("http", 8080);
        CapturingResponse cap = new CapturingResponse();
        HttpServletResponse response = cap.asHttpServletResponse();

        assertDoesNotThrow(() -> servlet.doGet(request, response));
    }
}