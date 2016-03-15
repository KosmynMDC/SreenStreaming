package mdc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

/**
 * Multicast Image Receiver
 * Version: 0.1
 * <p/>
 * Created by cosmin on 06.03.2016.
 */
public class ImageReceiver {
    public static double scalingFactor = Config.DEFAULT_SCALING_FACTOR;

    /* Default values */
    private static String ipAddress = Config.DEFAULT_IP_ADDRESS;
    private static int port = Config.DEFAULT_PORT;
    private static boolean debug = true;

    /* Ui Frame */
    JFrame frame;
    JWindow fullscreenWindow;
    boolean fullscreen = false;
    JLabel labelImage;
    JLabel windowImage;

    /**
     * MAIN
     *
     * @param args
     */
    public static void main(String[] args) throws AWTException {
        handleArgs(args);

        ImageReceiver receiver = new ImageReceiver();
        receiver.createUiFrame();
        receiver.start(ipAddress, port);
    }

    /**
     * Handle command line arguments
     */
    private static void handleArgs(String[] args) {
        switch (args.length) {
            case 2:
                ipAddress = args[1];
            case 1:
                port = Integer.parseInt(args[0]);
        }
    }

    /**
     * Create Ui Frame.
     */
    private void createUiFrame() {
        labelImage = new JLabel();
        windowImage = new JLabel();

        /* Create frame */
        frame = new JFrame("Multicast Image Receiver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(labelImage);
        frame.setSize(300, 10);
        frame.setVisible(true);
        frame.addKeyListener(new FrameKeyListener());

		/* Construct full screen window */
        fullscreenWindow = new JWindow();
        fullscreenWindow.getContentPane().add(windowImage);
        fullscreenWindow.addKeyListener(new FrameKeyListener());
    }

    /**
     * Receives image.
     *
     * @param multicastAddress IP multicast adress
     * @param port             Port
     */
    private void start(String multicastAddress, int port) throws AWTException {
        InetAddress inetAddress = null;
        MulticastSocket socket = null;

        try {
            /* Get address */
            inetAddress = InetAddress.getByName(multicastAddress);

			/* Setup socket and join group */
            socket = new MulticastSocket(port);
            socket.joinGroup(inetAddress);

            /* Start receiving images */
            receiveImages(socket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    /* Leave group and close socket */
                    socket.leaveGroup(inetAddress);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Receiving images on socket.
     *
     * @param socket
     * @throws IOException
     */
    private void receiveImages(MulticastSocket socket) throws IOException, AWTException {
        BufferedImage testImage = Config.getScreenshot();
        Graphics2D g = testImage.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, testImage.getWidth(), testImage.getHeight());
        g.dispose();
        byte[] imageData = Config.bufferedImageToByteArray(testImage);

		/* Setup byte array to store data received */
        byte[] buffer = new byte[Config.DATAGRAM_PACKET_DATA_MAX_SIZE];

		/* Receiving loop */
        while (true) {
            /* Receive a UDP packet */
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            socket.receive(dp);
            byte[] data = dp.getData();

			/* Read header infomation */
            int entireWidth = (int) ((data[0] & 0xff) << 8 | (data[1] & 0xff)); // mask the sign bit
            int entireHeight = (int) ((data[2] & 0xff) << 8 | (data[3] & 0xff)); // mask the sign bit
            short sliceIndexStart = (short) (data[4] & 0xff);
            int sliceSize = (int) ((data[5] & 0xff) << 8 | (data[6] & 0xff)); // mask the sign bit

            if (debug) {
                System.out.println("------------- PACKET -------------");
                System.out.println("ENTIRE WIDTH = " + entireWidth);
                System.out.println("ENTIRE HEIGHT = " + entireHeight);
                System.out.println("SLICE INDEX START = " + sliceIndexStart);
                System.out.println("SLICE SIZE = " + sliceSize);
                System.out.println("------------- PACKET -------------\n");
            }

			/* If package belongs to current session */
            System.arraycopy(data, Config.HEADER_SIZE, imageData, sliceIndexStart
                    * Config.DATAGRAM_PACKET_IMAGE_DATA_SIZE, sliceSize);

			/* If image is complete display it */
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);
            labelImage.setIcon(new ImageIcon(image));
            windowImage.setIcon(new ImageIcon(image));

            frame.pack();
        }
    }

    private class FrameKeyListener implements KeyListener {

        /**
         * (non-Javadoc)
         *
         * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
         */
        @Override
        public void keyPressed(KeyEvent keyevent) {
            GraphicsDevice device = GraphicsEnvironment
                    .getLocalGraphicsEnvironment().getDefaultScreenDevice();

            /* Toggle full screen mode on key press */
            if (fullscreen) {
                device.setFullScreenWindow(null);
                fullscreenWindow.setVisible(false);
                fullscreen = false;
            } else {
                device.setFullScreenWindow(fullscreenWindow);
                fullscreenWindow.setVisible(true);
                fullscreen = true;
            }

        }

        @Override
        public void keyReleased(KeyEvent keyevent) {
        }

        @Override
        public void keyTyped(KeyEvent keyevent) {
        }
    }
}
