package natty.components;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Arrays;

import natty.helper.Helper;
import natty.internal_network.IPHeader;
import natty.internal_network.UDPHeader;

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

  public static DatagramPacket toDatagram(Packet p, InetAddress addr, int port) {
    byte[] packet = new byte[Helper.MAX_SIZE_IP_HEADER + Helper.MAX_SIZE_UDP_HEADER + p.payload.length];

    IPHeader.setIPHeader(packet, p.srcIP, p.dstIP, p.payload.length);
    UDPHeader.setUDPHeader(packet, p.srcPort, p.dstPort, p.payload);
    System.arraycopy(p.payload, 0, packet, 28, p.payload.length);

    return new DatagramPacket(
        packet, packet.length, addr, port);
  }
}
