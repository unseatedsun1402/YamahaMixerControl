package MidiControl.Services;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class GetServerIP extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String base = req.getScheme() + "://" +
                      detectLanIp() + ":" +
                      req.getServerPort() + "/MidiControl";

        String json = String.format(
            "{\"type\":\"loc-request\",\"payload\":\"%s\"}", 
            base.replace("\"", "\\\"")
        );

        resp.setContentType("application/json");
        resp.getWriter().print(json);
    }

    
    private String detectLanIp() {
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();

                // Skip unusable adapters
                if (!nic.isUp() || nic.isLoopback() || nic.isPointToPoint()) continue;

                // Skip virtual adapters by common names
                String name = nic.getDisplayName().toLowerCase();
                if (name.contains("virtual")
                    || name.contains("vmware")
                    || name.contains("vbox")
                    || name.contains("hyper-v")
                    || name.contains("docker")
                    || name.contains("bridge")
                    || name.contains("tap")
                    || name.contains("tun")
                    || name.contains("hamachi")
                    || name.contains("zt")) continue;

                // Skip adapters with no usable MAC address
                byte[] mac = nic.getHardwareAddress();
                if (mac == null || isZeroMac(mac)) continue;

                // Iterate addresses
                for (InetAddress addr : Collections.list(nic.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}

        return "127.0.0.1"; // fallback
    }

    private boolean isZeroMac(byte[] mac) {
        for (byte b : mac) {
            if (b != 0) return false;
        }
        return true;
    }

}