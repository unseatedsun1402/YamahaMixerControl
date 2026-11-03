package MidiControl.Services;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import MidiControl.MidiDeviceManager.ServerSettings;

public class GetServerIP extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String action = req.getParameter("action");
        String preferredIp = req.getParameter("preferredIp");

        if ("list".equals(action) || "interfaces".equals(action) || req.getParameter("list") != null) {
            resp.setContentType("application/json");
            resp.getWriter().print(buildInterfaceListJson());
            return;
        }

        if ("select".equals(action) || preferredIp != null) {
            String selected = preferredIp == null ? req.getParameter("preferredHost") : preferredIp;
            if (selected != null && !selected.trim().isEmpty()) {
                ServerSettings settings = new ServerSettings();
                settings.setPreferredHostAddress(selected.trim());
                settings.saveSettings();
            }
            resp.setContentType("application/json");
            resp.getWriter().print(String.format("{\"type\":\"network-selection\",\"payload\":{\"success\":true,\"selected\":\"%s\"}}",
                    selected == null ? "" : selected.replace("\"", "\\\"")));
            return;
        }

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

    private String buildInterfaceListJson() {
        List<String> addresses = getInterfaceAddresses();
        ServerSettings settings = new ServerSettings();
        String preferred = settings.getPreferredHostAddress();

        StringBuilder payload = new StringBuilder("[");
        for (int i = 0; i < addresses.size(); i++) {
            String address = addresses.get(i);
            boolean isPreferred = address.equals(preferred);
            String label = isPreferred ? "Preferred" : "Available";
            if (i > 0) payload.append(',');
            payload.append(String.format("{\"address\":\"%s\",\"label\":\"%s\",\"preferred\":%s}",
                    address.replace("\"", "\\\""),
                    label,
                    Boolean.toString(isPreferred)));
        }
        payload.append(']');

        return String.format("{\"type\":\"network-selection\",\"payload\":%s}", payload);
    }

    private List<String> getInterfaceAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback() || nic.isPointToPoint()) continue;

                String name = nic.getDisplayName().toLowerCase(Locale.ROOT);
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

                byte[] mac = nic.getHardwareAddress();
                if (mac == null || isZeroMac(mac)) continue;

                for (InetAddress addr : Collections.list(nic.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String host = addr.getHostAddress();
                        if (!addresses.contains(host)) {
                            addresses.add(host);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return addresses;
    }

    private String detectLanIp() {
        String preferred = new ServerSettings().getPreferredHostAddress();
        if (preferred != null && !preferred.trim().isEmpty()) {
            return preferred.trim();
        }

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            String fallbackIp = null;

            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();

                if (!nic.isUp() || nic.isLoopback() || nic.isPointToPoint()) continue;

                String name = nic.getDisplayName().toLowerCase(Locale.ROOT);
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

                byte[] mac = nic.getHardwareAddress();
                if (mac == null || isZeroMac(mac)) continue;

                for (InetAddress addr : Collections.list(nic.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        if (isWireless(nic)) {
                            return addr.getHostAddress();
                        }
                        if (fallbackIp == null) {
                            fallbackIp = addr.getHostAddress();
                        }
                    }
                }
            }

            if (fallbackIp != null) {
                return fallbackIp;
            }
        } catch (Exception ignored) {}

        return "127.0.0.1";
    }

    private boolean isWireless(NetworkInterface nic) {
        String interfaceName = (
                nic.getName() + " " + nic.getDisplayName()
        ).toLowerCase(Locale.ROOT);

        return interfaceName.contains("wireless")
                || interfaceName.contains("wi-fi")
                || interfaceName.contains("wifi")
                || interfaceName.contains("wlan");
    }

    private boolean isZeroMac(byte[] mac) {
        for (byte b : mac) {
            if (b != 0) return false;
        }
        return true;
    }

}