package mdc;

import com.sun.image.codec.jpeg.ImageFormatException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by cosmin on 06.03.2016.
 */
public abstract class Config {
    // Use multicast ip
    public static final String DEFAULT_IP_ADDRESS = "225.4.5.6";
    public static final int DEFAULT_PORT = 4444;

    public static final String IMAGE_OUTPUT_FORMAT = "jpg";
    public static final double DEFAULT_SCALING_FACTOR = 0.5;
    public static final int DEFAULT_SLEEP_MILLIS = 2000;

    // Protocol
    public static int SESSION_START = 128;
    public static int SESSION_END = 64;
    public static int HEADER_SIZE = 8;

    /**
     * Takes a screenshot (fullscreen)
     *
     * @return Sreenshot
     * @throws java.awt.AWTException
     * @throws com.sun.image.codec.jpeg.ImageFormatException
     * @throws java.io.IOException
     */
    public static BufferedImage getScreenshot() throws AWTException,
            ImageFormatException, IOException {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        Rectangle screenRect = new Rectangle(screenSize);

        Robot robot = new Robot();
        BufferedImage image = robot.createScreenCapture(screenRect);

        return image;
    }

    /**
     * Converts BufferedImage to byte array
     *
     * @param image Image to convert
     * @return Byte Array
     * @throws java.io.IOException
     */
    public static byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, IMAGE_OUTPUT_FORMAT, baos);
        return baos.toByteArray();
    }

    /**
     * Scales a buffered image
     *
     * @param source Image to scale
     * @param w      New image width
     * @param h      New image height
     * @return Scaled image
     */
    public static BufferedImage scaleBufferedImage(BufferedImage source, int w, int h) {
        Image image = source
                .getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    /**
     * Shrinks a BufferedImage
     *
     * @param source Image to shrink
     * @param factor Scaling factor
     * @return Scaled image
     */
    public static BufferedImage shrinkBufferedImage(BufferedImage source, double factor) {
        int w = (int) (source.getWidth() * factor);
        int h = (int) (source.getHeight() * factor);
        return Config.scaleBufferedImage(source, w, h);
    }

    /**
     * Draws mouse pointer into the image.
     */
    public static void drawMousePointer(BufferedImage image) {
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
}
