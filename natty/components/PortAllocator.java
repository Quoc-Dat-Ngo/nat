package natty.components;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PortAllocator {
  private final Set<Integer> usedPortTracker = ConcurrentHashMap.newKeySet();
  private final int numExtPorts;

  public PortAllocator(int numExtPorts) {
    this.numExtPorts = numExtPorts;
  }

  public synchronized void releasePort(int port) {
    usedPortTracker.remove(port);
  }

  public synchronized int getNextAvailablePort() {
    int port = 1;
    while (usedPortTracker.contains(port) && port <= numExtPorts) {
      port++;
    }

    if (port > numExtPorts) {
      throw new RuntimeException("No available ports");
    }

    usedPortTracker.add(port);

    return port;
  }
}
