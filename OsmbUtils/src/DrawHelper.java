import com.osmb.api.location.position.Position;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;

import java.awt.*;

public class DrawHelper {
  private final Script script;

  public DrawHelper(Script script) {
    this.script = script;
  }

  public void drawPolygon(String queueKey, Polygon polygon, Color borderColor, Color fillColor) {
    script.getScreen().queueCanvasDrawable(
      queueKey + "=" + polygon,
      canvas -> {
        if (fillColor != null) canvas.fillPolygon(polygon, fillColor.getRGB(), 0.3);
        if (borderColor != null) canvas.drawPolygon(polygon, borderColor.getRGB(), 0.6);
      });
  }
  public void drawPolygon(String queueKey, Polygon polygon, Color color) {
    drawPolygon(queueKey, polygon, color, color);
  }
  public void drawPolygon(Polygon polygon, Color color) {
    drawPolygon("polygon", polygon, color);
  }

  public void drawPosition(String queueKey, Position position, Color borderColor, Color fillColor) {
    Polygon tilePoly = script.getSceneProjector().getTilePoly(position);
    if (tilePoly == null) return;

    this.drawPolygon(queueKey, tilePoly, borderColor, fillColor);
  }
  public void drawPosition(String queueKey, Position position, Color color) {
    drawPosition(queueKey, position, color, color);
  }
  public void drawPosition(Position position, Color color) {
    drawPosition("position", position, color);
  }
}
