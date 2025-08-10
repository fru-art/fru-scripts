import com.osmb.api.location.position.Position;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;

import java.awt.*;

public class DrawHelper {
  private final Script script;

  public DrawHelper(Script script) {
    this.script = script;
  }

  public void drawPosition(String name, Position position, Color borderColor, Color fillColor) {
    script.getScreen().queueCanvasDrawable(name + "=" + position, (canvas) -> {
      Polygon tilePoly = script.getSceneProjector().getTilePoly(position);

      if (fillColor != null) {
        canvas.fillPolygon(tilePoly, fillColor.getRGB(), 0.3);
      }
      if (borderColor != null) {
        canvas.drawPolygon(tilePoly, borderColor.getRGB(), 0.6);
      }
    });
  }
  public void drawPosition(String name, Position position, Color color) {
    drawPosition(name, position, color, color);
  }

  public void drawPosition(Position position, Color color) {
    drawPosition("position", position, color);
  }
}
