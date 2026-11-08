package MidiControl.Routing;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public class WebSocketPeerAddressFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if ("websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            String connectionId = request.getParameter("connectionId");
            WebSocketPeerAddressRegistry.register(connectionId, request.getRemoteAddr());
        }
        filterChain.doFilter(request, response);
    }
}
