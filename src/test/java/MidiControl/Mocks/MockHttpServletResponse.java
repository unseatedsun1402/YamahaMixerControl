package MidiControl.Mocks;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Locale;

public class MockHttpServletResponse implements HttpServletResponse {
    private int status = 200;    //OK
    private final StringWriter stringWriter = new StringWriter();
    private final PrintWriter writer = new PrintWriter(stringWriter);

    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    public String getContentAsString() {
        writer.flush();
        return stringWriter.toString();
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
    }

    @Override
    public void sendError(int sc) throws IOException {
        this.status = sc;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        this.status = sc;
    }

    @Override
    public int getStatus() {
        return this.status;
    }

    // --- Minimal stubs required by interface ---

    @Override public void setContentType(String type) { /* ignored */ }
    @Override public void setCharacterEncoding(String charset) { /* ignored */ }

    // --- Everything else can throw for now ---
    @Override public String getCharacterEncoding() { throw new UnsupportedOperationException(); }
    @Override public String getContentType() { throw new UnsupportedOperationException(); }
    @Override public ServletOutputStream getOutputStream() throws IOException { throw new UnsupportedOperationException(); }
    @Override public void setContentLength(int len) { throw new UnsupportedOperationException(); }
    @Override public void setContentLengthLong(long len) { throw new UnsupportedOperationException(); }
    @Override public void setBufferSize(int size) { throw new UnsupportedOperationException(); }
    @Override public int getBufferSize() { throw new UnsupportedOperationException(); }
    @Override public void flushBuffer() throws IOException { throw new UnsupportedOperationException(); }
    @Override public void resetBuffer() { throw new UnsupportedOperationException(); }
    @Override public boolean isCommitted() { throw new UnsupportedOperationException(); }
    @Override public void reset() { throw new UnsupportedOperationException(); }
    @Override public void setLocale(Locale loc) { throw new UnsupportedOperationException(); }
    @Override public Locale getLocale() { throw new UnsupportedOperationException(); }

    // Headers & status (unused by BuildPage)
    @Override public void sendRedirect(String location) { throw new UnsupportedOperationException(); }
    @Override public void setHeader(String name, String value) { throw new UnsupportedOperationException(); }
    @Override public void addHeader(String name, String value) { throw new UnsupportedOperationException(); }
    @Override public void setIntHeader(String name, int value) { throw new UnsupportedOperationException(); }
    @Override public void addIntHeader(String name, int value) { throw new UnsupportedOperationException(); }
    @Override public void setDateHeader(String name, long date) { throw new UnsupportedOperationException(); }
    @Override public void addDateHeader(String name, long date) { throw new UnsupportedOperationException(); }
    @Override public void setTrailerFields(java.util.function.Supplier<java.util.Map<String,String>> supplier) { throw new UnsupportedOperationException(); }
    @Override public java.util.function.Supplier<java.util.Map<String,String>> getTrailerFields() { throw new UnsupportedOperationException(); }

    @Override
    public void addCookie(Cookie cookie) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addCookie'");
    }

    @Override
    public boolean containsHeader(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'containsHeader'");
    }

    @Override
    public String encodeURL(String url) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'encodeURL'");
    }

    @Override
    public String encodeRedirectURL(String url) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'encodeRedirectURL'");
    }

    @Override
    public String getHeader(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHeader'");
    }

    @Override
    public Collection<String> getHeaders(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHeaders'");
    }

    @Override
    public Collection<String> getHeaderNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHeaderNames'");
    }
}