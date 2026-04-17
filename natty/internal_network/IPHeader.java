package natty.internal_network;

import natty.helper.Helper;

public class IPHeader {
  public static void setIPHeader(byte[] packet, String srcIP, String dstIP, int dataLength) {
    // simulated IP Header
    // Version + IHL (Internet Header Length)
    // 4 bits + 4 bits
    packet[0] = 0x45;

    // DSCP (Differentiated Services Code Point) + ECN (Explicit Congestion
    // Notification)
    // 6 bits + 2 bits
    packet[1] = 0;

    // Total Length
    // 16 bits
    int totalLength = Helper.MAX_SIZE_IP_HEADER + Helper.MAX_SIZE_UDP_HEADER
        + dataLength;
    packet[2] = (byte) (totalLength >> 8);
    packet[3] = (byte) (totalLength & 0xFF);

    // Identification
    // 16 bits
    int id = 1;
    packet[4] = (byte) (id >> 8);
    packet[5] = (byte) (id & 0xFF);

    // Flags + Fragment Offset
    // 3 bits + 13 bits;
    packet[6] = 0;
    packet[7] = 0;

    // TTL
    // 8 bits
    packet[8] = 64;

    // Protocol
    // 8 bits
    packet[9] = 17;

    // Header Checksum
    // 16 bits
    packet[10] = 0;
    packet[11] = 0;

    /*
     * Since all sockets use the loopback interface (127.0.0.1) for communication.
     */
    // Source Address
    // 32 bits -> X.X.X.X (each X = 1 byte/8 bits)

    String[] srcIPComponents = srcIP.split("\\.");
    packet[12] = (byte) Integer.parseInt(srcIPComponents[0]);
    packet[13] = (byte) Integer.parseInt(srcIPComponents[1]);
    packet[14] = (byte) Integer.parseInt(srcIPComponents[2]);
    packet[15] = (byte) Integer.parseInt(srcIPComponents[3]);

    // Destination Address
    // 32 bits -> X.X.X.X (each X = 1 byte/8 bits)

    String[] dstIPComponents = dstIP.split("\\.");
    packet[16] = (byte) Integer.parseInt(dstIPComponents[0]);
    packet[17] = (byte) Integer.parseInt(dstIPComponents[1]);
    packet[18] = (byte) Integer.parseInt(dstIPComponents[2]);
    packet[19] = (byte) Integer.parseInt(dstIPComponents[3]);

  }
}
