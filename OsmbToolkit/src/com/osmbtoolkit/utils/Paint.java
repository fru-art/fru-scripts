package com.osmbtoolkit.utils;

import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.visual.PixelCluster;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
  public static final Font RUNESCAPE_SMALL_FONT;
  static {
    try {
      RUNESCAPE_SMALL_FONT = Font.createFont(Font.TRUETYPE_FONT,
        Objects.requireNonNull(ToolkitScript.class.getResourceAsStream("/fonts/runescape_small.ttf"))).deriveFont(20f);
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

  public static void drawCluster(Canvas canvas, PixelCluster cluster, Color color) {
    List<Point> points = cluster.getPoints();
    if (points.isEmpty()) return;

    int minX = points.stream().mapToInt(p -> p.x).min().orElse(0);
    int minY = points.stream().mapToInt(p -> p.y).min().orElse(0);
    int maxX = points.stream().mapToInt(p -> p.x).max().orElse(0);
    int maxY = points.stream().mapToInt(p -> p.y).max().orElse(0);
    int width = maxX - minX + 1;
    int height = maxY - minY + 1;
    int[] pixels = new int[width * height];
    Arrays.fill(pixels, 0);

    int rgb = color.getRGB();
    for (Point p2 : points) {
      int relX = p2.x - minX;
      int relY = p2.y - minY;
      int index = relY * width + relX;
      pixels[index] = rgb;
    }

    canvas.drawPixels(pixels, minX, minY, width, height);
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

  public static Rectangle drawSmallText(Canvas canvas, String text, int x, int y, Color color) {
    return drawSmallText(canvas, text, x, y, color, false);
  }
  public static Rectangle drawSmallText(Canvas canvas, String text, int x, int y, Color color, boolean centered) {
    Font font = Paint.RUNESCAPE_SMALL_FONT.deriveFont((float) 16);
    int textWidth = canvas.getFontMetrics(font).stringWidth(text);
    if (centered) x -= textWidth / 2;
    canvas.drawText(text, x + 1, y + 13, Color.BLACK.getRGB(), font);
    canvas.drawText(text, x, y + 12, color.getRGB(), font);
    return new Rectangle(x, y, textWidth, font.getSize());
  }

  public static Rectangle drawText(Canvas canvas, String text, int x, int y, Color color) {
    return drawText(canvas, text, x, y, color, false, false);
  }
  public static Rectangle drawText(Canvas canvas, String text, int x, int y, Color color, boolean bold, boolean centered) {
    Font font = (bold ? Paint.RUNESCAPE_BOLD_FONT : Paint.RUNESCAPE_FONT).deriveFont((float) 16);
    int textWidth = canvas.getFontMetrics(font).stringWidth(text);
    if (centered) x -= textWidth / 2;
    canvas.drawText(text, x + 1, y + 17, Color.BLACK.getRGB(), font);
    canvas.drawText(text, x, y + 16, color.getRGB(), font);
    return new Rectangle(x, y, textWidth, font.getSize());
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
      if (source.contains("://")) {
        bi = ImageIO.read(URI.create(source).toURL());
      } else {
        bi = ImageIO.read(new File(source));
      }

      if (bi == null) return Optional.empty();

      // 1. Create a standardized ARGB buffer
      BufferedImage standardized = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = standardized.createGraphics();
      g.drawImage(bi, 0, 0, null);
      g.dispose();

      // 2. Manually fix the "Junk Data" in transparent pixels
      int[] pixels = standardized.getRGB(0, 0, standardized.getWidth(), standardized.getHeight(), null, 0, standardized.getWidth());
      for (int i = 0; i < pixels.length; i++) {
        int argb = pixels[i];
        int alpha = (argb >> 24) & 0xFF;

        // If it's even slightly transparent (alpha < 255),
        // OSMB likely doesn't support blending, so we force it to absolute 0.
        if (alpha < 255) {
          pixels[i] = 0;
        }
      }

      // 3. Construct the OSMB Image using the cleaned pixel array
      return Optional.of(new Image(pixels, standardized.getWidth(), standardized.getHeight()));

    } catch (IOException | IllegalArgumentException e) {
      System.err.println("Failed to load image: " + source);
      return Optional.empty();
    }
  }

  public static Image resizeImage(Image image, int desiredHeight) {
    double scale = (double) desiredHeight / image.getHeight();
    return image.resize((int) (scale * image.getWidth()), (int) (scale * image.getHeight()));
  }
}
