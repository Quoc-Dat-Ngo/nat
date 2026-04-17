package natty.internal_network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import natty.components.Packet;
import natty.helper.Helper;

public class ClientB {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println(
          "Required arguments: <real_internal_port> <real_next_hop_port>");
      return;
    }

    // Define socket parameters, address and Port No
    InetAddress IPAddress = InetAddress.getByName("localhost");
    int internalSocketPort = Integer.parseInt(args[0]);
    int nextHopPort = Integer.parseInt(args[1]);

    // create socket which connects to internal NAT socket
    DatagramSocket clientSocket = new DatagramSocket();

    // Actual data sending
    int data = 1234789;
    byte[] dataBytes = ByteBuffer.allocate(4).putInt(data).array();

    // construct simulated IP Header, UDP Header, and actual Data
    byte[] logicalPacket = new byte[1024];

    // Set IP Header of the packet
    IPHeader.setIPHeader(logicalPacket, Helper.IP_ADDRESS, Helper.IP_ADDRESS,
        dataBytes.length);
    // Set UDP Header of the packet
    UDPHeader.setUDPHeader(logicalPacket, clientSocket.getLocalPort(), nextHopPort, dataBytes);
    // Set the actual payload/data into the packet
    System.arraycopy(dataBytes, 0, logicalPacket, 28, dataBytes.length);

    // write to server, need to create DatagramPAcket with server address and port
    // No.
    DatagramPacket sendPacket = new DatagramPacket(logicalPacket, logicalPacket.length, IPAddress, internalSocketPort);
    // actual send call
    clientSocket.send(sendPacket);

    // Logging statement for debugging purposes
    System.out.println("Client send packet to NAT on IP Address: "
        + clientSocket.getLocalAddress().getHostAddress() + ", and on port: "
        + clientSocket.getLocalPort());

    // prepare buffer to receive reply
    byte[] receiveData = new byte[1024];
    // receive from server
    DatagramPacket receivePacket = new DatagramPacket(receiveData,
        receiveData.length);
    clientSocket.receive(receivePacket);

    Packet packet = Packet.fromDatagram(receivePacket);

    byte[] payloadBytes = packet.getPayLoad();
    int value = ByteBuffer.wrap(payloadBytes).getInt();
    System.out.println("FROM SERVER (int): " + value);

    // close the scoket
    clientSocket.close();
  } // end of main
}
