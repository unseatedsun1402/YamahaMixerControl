package MidiControl.Server;

public record ServerStatus(String state, String networkAddress, String localAddress) {
    public static ServerStatus running(
            String ipAddress,
            int port,
            String localHostName,
            String discoveryError
    ) {
        String networkAddress = address(ipAddress, port);
        String localAddress = localHostName == null
                ? "Unavailable: " + discoveryError
                : address(localHostName, port);
        return new ServerStatus("Running", networkAddress, localAddress);
    }

    private static String address(String host, int port) {
        return "http://" + host + ":" + port + "/MidiControl/";
    }
}
