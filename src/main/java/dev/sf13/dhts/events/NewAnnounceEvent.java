package dev.sf13.dhts.events;

import java.net.InetAddress;

public class NewAnnounceEvent {
    private final String infoHashHex;
    private final InetAddress ip;
    private final int port;

    public NewAnnounceEvent(String infoHashHex, InetAddress ip, int port) {
        this.infoHashHex = infoHashHex;
        this.ip = ip;
        this.port = port;
    }

    public String getInfoHashHex() {
        return infoHashHex;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
