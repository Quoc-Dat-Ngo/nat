package natty.components;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

import natty.helper.Helper;

/**
 * Represents a simplified network packet abstraction for the NAT system.
 * This class encapsulates key fields from an IP + UDP packet, including:
 * - Source and destination IP addresses
 * - Source and destination ports
 * - Payload data
 *
 * It provides utility methods to:
 * 1. Parse a raw DatagramPacket into a Packet object (fromDatagram),
 * by extracting IP header fields (bytes 12–19) and UDP header fields
 * (ports, length, and payload).
 * 2. Construct a DatagramPacket (toDatagram) from a Packet object by
 * rebuilding the IP and UDP headers and attaching the payload.
 *
 * This abstraction allows the NAT logic to easily inspect and modify
 * packet-level information (e.g., for address/port translation) without
 * dealing directly with raw byte manipulation throughout the codebase.
 */
public class Packet {
  private String srcIP;
  private String dstIP;
  private int srcPort;
  private int dstPort;
  private byte[] payload;

  public Packet(String srcIP, String dstIP, int srcPort, int dstPort, byte[] payload) {
    this.srcIP = srcIP;
    this.dstIP = dstIP;
    this.srcPort = srcPort;
    this.dstPort = dstPort;
    this.payload = payload;
  }

  public String getDstIP() {
    return dstIP;
  }

  public byte[] getPayLoad() {
    return payload;
  }

  public int getDstPort() {
    return dstPort;
  }

  public String getSrcIP() {
    return srcIP;
  }

  public void setSrcIP(String srcIP) {
    this.srcIP = srcIP;
  }

  public int getSrcPort() {
    return srcPort;
  }

  public void setSrcPort(int srcPort) {
    this.srcPort = srcPort;
  }

  public void setDstIP(String dstIP) {
    this.dstIP = dstIP;
  }

  public void setDstPort(int dstPort) {
    this.dstPort = dstPort;
  }

  // After receiving from Client, start processing bytes to retrieve info
  public static Packet fromDatagram(DatagramPacket dp) {
    byte[] packet = Arrays.copyOfRange(dp.getData(), 0, dp.getLength());

    String srcIP = (int) (packet[12] & 0xFF) + "." + (int) (packet[13] & 0xFF) + "." + (int) (packet[14] & 0xFF) + "."
        + (int) (packet[15] & 0xFF);

    String dstIP = (int) (packet[16] & 0xFF) + "." + (int) (packet[17] & 0xFF) + "." + (int) (packet[18] & 0xFF) + "."
        + (int) (packet[19] & 0xFF);
    /*
     * UDP Header's source port and destination port fields
     * // Source Port
     * // 16 bits
     * packet[20] = (byte) (srcPort >> 8);
     * packet[21] = (byte) (srcPort & 0xFF);
     * 
     * // Destination Port
     * // 16 bits
     * packet[22] = (byte) (dstPort >> 8);
     * packet[23] = (byte) (dstPort & 0xFF);
     */

    int srcPort = ((packet[20] & 0xFF) << 8) | (packet[21] & 0xFF);
    int dstPort = ((packet[22] & 0xFF) << 8) | (packet[23] & 0xFF);

    int udpPacketLength = ((packet[24] & 0xFF) << 8) | (packet[25] & 0xFF);
    int payloadLength = udpPacketLength - Helper.MAX_SIZE_UDP_HEADER;
    byte[] data = Arrays.copyOfRange(packet, 28, 28 + payloadLength);

    return new Packet(srcIP, dstIP, srcPort, dstPort, data);
  }

  // Construct DatagramPacket to send to Next Hop
  public static DatagramPacket toDatagram(Packet p, InetAddress addr, int port) {
    byte[] packet = new byte[Helper.MAX_SIZE_IP_HEADER + Helper.MAX_SIZE_UDP_HEADER + p.payload.length];

    IPHeader.setIPHeader(packet, p.srcIP, p.dstIP, p.payload.length);
    UDPHeader.setUDPHeader(packet, p.srcPort, p.dstPort, p.payload);
    System.arraycopy(p.payload, 0, packet, 28, p.payload.length);

    return new DatagramPacket(
        packet, packet.length, addr, port);
  }
}
