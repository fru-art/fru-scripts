package com.osmbtoolkit.utils;

import com.osmb.api.shape.Polygon;
import com.osmb.api.visual.drawing.Canvas;

import java.awt.Color;

public class Paint {
  public static void drawPolygon(Canvas canvas, Polygon polygon, Color color) {
    canvas.fillPolygon(polygon, color.getRGB(), 0.3);
    canvas.drawPolygon(polygon, color.getRGB(), 0.6);
  }

  public static Color saturate(Color color, float multiplier) {
    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
    float newSaturation = hsb[1] * multiplier;
    return Color.getHSBColor(hsb[0], newSaturation, hsb[2]);
  }
}
