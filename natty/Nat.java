package natty;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import natty.components.Packet;
import natty.components.Translator;

public class Nat {
  public static void main(String[] args) throws Exception {
    if (args.length != 6) {
      System.out.println(
          "Required arguments: <external_ip> <num_external_ports> <timeout> <mtu> <real_internal_port> <real_next_hop_port>");
      return;
    }

    String externalIP = args[0];
    int numExtPorts = Integer.parseInt(args[1]);
    int timeout = Integer.parseInt(args[2]);
    int mtu = Integer.parseInt(args[3]);
    int internalPort = Integer.parseInt(args[4]);
    int nextHopPort = Integer.parseInt(args[5]);

    DatagramSocket internalSocket = new DatagramSocket(internalPort);
    DatagramSocket externalSocket = new DatagramSocket();
    System.out.println("Internal socket is listening on port " + internalPort + "...");

    new Thread(() -> {
      try {
        while (true) {
          byte[] receiveData = new byte[1024];
          DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
          internalSocket.receive(receivePacket);

          System.out.println("--------------------------------------------");
          System.out.println("Packet from Client received, translating...");

          InetAddress clientAddr = receivePacket.getAddress();
          int clientPort = receivePacket.getPort();

          Packet internalPacket = Packet.fromDatagram(receivePacket);
          System.out.println("BEFORE TRANSLATION");
          System.out.println("Source IP Address: " + internalPacket.getSrcIP());
          System.out.println("Source port: " + internalPacket.getSrcPort());
          System.out.println("Destination IP Address: " + internalPacket.getDstIP());
          System.out.println("Destination port: " + internalPacket.getDstPort());

          Packet mappedPacket = Translator.translateOutBound(
              internalPacket,
              externalIP,
              numExtPorts,
              clientAddr,
              clientPort);

          System.out.println("\nAFTER TRANSLATION");
          System.out.println("Source IP Address: " + mappedPacket.getSrcIP());
          System.out.println("Source port: " + mappedPacket.getSrcPort());
          System.out.println("Destination IP Address: " + mappedPacket.getDstIP());
          System.out.println("Destination port: " + mappedPacket.getDstPort());

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

    new Thread(() -> {
      try {
        while (true) {
          byte[] receiveData = new byte[1024];
          DatagramPacket receivePacket = new DatagramPacket(
              receiveData,
              receiveData.length);
          externalSocket.receive(receivePacket);

          System.out.println("--------------------------------------------");
          System.out.println("Packet from Next Hop received, translating back...");

          Packet externalPacket = Packet.fromDatagram(receivePacket);

          System.out.println("BEFORE TRANSLATION");
          System.out.println("Source IP Address: " + externalPacket.getSrcIP());
          System.out.println("Source port: " + externalPacket.getSrcPort());
          System.out.println("Destination IP Address: " + externalPacket.getDstIP());
          System.out.println("Destination port: " + externalPacket.getDstPort());

          Packet mappedBack = Translator.translateInBound(externalPacket);

          if (mappedBack != null) {
            System.out.println("\nAFTER TRANSLATION");
            System.out.println("Source IP Address: " + mappedBack.getSrcIP());
            System.out.println("Source port: " + mappedBack.getSrcPort());
            System.out.println("Destination IP Address: " + mappedBack.getDstIP());
            System.out.println("Destination port: " + mappedBack.getDstPort());

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

    Translator.handleCleanUpIdleMapping(timeout);
  }
}
