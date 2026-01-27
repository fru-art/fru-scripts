package com.osmbtoolkit.utils;

import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmbtoolkit.script.ToolkitScript;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class Paint {
  static public class Origin {
    public final int originX;
    public final int originY;

    public Origin(int x, int y) {
      originX = x;
      originY = y;
    }

    public Point translate(int x, int y) {
      return new Point(x + originX, y + originY);
    }

    public Origin translateSelf(int x, int y) {
      return new Origin(originX + x, originY + y);
    }
  }

  public static final Font RUNESCAPE_BOLD_FONT;
  static {
    try {
      RUNESCAPE_BOLD_FONT = Font.createFont(Font.TRUETYPE_FONT,
        Objects.requireNonNull(ToolkitScript.class.getResourceAsStream("/fonts/runescape_bold.ttf"))).deriveFont(20f);
    } catch (FontFormatException | IOException e) {
      throw new RuntimeException(e);
    }
  }
  public static final Font RUNESCAPE_FONT;
  static {
    try {
      RUNESCAPE_FONT = Font.createFont(Font.TRUETYPE_FONT,
        Objects.requireNonNull(ToolkitScript.class.getResourceAsStream("/fonts/runescape.ttf"))).deriveFont(20f);
    } catch (FontFormatException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Image applyCircleMask(Image image) {
    BufferedImage bi = image.toBufferedImage();
    int size = Math.min(bi.getWidth(), bi.getHeight());
    BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = output.createGraphics();

    // 1. Enable Antialiasing for smooth edges
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // 2. Draw the "Mask" (the circle)
    g2.setColor(Color.WHITE);
    g2.fill(new Ellipse2D.Double(0, 0, size, size));

    // 3. Use SRC_IN composite: Only keep pixels where the source AND mask overlap
    g2.setComposite(AlphaComposite.SrcIn);
    g2.drawImage(bi, 0, 0, null);

    g2.dispose();
    return new Image(output);
  }

  public static void drawImage(Canvas canvas, Image image, int x, int y) {
    drawImage(canvas, image, x, y, Integer.MIN_VALUE);
  }
  public static void drawImage(Canvas canvas, Image image, int x, int y, int height) {
    if (height != Integer.MIN_VALUE) {
      image = resizeImage(image, height);
    }
    canvas.drawAtOn(image, x, y);
  }

  /**
   * @return image if successfully loaded and drawn
   */
  public static Optional<Image> drawImage(Canvas canvas, String source, int x, int y) {
    return drawImage(canvas, source, x, y, Integer.MIN_VALUE);
  }
  /**
   * @return image if successfully loaded and drawn
   */
  public static Optional<Image> drawImage(Canvas canvas, String source, int x, int y, int height) {
    Optional<Image> maybeImage = loadImage(source);
    if (maybeImage.isEmpty()) return Optional.empty();
    Image image = maybeImage.get();

    drawImage(canvas, image, x, y, height);
    return Optional.of(image);
  }

  public static void drawPolygon(Canvas canvas, Polygon polygon, Color color) {
    canvas.fillPolygon(polygon, color.getRGB(), 0.3);
    canvas.drawPolygon(polygon, color.getRGB(), 0.6);
  }

  public static Origin getOrigin(int x, int y) {
    return new Origin(x, y);
  }

  public static Color saturate(Color color, float multiplier) {
    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float newSaturation = hsb[1] * multiplier;
    return Color.getHSBColor(hsb[0], newSaturation, hsb[2]);
  }

  /**
   * Loads an image from a URL string or a local file path.
   * @param source The path (e.g., "C:/images/logo.png") or URL (e.g., "https://site.com/logo.png")
   * @return An OSMB Image object, or null if loading fails.
   */
  public static Optional<Image> loadImage(String source) {
    try {
      BufferedImage bi;

      // Check if the source starts with a protocol (http, https, ftp, etc.)
      if (source.contains("://")) {
        // Treat as URL
        bi = ImageIO.read(URI.create(source).toURL());
      } else {
        // Treat as local file path
        bi = ImageIO.read(new File(source));
      }

      return bi == null ? Optional.empty() : Optional.of(new Image(bi));
    } catch (IOException | IllegalArgumentException e) {
      System.err.println("Failed to load image from: " + source + " - " + e.getMessage());
      return Optional.empty();
    }
  }

  public static Image resizeImage(Image image, int desiredHeight) {
    double scale = (double) desiredHeight / image.getHeight();
    return image.resize((int) (scale * image.getWidth()), (int) (scale * image.getHeight()));
  }
}
