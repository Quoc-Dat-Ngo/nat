package natty.components;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles NAT translation logic for both outbound and inbound traffic.
 * This class maintains two concurrent mapping tables:
 * - internalMapping: maps (internal IP:port) → NatEntry
 * - externalMapping: maps external port → NatEntry (for reverse lookup)
 *
 * Responsibilities:
 * - Perform source NAT (internal → external) by assigning external ports
 * - Perform reverse NAT (external → internal) using stored mappings
 * - Manage port allocation via PortAllocator
 * - Periodically clean up idle/expired NAT mappings to free resources
 */
public class Translator {
  // Maps (internal IP, port) → NAT entry
  private static Map<String, NatEntry> internalMapping = new ConcurrentHashMap<>();
  // Maps external port → NAT entry for reverse lookup
  private static Map<Integer, NatEntry> externalMapping = new ConcurrentHashMap<>();
  private static PortAllocator portAllocator;

  public static Map<String, NatEntry> getInternalMapping() {
    return internalMapping;
  }

  public static Map<Integer, NatEntry> getExternalMapping() {
    return externalMapping;
  }

  /**
   * Performs outbound NAT translation:
   * - Checks if a mapping exists for (internal IP:port)
   * - If not, allocates a new external port and creates a mapping
   * - Rewrites packet source IP/port to external values
   * - Updates last used time for timeout tracking
   * 
   * @param p
   * @param extIP
   * @param numExtPorts
   * @param clientAddr
   * @param clientPort
   * @return a pre-defined class Packet to keep a record of srcIP, srcPort, dstIP
   *         and dstPort
   */
  public static Packet translateOutBound(Packet p, String extIP, int numExtPorts, InetAddress clientAddr,
      int clientPort) {
    String internalNetworkKey = p.getSrcIP() + ":" + p.getSrcPort();
    NatEntry entry;

    if (!internalMapping.containsKey(internalNetworkKey)) {
      // Lazy initialization of PortAllocator (thread-safe)
      if (portAllocator == null) {
        synchronized (Translator.class) {
          if (portAllocator == null) {
            portAllocator = new PortAllocator(numExtPorts);
          }
        }
      }

      // Allocate a new external port and create NAT entry
      int externalPort = portAllocator.getNextAvailablePort();
      entry = new NatEntry(p.getSrcIP(), extIP, p.getSrcPort(), externalPort, clientAddr, clientPort);

      internalMapping.put(internalNetworkKey, entry);
      externalMapping.put(entry.getExternalPort(), entry);
    } else {
      // Reuse existing mapping
      entry = internalMapping.get(internalNetworkKey);
    }
    // Rewrite packet source to external mapping
    p.setSrcIP(entry.getExternalIP());
    p.setSrcPort(entry.getExternalPort());

    // Update metadata for timeout handling
    entry.setLastUsedTime(System.currentTimeMillis());
    entry.setInternalKey(internalNetworkKey);

    return p;
  }

  /**
   * Performs inbound (reverse) NAT translation:
   * - Looks up mapping using destination external port
   * - If mapping exists, rewrites destination to internal IP/port
   * - If not, drops the packet (returns null)
   * 
   * @param p
   * @return the Packet from the mapping(ConcurrentHasmap) if such exists.
   */
  public static Packet translateInBound(Packet p) {
    System.out.println(internalMapping.toString());
    System.out.println(externalMapping.toString());

    NatEntry entry = externalMapping.get(p.getDstPort());
    if (entry == null) {
      // Handle no mapping --> drop packet
      return null;
    }

    // Rewrite packet destination back to internal endpoint
    p.setDstIP(entry.getInternalIP());
    p.setDstPort(entry.getInternalPort());

    // Update last used time
    entry.setLastUsedTime(System.currentTimeMillis());

    return p;
  }

  /**
   * Starts a background thread to clean up idle NAT mappings:
   * - Iterates over external mappings periodically (every 1 second)
   * - Removes entries that exceed the timeout threshold
   * - Frees associated external ports via PortAllocator
   * 
   * @param timeout
   */
  public static void handleCleanUpIdleMapping(int timeout) {
    new Thread(() -> {
      while (true) {
        long now = System.currentTimeMillis();

        Iterator<Entry<Integer, NatEntry>> iter = externalMapping.entrySet().iterator();

        while (iter.hasNext()) {
          Entry<Integer, NatEntry> mappingEntry = iter.next();
          NatEntry entry = mappingEntry.getValue();

          // Check if entry has been idle longer than timeout
          if (now - entry.getLastUsedTime() > timeout * 1000) {
            iter.remove();

            // Remove corresponding internal mapping and release port
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