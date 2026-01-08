package MidiControl.Mocks;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;


public class MockServletContext implements jakarta.servlet.ServletContext {

    private final Map<String, Object> attributes = new HashMap<>();
    private final Map<String, String> realPaths = new HashMap<>();

    public void setRealPathMapping(String virtualPath, String realPath) {
        realPaths.put(virtualPath, realPath);
    }

    @Override
    public String getRealPath(String path) {
        return realPaths.get(path);
    }


    @Override
    public void setAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    // Everything else can throw UnsupportedOperationException
    @Override public String getContextPath() { throw new UnsupportedOperationException(); }
    @Override public jakarta.servlet.ServletContext getContext(String uripath) { throw new UnsupportedOperationException(); }
    @Override public int getMajorVersion() { throw new UnsupportedOperationException(); }
    @Override public int getMinorVersion() { throw new UnsupportedOperationException(); }
    @Override public int getEffectiveMajorVersion() { throw new UnsupportedOperationException(); }
    @Override public int getEffectiveMinorVersion() { throw new UnsupportedOperationException(); }
    @Override public String getMimeType(String file) { throw new UnsupportedOperationException(); }
    @Override public Set<String> getResourcePaths(String path) { throw new UnsupportedOperationException(); }
    @Override public URL getResource(String path) { throw new UnsupportedOperationException(); }
    @Override public InputStream getResourceAsStream(String path) { throw new UnsupportedOperationException(); }
    @Override public RequestDispatcher getRequestDispatcher(String path) { throw new UnsupportedOperationException(); }
    @Override public RequestDispatcher getNamedDispatcher(String name) { throw new UnsupportedOperationException(); }
    @Override public void log(String msg) { throw new UnsupportedOperationException(); }
    @Override public void log(String message, Throwable throwable) { throw new UnsupportedOperationException(); }
    @Override public String getServerInfo() { throw new UnsupportedOperationException(); }
    @Override public String getInitParameter(String name) { throw new UnsupportedOperationException(); }
    @Override public Enumeration<String> getInitParameterNames() { throw new UnsupportedOperationException(); }
    @Override public boolean setInitParameter(String name, String value) { throw new UnsupportedOperationException(); }
    @Override public void removeAttribute(String name) { throw new UnsupportedOperationException(); }
    @Override public String getServletContextName() { throw new UnsupportedOperationException(); }

    @Override
    public Dynamic addServlet(String servletName, String className) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addServlet'");
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addServlet'");
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addServlet'");
    }

    @Override
    public Dynamic addJspFile(String servletName, String jspFile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addJspFile'");
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createServlet'");
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServletRegistration'");
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getServletRegistrations'");
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addFilter'");
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addFilter'");
    }

    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName,
            Class<? extends Filter> filterClass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addFilter'");
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createFilter'");
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFilterRegistration'");
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFilterRegistrations'");
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSessionCookieConfig'");
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSessionTrackingModes'");
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDefaultSessionTrackingModes'");
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEffectiveSessionTrackingModes'");
    }

    @Override
    public void addListener(String className) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addListener'");
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addListener'");
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addListener'");
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createListener'");
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getJspConfigDescriptor'");
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getClassLoader'");
    }

    @Override
    public void declareRoles(String... roleNames) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'declareRoles'");
    }

    @Override
    public String getVirtualServerName() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getVirtualServerName'");
    }

    @Override
    public int getSessionTimeout() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSessionTimeout'");
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSessionTimeout'");
    }

    @Override
    public String getRequestCharacterEncoding() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestCharacterEncoding'");
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setRequestCharacterEncoding'");
    }

    @Override
    public String getResponseCharacterEncoding() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getResponseCharacterEncoding'");
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setResponseCharacterEncoding'");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAttributeNames'");
    }
}

