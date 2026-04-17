package natty;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import natty.components.Packet;
import natty.components.Translator;

/**
 * Entry point of the NAT application.
 * This class sets up the networking environment and coordinates packet flow
 * between the internal network (clients) and the external network (next
 * hop).
 *
 * Responsibilities:
 * - Parse configuration arguments (external IP, port pool size, timeout,
 * etc.)
 * - Create sockets for internal and external communication
 * - Spawn two threads:
 * 1. Internal Thread: handles outbound traffic (client → NAT → next hop)
 * 2. External Thread: handles inbound traffic (next hop → NAT → client)
 * - Invoke Translator to perform NAT mappings (outbound and inbound)
 * - Start a background cleanup process for removing idle NAT entries
 */
public class Nat {
  public static void main(String[] args) throws Exception {
    // Validate required arguments for NAT configuration
    if (args.length != 6) {
      System.out.println(
          "Required arguments: <external_ip> <num_external_ports> <timeout> <mtu> <real_internal_port> <real_next_hop_port>");
      return;
    }

    // Parse command-line arguments
    String externalIP = args[0];
    int numExtPorts = Integer.parseInt(args[1]);
    int timeout = Integer.parseInt(args[2]);
    int mtu = Integer.parseInt(args[3]);
    int internalPort = Integer.parseInt(args[4]);
    int nextHopPort = Integer.parseInt(args[5]);

    // Socket for receiving packets from internal clients
    DatagramSocket internalSocket = new DatagramSocket(internalPort);

    // Socket for communicating with external network (next hop)
    DatagramSocket externalSocket = new DatagramSocket();
    System.out.println("Internal socket is listening on port " + internalPort + "...");

    // Thread handling outbound traffic (internal → external)
    new Thread(() -> {
      try {
        while (true) {
          // Receive packet from internal client
          byte[] receiveData = new byte[1024];
          DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
          internalSocket.receive(receivePacket);

          System.out.println("--------------------------------------------");
          System.out.println("Packet from Client received, translating...");

          InetAddress clientAddr = receivePacket.getAddress();
          int clientPort = receivePacket.getPort();

          // Parse raw packet into structured Packet object
          Packet internalPacket = Packet.fromDatagram(receivePacket);

          // Log packet before NAT translation
          System.out.println("BEFORE TRANSLATION");
          System.out.println("Source IP Address: " + internalPacket.getSrcIP());
          System.out.println("Source port: " + internalPacket.getSrcPort());
          System.out.println("Destination IP Address: " + internalPacket.getDstIP());
          System.out.println("Destination port: " + internalPacket.getDstPort());

          // Perform outbound NAT (source translation)
          Packet mappedPacket = Translator.translateOutBound(
              internalPacket,
              externalIP,
              numExtPorts,
              clientAddr,
              clientPort);

          // Log packet after NAT translation
          System.out.println("\nAFTER TRANSLATION");
          System.out.println("Source IP Address: " + mappedPacket.getSrcIP());
          System.out.println("Source port: " + mappedPacket.getSrcPort());
          System.out.println("Destination IP Address: " + mappedPacket.getDstIP());
          System.out.println("Destination port: " + mappedPacket.getDstPort());

          // Forward translated packet to next hop
          DatagramPacket sendPacket = Packet.toDatagram(
              mappedPacket,
              InetAddress.getByName("127.0.0.1"),
              nextHopPort);
          externalSocket.send(sendPacket);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, "Internal Thread").start();

    // Thread handling inbound traffic (external → internal)
    new Thread(() -> {
      try {
        while (true) {
          // Receive packet from next hop
          byte[] receiveData = new byte[1024];
          DatagramPacket receivePacket = new DatagramPacket(
              receiveData,
              receiveData.length);
          externalSocket.receive(receivePacket);

          System.out.println("--------------------------------------------");
          System.out.println("Packet from Next Hop received, translating back...");

          // Parse raw packet into Packet object
          Packet externalPacket = Packet.fromDatagram(receivePacket);

          // Log packet before reverse NAT
          System.out.println("BEFORE TRANSLATION");
          System.out.println("Source IP Address: " + externalPacket.getSrcIP());
          System.out.println("Source port: " + externalPacket.getSrcPort());
          System.out.println("Destination IP Address: " + externalPacket.getDstIP());
          System.out.println("Destination port: " + externalPacket.getDstPort());

          // Perform inbound NAT (reverse translation)
          Packet mappedBack = Translator.translateInBound(externalPacket);

          if (mappedBack != null) {
            // Log packet after reverse NAT
            System.out.println("\nAFTER TRANSLATION");
            System.out.println("Source IP Address: " + mappedBack.getSrcIP());
            System.out.println("Source port: " + mappedBack.getSrcPort());
            System.out.println("Destination IP Address: " + mappedBack.getDstIP());
            System.out.println("Destination port: " + mappedBack.getDstPort());

            // Send packet back to the original internal client
            DatagramPacket sendPacket = Packet.toDatagram(
                mappedBack,
                InetAddress.getByName(mappedBack.getDstIP()),
                mappedBack.getDstPort());
            internalSocket.send(sendPacket);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, "External Thread").start();

    // Start background cleanup for idle NAT mappings
    Translator.handleCleanUpIdleMapping(timeout);
  }
}
