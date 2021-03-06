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

            /* Takes a screenshot */
            ;
            BufferedImage image = Config.getScreenshot();

			/* Draw mousepointer into image */
            if (showMousePointer) {
                Config.drawMousePointer(image);
            }

			/* Scale image */
            image = Config.shrinkBufferedImage(image, scalingFactor);
            oldImageByteArray = imageByteArray;
            imageByteArray = Config.bufferedImageToByteArray(image);
            int noOfSlices = (int) Math.ceil(imageByteArray.length / (float) Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE);

			/* If image has more than MAX_PACKETS slices -> error */
            if (noOfSlices > MAX_PACKETS) {
                System.out.println("Image is too large to be transmitted!");
                continue;
            }

			/* Loop through slices */
            for (int sliceIndex = 0; sliceIndex < noOfSlices; sliceIndex++) {
                boolean noChanges = Config.sameImagesSlice(sliceIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE,
                        sliceIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE + Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE,
                        oldImageByteArray, imageByteArray);
                if (noChanges) {
                    continue;
                }

                int sliceSize = sliceIndex < noOfSlices - 1 ? Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE : imageByteArray.length - sliceIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE;

				/* Set additional header */
                byte[] data = new byte[Config.HEADER_SIZE + sliceSize];
                data[0] = (byte) (image.getWidth() >> 8);
                data[1] = (byte) image.getWidth();
                data[2] = (byte) (image.getHeight() >> 8);
                data[3] = (byte) image.getHeight();
                data[4] = (byte) sliceIndex;
                data[5] = (byte) (sliceSize >> 8);
                data[6] = (byte) sliceSize;

                System.out.println("------------- PACKET -------------");
                System.out.println("ENTIRE WIDTH = " + image.getWidth());
                System.out.println("ENTIRE HEIGHT = " + image.getHeight());
                System.out.println("SLICE INDEX START = " + sliceIndex);
                System.out.println("SLICE SIZE = " + sliceSize);
                System.out.println("------------- PACKET -------------\n");

				/* Copy current slice to byte array */
                System.arraycopy(imageByteArray, sliceIndex * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE, data, Config.HEADER_SIZE, sliceSize);

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