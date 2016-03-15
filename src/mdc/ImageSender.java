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
 * <p/>
 * Created by cosmin on 06.03.2016.
 */
public class ImageSender {

    /* Sizes */
    public static int MAX_PACKETS = 255;

    /* Default parameters */
    public static double scalingFactor = Config.DEFAULT_SCALING_FACTOR;
    public static int sleepMillis = Config.DEFAULT_SLEEP_MILLIS;
    public static boolean showMousePointer = true;
    private static String ipAddress = Config.DEFAULT_IP_ADDRESS;
    private static int port = Config.DEFAULT_PORT;

    /**
     * MAIN
     *
     * @param args
     */
    public static void main(String[] args) throws IOException, AWTException, InterruptedException {
        handleArgs(args);

        ImageSender sender = new ImageSender();
        sender.createUiFrame();
        sender.start();
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
     * Create Ui Frame.
     */
    private void createUiFrame() {
        // Create Frame
        JFrame frame = new JFrame("Multicast Image Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel label = new JLabel();
        label.setText("Multicasting screenshots...");

        frame.getContentPane().add(label);
        frame.setVisible(true);
        frame.pack();
    }

    /**
     * Gets screenshots and sends them.
     *
     * @throws InterruptedException
     * @throws IOException
     * @throws AWTException
     */
    private void start() throws InterruptedException, IOException, AWTException {
        byte[] imageByteArray = null, oldImageByteArray;

        /* Continuously send images */
        while (true) {

            /* Takes a screenshot */;
            BufferedImage image = Config.getScreenshot();

			/* Draw mousepointer into image */
            if (showMousePointer) {
                Config.drawMousePointer(image);
            }

			/* Scale image */
            image = Config.shrinkBufferedImage(image, scalingFactor);
            oldImageByteArray = imageByteArray;
            imageByteArray = Config.bufferedImageToByteArray(image);
            int noOfPackets = (int) Math.ceil(imageByteArray.length / (float) Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE);

			/* If image has more than MAX_PACKETS slices -> error */
            if (noOfPackets > MAX_PACKETS) {
                System.out.println("Image is too large to be transmitted!");
                continue;
            }

			/* Loop through slices */
            for (int packetIndex = 0; packetIndex < noOfPackets; packetIndex++) {
                boolean hasChanges = Config.compareImages(packetIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE,
                        packetIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE + Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE,
                        oldImageByteArray, imageByteArray);
                if (!hasChanges) {
                    continue;
                }

                int size = packetIndex == noOfPackets ? Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE : imageByteArray.length - packetIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE;

				/* Set additional header */
                byte[] data = new byte[Config.HEADER_SIZE + size];
                data[0] = (byte) (Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE >> 8);
                data[1] = (byte) Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE;
                data[2] = (byte) packetIndex;
                data[3] = (byte) (size >> 8);
                data[4] = (byte) size;

                System.out.println("------------- PACKET -------------");
                System.out.println("MAX PACKET SIZE = " + Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE);
                System.out.println("SLICE NR = " + packetIndex);
                System.out.println("SIZE = " + size);
                System.out.println("------------- PACKET -------------\n");

				/* Copy current slice to byte array */
                System.arraycopy(imageByteArray, packetIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE, data, Config.HEADER_SIZE, size);

                /* Send multicast packet */
                sendMessage(data, ipAddress, port);

            }
            /* Sleep */
            Thread.sleep(sleepMillis);
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
    private boolean sendMessage(byte[] imageData, String multicastAddress, int port) {
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