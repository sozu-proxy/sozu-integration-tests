package utils;

// Sozu backend infos
public class Backend {

    private String id;

    private String address;

    private int port;

    public Backend(String id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getAddressWithPort() {
        return address + ":" + port;
    }
}
