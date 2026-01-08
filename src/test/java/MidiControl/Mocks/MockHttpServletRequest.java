package MidiControl.Mocks;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

public class MockHttpServletRequest implements HttpServletRequest {

    private final MockServletContext context;
    private final Map<String, String> parameters = new HashMap<>();

    public MockHttpServletRequest(MockServletContext ctx) {
        this.context = ctx;
    }

    // --- Parameter handling ---
    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    @Override
    public String getParameter(String name) {
        return parameters.get(name);
    }

    // --- Servlet context ---
    @Override
    public ServletContext getServletContext() {
        return context;
    }

    // --- Everything else is unsupported for now ---
    @Override public String getAuthType() { throw new UnsupportedOperationException(); }
    @Override public Cookie[] getCookies() { throw new UnsupportedOperationException(); }
    @Override public long getDateHeader(String name) { throw new UnsupportedOperationException(); }
    @Override public String getHeader(String name) { throw new UnsupportedOperationException(); }
    @Override public Enumeration<String> getHeaders(String name) { throw new UnsupportedOperationException(); }
    @Override public Enumeration<String> getHeaderNames() { throw new UnsupportedOperationException(); }
    @Override public int getIntHeader(String name) { throw new UnsupportedOperationException(); }
    @Override public String getMethod() { throw new UnsupportedOperationException(); }
    @Override public String getPathInfo() { throw new UnsupportedOperationException(); }
    @Override public String getPathTranslated() { throw new UnsupportedOperationException(); }
    @Override public String getContextPath() { throw new UnsupportedOperationException(); }
    @Override public String getQueryString() { throw new UnsupportedOperationException(); }
    @Override public String getRemoteUser() { throw new UnsupportedOperationException(); }
    @Override public boolean isUserInRole(String role) { throw new UnsupportedOperationException(); }
    @Override public Principal getUserPrincipal() { throw new UnsupportedOperationException(); }
    @Override public String getRequestedSessionId() { throw new UnsupportedOperationException(); }
    @Override public String getRequestURI() { throw new UnsupportedOperationException(); }
    @Override public StringBuffer getRequestURL() { throw new UnsupportedOperationException(); }
    @Override public String getServletPath() { throw new UnsupportedOperationException(); }
    @Override public HttpSession getSession(boolean create) { throw new UnsupportedOperationException(); }
    @Override public HttpSession getSession() { throw new UnsupportedOperationException(); }
    @Override public boolean isRequestedSessionIdValid() { throw new UnsupportedOperationException(); }
    @Override public boolean isRequestedSessionIdFromCookie() { throw new UnsupportedOperationException(); }
    @Override public boolean isRequestedSessionIdFromURL() { throw new UnsupportedOperationException(); }
    @Override public boolean authenticate(HttpServletResponse response) { throw new UnsupportedOperationException(); }
    @Override public void login(String username, String password) { throw new UnsupportedOperationException(); }
    @Override public void logout() { throw new UnsupportedOperationException(); }
    @Override public Collection<Part> getParts() { throw new UnsupportedOperationException(); }
    @Override public Part getPart(String name) { throw new UnsupportedOperationException(); }

    // Input stream / reader
    @Override public String getCharacterEncoding() { throw new UnsupportedOperationException(); }
    @Override public void setCharacterEncoding(String env) { throw new UnsupportedOperationException(); }
    @Override public int getContentLength() { throw new UnsupportedOperationException(); }
    @Override public long getContentLengthLong() { throw new UnsupportedOperationException(); }
    @Override public String getContentType() { throw new UnsupportedOperationException(); }
    @Override public ServletInputStream getInputStream() { throw new UnsupportedOperationException(); }
    @Override public String getProtocol() { throw new UnsupportedOperationException(); }
    @Override public String getScheme() { throw new UnsupportedOperationException(); }
    @Override public String getServerName() { throw new UnsupportedOperationException(); }
    @Override public int getServerPort() { throw new UnsupportedOperationException(); }
    @Override public String getRemoteAddr() { throw new UnsupportedOperationException(); }
    @Override public String getRemoteHost() { throw new UnsupportedOperationException(); }
    @Override public void setAttribute(String name, Object o) { throw new UnsupportedOperationException(); }
    @Override public void removeAttribute(String name) { throw new UnsupportedOperationException(); }
    @Override public Locale getLocale() { throw new UnsupportedOperationException(); }
    @Override public Enumeration<Locale> getLocales() { throw new UnsupportedOperationException(); }
    @Override public boolean isSecure() { throw new UnsupportedOperationException(); }
    @Override public int getRemotePort() { throw new UnsupportedOperationException(); }
    @Override public String getLocalName() { throw new UnsupportedOperationException(); }
    @Override public String getLocalAddr() { throw new UnsupportedOperationException(); }
    @Override public int getLocalPort() { throw new UnsupportedOperationException(); }

    @Override
    public Object getAttribute(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttribute'");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributeNames'");
    }

    @Override
    public Enumeration<String> getParameterNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParameterNames'");
    }

    @Override
    public String[] getParameterValues(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParameterValues'");
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParameterMap'");
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReader'");
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestDispatcher'");
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startAsync'");
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'startAsync'");
    }

    @Override
    public boolean isAsyncStarted() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAsyncStarted'");
    }

    @Override
    public boolean isAsyncSupported() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAsyncSupported'");
    }

    @Override
    public AsyncContext getAsyncContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAsyncContext'");
    }

    @Override
    public DispatcherType getDispatcherType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDispatcherType'");
    }

    @Override
    public String getRequestId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestId'");
    }

    @Override
    public String getProtocolRequestId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProtocolRequestId'");
    }

    @Override
    public ServletConnection getServletConnection() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServletConnection'");
    }

    @Override
    public String changeSessionId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'changeSessionId'");
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'upgrade'");
    }
}