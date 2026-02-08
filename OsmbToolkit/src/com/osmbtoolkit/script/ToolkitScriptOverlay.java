package com.osmbtoolkit.script;

import com.osmb.api.screen.Screen;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.utils.Paint;

import java.awt.Color;
import java.awt.Point;
import java.util.Optional;

class ToolkitScriptOverlay {
  private final Runnable frameListener = this::setGameState;

  private boolean isBankVisible;
  private boolean isLoggedIn;
  private final ToolkitScript script;

  // These store dimensions from the previous frame to draw the background
  private int totalHeight = 100;
  private int totalWidth = 0;

  public ToolkitScriptOverlay(ToolkitScript script) {
    this.script = script;
    this.script.addFrameListener(frameListener);
  }

  public void draw(Canvas canvas) {
    ScriptDefinition definition = script.scriptDefinition;
    Screen screen = script.getScreen();
    if (screen == null) return;
    Rectangle screenBounds = screen.getBounds();
    if (screenBounds == null) return;

    // Layout Constants
    final int smallGap = 3;
    final int gap = 6;
    final Color subduedColor = new Color(0xFF9E9E9E, true);
    final Color mainColor = Color.WHITE;

    // Initial Drawing State
    int x = gap;
    int y = gap;
    int currentFrameMaxWidth = 0;

    // Calculate origin using dimensions calculated from the previous frame
    Paint.Origin origin = Paint.getOrigin(84, screenBounds.getHeight() - totalHeight - 12);

    // 1. Draw Background first (sequential drawing)
    // Uses totalWidth/Height from the end of the last draw call
    double opacity = isBankVisible ? 0.75 : isLoggedIn ? 0.2 : 0.9;
    canvas.fillRect(
      new Rectangle(origin.originX, origin.originY, totalWidth + smallGap, totalHeight),
      Color.BLACK.getRGB(),
      opacity);

    // 2. Logo
    Optional<Image> baseLogo = script.getLogoImage();
    if (baseLogo.isPresent()) {
      Image logo = Paint.resizeImage(baseLogo.get(), 32);
      Point p = origin.translate(x, y);
      Paint.drawImage(canvas, logo, p.x, p.y);
      x += logo.getWidth() + gap;
    }

    // 3. Header Section (Name & Version)
    int headerContentX = x;
    Point nameP = origin.translate(x, y);
    Rectangle nameRect = Paint.drawText(canvas, definition.name(), nameP.x, nameP.y, mainColor, true, false);

    x += nameRect.getWidth() + smallGap;

    Point versionP = origin.translate(x, y + 4); // Slight offset to align v. string with bold text
    Rectangle versionRect = Paint.drawSmallText(canvas, "v" + definition.version(), versionP.x, versionP.y, subduedColor);

    currentFrameMaxWidth = Math.max(currentFrameMaxWidth, x + versionRect.getWidth() + gap);

    // 4. Job Section
    x = headerContentX; // New row under name
    y += 18;

    Optional<Job> job = script.getCurrentJobDebounced();
    String jobLabel = job.isEmpty() ? "Waiting for job..." : "Running:";
    Point jobLabelP = origin.translate(x, y);
    Rectangle labelRect = Paint.drawSmallText(canvas, jobLabel, jobLabelP.x, jobLabelP.y, subduedColor);

    x += labelRect.getWidth() + smallGap;

    if (job.isPresent()) {
      String jobString = job.get().toString().isEmpty() ? "Unknown" : job.get().toString();
      Point jobP = origin.translate(x, y);
      Rectangle jobRect = Paint.drawSmallText(canvas, jobString, jobP.x, jobP.y, mainColor);
      currentFrameMaxWidth = Math.max(currentFrameMaxWidth, x + jobRect.getWidth() + gap);
    } else {
      currentFrameMaxWidth = Math.max(currentFrameMaxWidth, x + gap);
    }

    // 5. Divider
    y += 20;
    Point divP = origin.translate(0, y);
    canvas.drawLine(divP.x, divP.y, divP.x + totalWidth + smallGap, divP.y, Color.WHITE.getRGB(), 0.5);

    // 6. Author Section
    x = gap;
    y += gap;

    Point madeByP = origin.translate(x, y);
    Rectangle madeByRect = Paint.drawSmallText(canvas, "Made by", madeByP.x, madeByP.y, subduedColor);
    y += 14;

    // Author Logo & Name
    Optional<Image> baseAuthorLogo = script.getAuthorLogoImage();
    if (baseAuthorLogo.isPresent()) {
      Image authorLogo = Paint.applyCircleMask(Paint.resizeImage(baseAuthorLogo.get(), 16));
      Point authorLogoP = origin.translate(x, y + 2);
      Paint.drawImage(canvas, authorLogo, authorLogoP.x, authorLogoP.y);
      x += authorLogo.getWidth() + gap;
    }

    Point authorP = origin.translate(x, y);
    Rectangle authorRect = Paint.drawText(canvas, definition.author(), authorP.x, authorP.y, mainColor);
    currentFrameMaxWidth = Math.max(currentFrameMaxWidth, x + authorRect.getWidth() + gap);

    // Footer
    x = gap;
    y += 20;
    Point footerP = origin.translate(x, y);
    Rectangle footerRect = Paint.drawSmallText(canvas, "with OSMB Toolkit", footerP.x, footerP.y, subduedColor);
    currentFrameMaxWidth = Math.max(currentFrameMaxWidth, x + footerRect.getWidth() + gap);

    // Update dimensions for the NEXT frame
    this.totalWidth = currentFrameMaxWidth;
    this.totalHeight = y + 18; // Final y position + padding
  }

  private void setGameState() {
    WidgetManager widgetManager = script.getWidgetManager();
    Bank bank = widgetManager == null ? null : widgetManager.getBank();
    GameState gameState = widgetManager == null ? null : widgetManager.getGameState();

    this.isBankVisible = gameState == GameState.LOGGED_IN && bank != null && bank.isVisible();
    this.isLoggedIn = gameState == GameState.LOGGED_IN;
  }
}