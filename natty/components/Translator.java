package natty.components;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Translator {
  private static Map<String, NatEntry> internalMapping = new ConcurrentHashMap<>();
  private static Map<Integer, NatEntry> externalMapping = new ConcurrentHashMap<>();
  private static PortAllocator portAllocator;

  public static Map<String, NatEntry> getInternalMapping() {
    return internalMapping;
  }

  public static Map<Integer, NatEntry> getExternalMapping() {
    return externalMapping;
  }

  public static Packet translateOutBound(Packet p, String extIP, int numExtPorts, InetAddress clientAddr,
      int clientPort) {
    String internalNetworkKey = p.getSrcIP() + ":" + p.getSrcPort();
    NatEntry entry;

    if (!internalMapping.containsKey(internalNetworkKey)) {
      if (portAllocator == null) {
        synchronized (Translator.class) {
          if (portAllocator == null) {
            portAllocator = new PortAllocator(numExtPorts);
          }
        }
      }

      int externalPort = portAllocator.getNextAvailablePort();

      entry = new NatEntry(p.getSrcIP(), extIP, p.getSrcPort(), externalPort, clientAddr, clientPort);
      internalMapping.put(internalNetworkKey, entry);
      externalMapping.put(entry.getExternalPort(), entry);
    } else {
      entry = internalMapping.get(internalNetworkKey);
    }

    p.setSrcIP(entry.getExternalIP());
    p.setSrcPort(entry.getExternalPort());
    entry.setLastUsedTime(System.currentTimeMillis());
    entry.setInternalKey(internalNetworkKey);

    return p;
  }

  public static Packet translateInBound(Packet p) {
    System.out.println(internalMapping.toString());
    System.out.println(externalMapping.toString());
    NatEntry entry = externalMapping.get(p.getDstPort());

    if (entry == null) {
      // Handle no mapping --> drop packet
      return null;
    }

    p.setDstIP(entry.getClientIPAddr().getHostAddress());
    p.setDstPort(entry.getClientPort());
    entry.setLastUsedTime(System.currentTimeMillis());

    return p;
  }

  public static void handleCleanUpIdleMapping(int timeout) {
    new Thread(() -> {
      while (true) {
        long now = System.currentTimeMillis();

        Iterator<Entry<Integer, NatEntry>> iter = externalMapping.entrySet().iterator();

        while (iter.hasNext()) {
          Entry<Integer, NatEntry> mappingEntry = iter.next();
          NatEntry entry = mappingEntry.getValue();

          if (now - entry.getLastUsedTime() > timeout * 1000) {
            iter.remove();

            internalMapping.remove(entry.getInternalKey());
            portAllocator.releasePort(entry.getExternalPort());
          }
        }

        try {
          Thread.sleep(1000); // check once per second
        } catch (InterruptedException e) {
          break;
        }

      }
    }, "Idle Timout Thread").start();
  }
}