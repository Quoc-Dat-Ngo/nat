package natty.internal_network;

import natty.helper.Helper;

public class UDPHeader {
  public static void setUDPHeader(byte[] packet, int srcPort, int dstPort, byte[] dataBytes) {
    // Source Port
    // 16 bits
    packet[20] = (byte) (srcPort >> 8);
    packet[21] = (byte) (srcPort & 0xFF);

    // Destination Port
    // 16 bits
    packet[22] = (byte) (dstPort >> 8);
    packet[23] = (byte) (dstPort & 0xFF);

    // Length
    // 16 bits
    int udpTotalLength = Helper.MAX_SIZE_UDP_HEADER + dataBytes.length;
    packet[24] = (byte) (udpTotalLength >> 8);
    packet[25] = (byte) (udpTotalLength & 0xFF);

    // Checksum
    // 16 bits
    packet[26] = 0;
    packet[27] = 0;

    // Pseudo header + UDP header + data
    // 12 bytes + 8 bytes () + dataBytes.length
    byte[] udpPacket = new byte[Helper.MAX_SIZE_PSEUDO_HEADER + Helper.MAX_SIZE_UDP_HEADER + dataBytes.length];

    // Construct IPv4 Pseudo-Header fields for UDP Checksum
    byte[] pseudoHeaderFields = { 127, 0, 0, 1, 127, 0, 0, 1, 0, 17, packet[24], packet[25] }; // Need to reconstruct
                                                                                               // hard-coded srcIP and
                                                                                               // dstIP
    System.arraycopy(pseudoHeaderFields, 0, udpPacket, 0, pseudoHeaderFields.length);
    // Construct UDP header for UDP Checksum
    System.arraycopy(packet, 20, udpPacket, 12, 8);
    // Construct data for UDP Checksum
    System.arraycopy(dataBytes, 0, udpPacket, 20, dataBytes.length);

    // Handle odd length UDP packet
    if (udpPacket.length % 2 != 0) {
      byte[] padded = new byte[udpPacket.length + 1];
      System.arraycopy(udpPacket, 0, padded, 0, udpPacket.length);
      udpPacket = padded;
    }

    long sum = computeUDPChecksum(udpPacket);
    int checksum = (~(int) sum & 0xFFFF);

    // Stage 4: Checksum Verification and Update
    if (checksum == 0x0000) {
      checksum = 0xFFFF;
    }

    packet[26] = (byte) (checksum >> 8);
    packet[27] = (byte) (checksum & 0xFF);
  }

  public static long computeUDPChecksum(byte[] udpPacket) {
    long sum = 0;
    int length = udpPacket.length;

    for (int i = 0; i < length; i += 2) {
      int word;

      if (i + 1 < length) {
        word = ((udpPacket[i] & 0xFF) << 8) | (udpPacket[i + 1] & 0xFF);
      } else {
        word = (udpPacket[i] & 0xFF) << 8;
      }

      sum += word;

      // wrap around (16 bits)
      if ((sum & 0xFFFF0000) != 0) {
        sum = (sum & 0xFFFF) + (sum >> 16);
      }
    }

    while ((sum >> 16) != 0) {
      sum = (sum & 0xFFFF) + (sum >> 16);
    }

    return sum;
  }
}
