package mdc;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

/**
 * Multicast Image Sender
 * Version: 0.1
 *
 * @author Jochen Luell
 */
public class ImageSender {

    /* Flags and sizes */
    public static int HEADER_SIZE = 8;
    public static int MAX_PACKETS = 255;
    public static int SESSION_START = 128;
    public static int SESSION_END = 64;

    public static int MAX_SESSION_NUMBER = 255;

    /*
     * The absolute maximum datagram packet size is 65507, The maximum IP packet
     * size of 65535 minus 20 bytes for the IP header and 8 bytes for the UDP
     * header.
     */
    public static int DATAGRAM_MAX_SIZE = 65507 - HEADER_SIZE;

    /* Default parameters */
    public static double scalingFactor = Config.DEFAULT_SCALING_FACTOR;
    public static int sleepMillis = Config.DEFAULT_SLEEP_MILLIS;
    public static boolean showMousePointer = true;
    private static String ipAddress = Config.DEFAULT_IP_ADDRESS;
    private static int port = Config.DEFAULT_PORT;

    public static void main(String[] args) {
        ImageSender sender = new ImageSender();
        int sessionNumber = 0;


        // Create Frame
        JFrame frame = new JFrame("Multicast Image Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel();
        frame.getContentPane().add(label);
        frame.setVisible(true);


        handleArgs(args);

        label.setText("Multicasting screenshots...");

        frame.pack();

        try {
            /* Continuously send images */
            while (true) {
                BufferedImage image;

                image = Config.getScreenshot();

					/* Draw mousepointer into image */
                if (showMousePointer) {
                    PointerInfo p = MouseInfo.getPointerInfo();
                    int mouseX = p.getLocation().x;
                    int mouseY = p.getLocation().y;

                    Graphics2D g2d = image.createGraphics();
                    g2d.setColor(Color.red);
                    Polygon polygon1 = new Polygon(new int[]{mouseX, mouseX + 10, mouseX, mouseX},
                            new int[]{mouseY, mouseY + 10, mouseY + 15, mouseY}
                            , 4);

                    Polygon polygon2 = new Polygon(new int[]{mouseX + 1, mouseX + 10 + 1, mouseX + 1, mouseX + 1},
                            new int[]{mouseY + 1, mouseY + 10 + 1, mouseY + 15 + 1, mouseY + 1}
                            , 4);
                    g2d.setColor(Color.black);
                    g2d.fill(polygon1);

                    g2d.setColor(Color.red);
                    g2d.fill(polygon2);
                    g2d.dispose();
                }


				/* Scale image */
                image = Config.shrinkBufferedImage(image, scalingFactor);
                byte[] imageByteArray = Config.bufferedImageToByteArray(image);
                int noOfPackets = (int) Math.ceil(imageByteArray.length / (float) DATAGRAM_MAX_SIZE);

				/* If image has more than MAX_PACKETS slices -> error */
                if (noOfPackets > MAX_PACKETS) {
                    System.out.println("Image is too large to be transmitted!");
                    continue;
                }

				/* Loop through slices */
                for (int i = 0; i <= noOfPackets; i++) {
                    int flags = 0;
                    flags = i == 0 ? flags | SESSION_START : flags;
                    flags = (i + 1) * DATAGRAM_MAX_SIZE > imageByteArray.length ? flags | SESSION_END : flags;

                    int size = (flags & SESSION_END) != SESSION_END ? DATAGRAM_MAX_SIZE : imageByteArray.length - i * DATAGRAM_MAX_SIZE;

					/* Set additional header */
                    byte[] data = new byte[HEADER_SIZE + size];
                    data[0] = (byte) flags;
                    data[1] = (byte) sessionNumber;
                    data[2] = (byte) noOfPackets;
                    data[3] = (byte) (DATAGRAM_MAX_SIZE >> 8);
                    data[4] = (byte) DATAGRAM_MAX_SIZE;
                    data[5] = (byte) i;
                    data[6] = (byte) (size >> 8);
                    data[7] = (byte) size;

					/* Copy current slice to byte array */
                    System.arraycopy(imageByteArray, i * DATAGRAM_MAX_SIZE, data, HEADER_SIZE, size);
                    /* Send multicast packet */
                    sender.sendImage(data, ipAddress, port);

					/* Leave loop if last slice has been sent */
                    if ((flags & SESSION_END) == SESSION_END) break;
                }
                /* Sleep */
                Thread.sleep(sleepMillis);

				/* Increase session number */
                sessionNumber = sessionNumber < MAX_SESSION_NUMBER ? ++sessionNumber : 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle command line arguments
     */
    private static void handleArgs(String[] args) {
        switch (args.length) {
            case 5:
                ipAddress = args[4];
            case 4:
                port = Integer.parseInt(args[3]);
            case 3:
                showMousePointer = Integer.parseInt(args[2]) == 1 ? true : false;
            case 2:
                sleepMillis = Integer.parseInt(args[1]) * 1000;
            case 1:
                scalingFactor = Double.parseDouble(args[0]);
        }
    }

    /**
     * Sends a byte array via multicast
     * Multicast addresses are IP addresses in the range of 224.0.0.0 to
     * 239.255.255.255.
     *
     * @param imageData        Byte array
     * @param multicastAddress IP multicast address
     * @param port             Port
     * @return <code>true</code> on success otherwise <code>false</code>
     */
    private boolean sendImage(byte[] imageData, String multicastAddress, int port) {
        InetAddress inetAddress;

        boolean result = false;
        int ttl = 2;

        try {
            inetAddress = InetAddress.getByName(multicastAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return result;
        }

        MulticastSocket socket = null;

        try {
            socket = new MulticastSocket();
            socket.setTimeToLive(ttl);
            DatagramPacket dp = new DatagramPacket(imageData, imageData.length,
                    inetAddress, port);
            socket.send(dp);
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }

        return result;
    }

}