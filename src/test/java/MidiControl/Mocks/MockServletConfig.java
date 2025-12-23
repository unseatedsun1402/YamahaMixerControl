package MidiControl.Mocks;


import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import java.util.Enumeration;
import java.util.Collections;

public class MockServletConfig implements ServletConfig {

    private final ServletContext context;

    public MockServletConfig(ServletContext context) {
        this.context = context;
    }

    @Override
    public String getServletName() {
        return "MockServlet";
    }

    @Override
    public ServletContext getServletContext() {
        return context;
    }

    @Override
    public String getInitParameter(String name) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.emptyEnumeration();
    }
}