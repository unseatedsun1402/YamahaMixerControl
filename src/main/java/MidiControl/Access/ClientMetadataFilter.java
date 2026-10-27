package MidiControl.Access;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

import MidiControl.Routing.WebSocketEndpoint;

public final class ClientMetadataFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if (httpRequest.getRequestURI().endsWith("/endpoint")) {
            HttpSession httpSession = httpRequest.getSession(true);
            httpSession.setAttribute(
                    WebSocketEndpoint.HTTP_SESSION_CLIENT_IP,
                    httpRequest.getRemoteAddr());
        }
        chain.doFilter(request, response);
    }
}
