package natty.external_network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class NextHop {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println(
          "Required arguments: <real_next_hop_port>");
      return;
    }
    int nextHopPort = Integer.parseInt(args[0]);

    /*
     * create server socket that is assigned the serverPort (6789)
     * We will listen on this port for requests from clients
     * DatagramSocket specifies that we are using UDP
     */
    DatagramSocket serverSocket = new DatagramSocket(nextHopPort);
    System.out.println("Server is ready on port " + nextHopPort + "...");

    // prepare buffers

    while (true) {
      byte[] receiveData = new byte[1024];

      // receive UDP datagram
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      serverSocket.receive(receivePacket);

      // get info of the client with whom we are communicating
      InetAddress IPAddress = receivePacket.getAddress();
      int port = receivePacket.getPort();

      System.out.println("Packet received from NAT");
      System.out.println("Source IP Address: " + IPAddress.getHostAddress());
      System.out.println("Source port: " + port);

      // get data
      byte[] data = Arrays.copyOfRange(receivePacket.getData(), 0, receivePacket.getLength());

      System.out.println("Before swap, Source IP: " + Arrays.toString(Arrays.copyOfRange(data, 12, 16)));
      System.out.println("Before swap, Source Port: " + ((data[20] & 0xFF) << 8 | (data[21] & 0xFF)));

      // Store a copy of dstIP and dstPort
      byte[] tempIP = Arrays.copyOfRange(data, 16, 20);
      byte[] tempPort = Arrays.copyOfRange(data, 22, 24);

      System.arraycopy(data, 12, data, 16, 4); // Overwrite dstIP with existing srcIP
      System.arraycopy(tempIP, 0, data, 12, tempIP.length); // Overwrite existing srcIP with tempIP

      System.arraycopy(data, 20, data, 22, 2); // Overwrite dstPort with existing srcPort
      System.arraycopy(tempPort, 0, data, 20, tempPort.length); // Overwrite existing srcPort with tempPort

      System.out.println("After swap, Source IP: " + Arrays.toString(Arrays.copyOfRange(data, 12, 16)));
      System.out.println("After swap, Source Port: " + ((data[20] & 0xFF) << 8 | (data[21] & 0xFF)));

      // send it back to client
      DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
      serverSocket.send(sendPacket);

    } // end of while (true)

  } // end of main()
}
