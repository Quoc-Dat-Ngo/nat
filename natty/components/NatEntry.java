package natty.components;

import java.net.InetAddress;

public class NatEntry {
  private String internalIP;
  private int internalPort;
  private String externalIP;
  private int externalPort;
  private InetAddress clientIPAddr;
  private int clientPort;
  private long lastUsedTime;
  private String internalKey;

  public NatEntry(String internalIP, String externalIP, int internalPort, int externalPort, InetAddress clientIPAddr,
      int clientPort) {
    this.internalIP = internalIP;
    this.internalPort = internalPort;
    this.externalIP = externalIP;
    this.externalPort = externalPort;
    this.clientIPAddr = clientIPAddr;
    this.clientPort = clientPort;
  }

  public InetAddress getClientIPAddr() {
    return clientIPAddr;
  }

  public int getClientPort() {
    return clientPort;
  }

  public String getInternalIP() {
    return internalIP;
  }

  public int getInternalPort() {
    return internalPort;
  }

  public String getExternalIP() {
    return externalIP;
  }

  public int getExternalPort() {
    return externalPort;
  }

  public long getLastUsedTime() {
    return lastUsedTime;
  }

  public void setLastUsedTime(long lastUsedTime) {
    this.lastUsedTime = lastUsedTime;
  }

  public String getInternalKey() {
    return internalKey;
  }

  public void setInternalKey(String internalKey) {
    this.internalKey = internalKey;
  }
}
