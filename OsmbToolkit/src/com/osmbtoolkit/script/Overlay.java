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
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class Overlay {
  private final Runnable frameListener = this::setGameState;

  private boolean isBankVisible;
  private boolean isLoggedIn;
  private ToolkitScript script;
  private int totalHeight = 100;
  private int totalWidth = 0;

  public Overlay(ToolkitScript script) {
    this.script = script;
    this.script.addFrameListener(frameListener);
  }

  public void draw(Canvas canvas) {
    ScriptDefinition definition = script.scriptDefinition;
    Screen screen = script.getScreen();
    if (screen == null) return;
    Rectangle screenBounds = screen.getBounds();
    if (screenBounds == null) return;

    int smallGap = 3;
    int gap = 6;
    int x = gap;
    int y = gap;
    int textWidth;
    int textHeight = 16;
    Font font = Paint.RUNESCAPE_FONT.deriveFont((float) textHeight);
    int black = Color.BLACK.getRGB();
    int color = Color.WHITE.getRGB();
    int subduedColor = 0xFF9E9E9E;
    int subduedHeight = 12;
    // Use 16 as derived size since the small font just renders small but uses the same units.
    Font subduedFont = Paint.RUNESCAPE_SMALL_FONT.deriveFont((float) 16);
    setTotalWidth(x);

    Paint.Origin origin = Paint.getOrigin(84, screenBounds.getHeight() - totalHeight - 12);

    List<Runnable> draws = new ArrayList<>();

    int headerHeight = 16;
    // Logo
    Optional<Image> baseLogo = script.getLogoImage();
    if (baseLogo.isPresent()) {
      Image logo = Paint.resizeImage(baseLogo.get(), 32);
      Point logoPoint = origin.translate(x, y);
      draws.add(() -> Paint.drawImage(canvas, logo, logoPoint.x, logoPoint.y));
      x += logo.getWidth() + gap;
      setTotalWidth(x);
    }
    int headerTextX = x;
    // Name
    Point namePoint = origin.translate(x, y);
    Font headerFont = Paint.RUNESCAPE_BOLD_FONT.deriveFont((float) headerHeight);
    draws.add(() -> canvas.drawText(
      definition.name(),
      namePoint.x + 1,
      namePoint.y + headerHeight + 1,
      black,
      headerFont));
    draws.add(() -> canvas.drawText(definition.name(), namePoint.x, namePoint.y + headerHeight, color, headerFont));
    textWidth = canvas.getFontMetrics(headerFont).stringWidth(definition.name());
    x += textWidth + smallGap;
    setTotalWidth(x);
    // Version
    Point versionPoint = origin.translate(x, y + headerHeight - subduedHeight);
    String versionString = "v" + definition.version();
    draws.add(() -> canvas.drawText(
      versionString,
      versionPoint.x + 1,
      versionPoint.y + subduedHeight + 1,
      black,
      subduedFont));
    draws.add(() -> canvas.drawText(
      versionString,
      versionPoint.x,
      versionPoint.y + subduedHeight,
      subduedColor,
      subduedFont));
    textWidth = canvas.getFontMetrics(subduedFont).stringWidth(versionString);
    x += textWidth + gap;
    setTotalWidth(x);
    // Job label
    Optional<Job> job = script.getCurrentJobDebounced();
    String jobLabel = job.isEmpty() ? "Waiting for job..." : "Running:";
    x = headerTextX; // New row
    y += textHeight + smallGap;
    Point jobLabelPoint = origin.translate(x, y);
    draws.add(() -> canvas.drawText(
      jobLabel,
      jobLabelPoint.x + 1,
      jobLabelPoint.y + subduedHeight + 1,
      black,
      subduedFont));
    draws.add(() -> canvas.drawText(
      jobLabel,
      jobLabelPoint.x,
      jobLabelPoint.y + subduedHeight,
      subduedColor,
      subduedFont));
    textWidth = canvas.getFontMetrics(subduedFont).stringWidth(jobLabel);
    x += textWidth + smallGap;
    setTotalWidth(x);
    // Job string
    if (job.isPresent()) {
      Point jobPoint = origin.translate(x, y);
      String jobString = job.get().toString().isEmpty() ? "Unknown" : job.get().toString();
      draws.add(() -> canvas.drawText(jobString, jobPoint.x + 1, jobPoint.y + subduedHeight + 1, black, subduedFont));
      draws.add(() -> canvas.drawText(jobString, jobPoint.x, jobPoint.y + subduedHeight, color, subduedFont));
      textWidth = canvas.getFontMetrics(subduedFont).stringWidth(jobString);
      x += textWidth + gap;
      setTotalWidth(x);
    }
    // Divider (needs total width, drawn later)
    y += textHeight + smallGap;
    Point dividerPoint = origin.translate(0, y);

    // "Made by"
    x = gap; // New row
    y += gap;
    Point madeByPoint = origin.translate(x, y);
    String madeBy = "Made by";
    draws.add(() -> canvas.drawText(madeBy, madeByPoint.x + 1, madeByPoint.y + subduedHeight + 1, black, subduedFont));
    draws.add(() -> canvas.drawText(madeBy, madeByPoint.x, madeByPoint.y + subduedHeight, subduedColor, subduedFont));
    textWidth = canvas.getFontMetrics(subduedFont).stringWidth(madeBy);
    x += textWidth + gap;
    setTotalWidth(x);
    // Author logo
    x = gap; // New row
    y += subduedHeight + smallGap;
    Optional<Image> baseAuthorLogo = script.getAuthorLogoImage();
    if (baseAuthorLogo.isPresent()) {
      Image authorLogo = Paint.applyCircleMask(Paint.resizeImage(baseAuthorLogo.get(), textHeight));
      Point authorLogoPoint = origin.translate(x, y);
      draws.add(() -> Paint.drawImage(canvas, authorLogo, authorLogoPoint.x, authorLogoPoint.y));
      x += authorLogo.getWidth() + gap;
      setTotalWidth(x);
    }
    // Author
    Point authorPoint = origin.translate(x, y);
    draws.add(() -> canvas.drawText(
      definition.author(),
      authorPoint.x + 1,
      authorPoint.y + textHeight + 1,
      black,
      font));
    draws.add(() -> canvas.drawText(definition.author(), authorPoint.x, authorPoint.y + textHeight, color, font));
    textWidth = canvas.getFontMetrics(font).stringWidth(definition.author());
    x += textWidth + gap;
    setTotalWidth(x);
    // "with OSMB Toolkit"
    x = gap; // New row
    y += textHeight + smallGap;
    Point withOsmbToolkitPoint = origin.translate(x, y);
    String withOsmbToolkit = "with OSMB Toolkit";
    draws.add(() -> canvas.drawText(
      withOsmbToolkit,
      withOsmbToolkitPoint.x + 1,
      withOsmbToolkitPoint.y + subduedHeight + 1,
      black,
      subduedFont));
    draws.add(() -> canvas.drawText(
      withOsmbToolkit,
      withOsmbToolkitPoint.x,
      withOsmbToolkitPoint.y + subduedHeight,
      subduedColor,
      subduedFont));
    textWidth = canvas.getFontMetrics(subduedFont).stringWidth(withOsmbToolkit);
    x += textWidth + gap;
    setTotalWidth(x);
    // Calculate total height
    y += subduedHeight + gap;
    setTotalHeight(y);

    canvas.fillRect(
      new Rectangle(origin.originX, origin.originY, totalWidth + smallGap, totalHeight),
      Color.BLACK.getRGB(),
      isBankVisible ? 0.75 : isLoggedIn ? 0.1 : 0.9);
    canvas.drawLine(
      dividerPoint.x,
      dividerPoint.y,
      dividerPoint.x + totalWidth + smallGap,
      dividerPoint.y,
      Color.WHITE.getRGB(),
      0.5);

    for (Runnable draw : draws) {
      draw.run();
    }
  }

  // Listens to frames as opposed to grabbing these values on paint, which potentially causes stack overflow
  private void setGameState() {
    WidgetManager widgetManager = script.getWidgetManager();
    Bank bank = widgetManager == null ? null : widgetManager.getBank();
    GameState gameState = widgetManager == null ? null : widgetManager.getGameState();

    this.isBankVisible = gameState == GameState.LOGGED_IN && bank != null && bank.isVisible();
    this.isLoggedIn = gameState == GameState.LOGGED_IN;
  }

  private int setTotalHeight(int nextTotalHeight) {
    if (nextTotalHeight < totalHeight) totalHeight = nextTotalHeight;
    return totalHeight;
  }
  private int setTotalWidth(int nextTotalWidth) {
    if (nextTotalWidth > totalWidth) totalWidth = nextTotalWidth;
    return totalWidth;
  }
}
