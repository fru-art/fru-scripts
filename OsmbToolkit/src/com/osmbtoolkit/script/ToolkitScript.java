package com.osmbtoolkit.script;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.screen.Screen;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.ui.tabs.Inventory;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmbtoolkit.job.JobLoopScript;
import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.options.impl.UpdateAvailableOptions;
import com.osmbtoolkit.utils.Paint;
import com.osmbtoolkit.utils.PausableTimer;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ToolkitScript extends JobLoopScript {
  public final ScriptDefinition scriptDefinition;
  public final PausableTimer pausableTimer;

  private final List<WeakReference<Consumer<Canvas>>> paintListeners = new ArrayList<>();
  private final List<WeakReference<Consumer<Boolean>>> pauseListeners = new ArrayList<>();

  public ToolkitScript(Object scriptCore) {
    super(scriptCore);

    scriptDefinition = this.getClass().getAnnotation(ScriptDefinition.class);
    assert scriptDefinition != null;

    pausableTimer = new PausableTimer(this);
    pausableTimer.attachTo(this);

    try {
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      ge.registerFont(Paint.RUNESCAPE_FONT);
      ge.registerFont(Paint.RUNESCAPE_BOLD_FONT);
    } catch (Exception ignored) {
    }
  }

  public void addPaintListener(Consumer<Canvas> listener) {
    // Avoid duplicates: check if any existing weak ref points to this listener
    boolean exists = paintListeners.stream().anyMatch(ref -> ref.get() == listener);
    if (!exists) {
      paintListeners.add(new WeakReference<>(listener));
    }
  }

  public void addPauseListener(Consumer<Boolean> listener) {
    // Avoid duplicates: check if any existing weak ref points to this listener
    boolean exists = pauseListeners.stream().anyMatch(ref -> ref.get() == listener);
    if (!exists) {
      pauseListeners.add(new WeakReference<>(listener));
    }
  }

  public Optional<PixelCluster> findLargestCluster(Shape searchShape, SearchablePixel[] searchablePixels) {
    return findLargestCluster(searchShape, searchablePixels, 1, 0);
  }

  public Optional<PixelCluster> findLargestCluster(Shape searchShape,
                                                   SearchablePixel[] searchablePixels,
                                                   int maxDistance,
                                                   int minSize) {
    PixelCluster.ClusterQuery clusterQuery = new PixelCluster.ClusterQuery(maxDistance, minSize, searchablePixels);
    return findLargestCluster(searchShape, clusterQuery);
  }

  public Optional<PixelCluster> findLargestCluster(Shape searchShape, PixelCluster.ClusterQuery clusterQuery) {
    PixelCluster.ClusterSearchResult searchResult = getPixelAnalyzer().findClusters(searchShape, clusterQuery);
    if (searchResult == null) return Optional.empty();

    List<PixelCluster> clusters = searchResult.getClusters();
    if (clusters == null || clusters.isEmpty()) return Optional.empty();

    return Optional.of(clusters.stream()
      .filter(Objects::nonNull)
      .sorted(Comparator.comparingInt((PixelCluster cluster) -> cluster.getPoints().size()).reversed())
      .toList()
      .get(0));
  }

  @Override
  public void onPaint(Canvas canvas) {
    super.onPaint(canvas);
    if (canvas == null) return;

    // Notify and clean up dead references in one pass
    Iterator<WeakReference<Consumer<Canvas>>> iterator = paintListeners.iterator();
    while (iterator.hasNext()) {
      Consumer<Canvas> listener = iterator.next().get();
      if (listener == null) {
        iterator.remove(); // Self-cleaning: referent was GC'ed
      } else {
        listener.accept(canvas);
      }
    }

    drawOverlay(canvas);
  }

  @Override
  public void onStart() {
    super.onStart();

    List<Options> _optionsList = getOptions();
    // Mutable
    List<Options> optionsList = _optionsList == null ? new ArrayList<>() : new ArrayList<>(_optionsList);

    Optional<Double> remoteVersion = getRemoteVersion();
    // Account for double imprecision and assume that intentional double values only have a maximum of two decimal
    // places.
    if (remoteVersion.isPresent() && getVersion() - remoteVersion.get() < -0.001) {
      optionsList.add(0, new UpdateAvailableOptions(this));
    }

    for (Options options : optionsList) {
      if (stopped()) return;
      options.show();
    }
  }

  public String getAuthorLogo() {
    return null;
  }

  public String getLogo() {
    return null;
  }

  public ItemGroupResult pollFramesUntilInventory(Set<Integer> itemsToRecognize) {
    AtomicReference<ItemGroupResult> atomicSnapshot = new AtomicReference<>();

    pollFramesUntil(
      () -> {
        WidgetManager widgetManager = getWidgetManager();
        if (widgetManager == null) return false;
        Inventory inventory = widgetManager.getInventory();
        if (inventory == null) return false;
        if (!inventory.isOpen()) {
          inventory.open();
          pollFramesUntil(inventory::isOpen, 1_800);
        }
        atomicSnapshot.set(inventory.search(itemsToRecognize));
        return atomicSnapshot.get() != null;
      }, Integer.MAX_VALUE);

    assert atomicSnapshot.get() != null;
    return atomicSnapshot.get();
  }

  public void pollFramesUntilMoving() {
    pollFramesUntilMoving(null);
  }

  public void pollFramesUntilMoving(BooleanSupplier breakCondition) {
    AtomicReference<WorldPosition> lastPosition = new AtomicReference<>(getWorldPosition());
    pollFramesUntil(
      () -> {
        if (breakCondition != null && breakCondition.getAsBoolean()) return true;
        WorldPosition position = getWorldPosition();
        if (position == null) return false;

        if (lastPosition.get() == null || position.distanceTo(lastPosition.get()) == 0) {
          lastPosition.set(position);
          return false;
        }

        return true;
      }, Integer.MAX_VALUE);
  }

  public void pollFramesUntilStill() {
    pollFramesUntilStill(null);
  }

  public void pollFramesUntilStill(BooleanSupplier breakCondition) {
    AtomicReference<WorldPosition> lastPosition = new AtomicReference<>(getWorldPosition());
    AtomicReference<Long> lastPositionUpdated = new AtomicReference<>(System.currentTimeMillis());

    pollFramesUntil(
      () -> {
        if (breakCondition != null && breakCondition.getAsBoolean()) return true;

        // Debounce interval is 2 ticks
        if (System.currentTimeMillis() - lastPositionUpdated.get() < 1_200) return false;

        WorldPosition position = getWorldPosition();
        if (position == null) return false;

        if (lastPosition.get() == null || position.distanceTo(lastPosition.get()) > 0.05) {
          lastPosition.set(position);
          lastPositionUpdated.set(System.currentTimeMillis());
          return false;
        }

        return true;
      }, Integer.MAX_VALUE);
  }


  public void removePaintListener(Consumer<Canvas> listener) {
    paintListeners.remove(listener);
  }

  public void removePauseListener(Consumer<Boolean> listener) {
    pauseListeners.remove(listener);
  }

  @Override
  public int[] regionsToPrioritise() {
    return getRequiredRegions().stream().distinct().mapToInt(Integer::intValue).toArray();
  }

  @Override
  public void setPause(boolean paused) {
    super.setPause(paused);
    onPauseChanged(paused);

    // Notify and clean up dead references in one pass
    Iterator<WeakReference<Consumer<Boolean>>> iterator = pauseListeners.iterator();
    while (iterator.hasNext()) {
      Consumer<Boolean> listener = iterator.next().get();
      if (listener == null) {
        iterator.remove(); // Self-cleaning: referent was GC'ed
      } else {
        listener.accept(paused);
      }
    }
  }

  public void queuePolygonDrawable(String key, Polygon polygon, Color color) {
    queuePolygonsDrawable(key, List.of(polygon), color);
  }

  public void queuePolygonsDrawable(String _key, List<Polygon> polygons, Color color) {
    if (polygons == null || polygons.isEmpty()) return;
    String key = "polygon=" + _key;
    Screen screen = getScreen();
    if (screen == null) return;

    screen.queueCanvasDrawable(
      key, canvas -> {
        for (Polygon polygon : polygons) {
          Paint.drawPolygon(canvas, polygon, color);
        }
      });
  }

  protected List<Options> getOptions() {
    return null;
  }

  /**
   * The script will exit if the player is not within one of the provided regions at any point.
   */
  protected List<Integer> getRequiredRegions() {
    return Collections.emptyList();
  }

  protected String getSourceUrl() {
    return null;
  }

  protected void onPauseChanged(boolean paused) {
  }

  private void drawOverlay(Canvas canvas) {
    Screen screen = getScreen();
    if (screen == null) return;
    Rectangle screenBounds = screen.getBounds();
    if (screenBounds == null) return;
    WidgetManager widgetManager = getWidgetManager();
    Bank bank = widgetManager == null ? null : widgetManager.getBank();
    GameState gameState = widgetManager == null ? null : widgetManager.getGameState();

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
    int subduedHeight = 14;
    Font subduedFont = Paint.RUNESCAPE_FONT.deriveFont((float) subduedHeight);
    int totalWidth = x;
    int totalHeight = 160;

    Paint.Origin origin = Paint.getOrigin(84, screenBounds.getHeight() - totalHeight - 12);

    List<Runnable> draws = new ArrayList<>();

    int headerHeight = 16;
    // Logo
    Optional<Image> baseLogo = Paint.loadImage(getLogo());
    if (baseLogo.isPresent()) {
      Image logo = Paint.resizeImage(baseLogo.get(), headerHeight);
      Point logoPoint = origin.translate(x, y);
      draws.add(() -> Paint.drawImage(canvas, logo, logoPoint.x, logoPoint.y));
      x += logo.getWidth() + gap;
      totalWidth = Math.max(totalWidth, x);
    }
    // Name
    Point namePoint = origin.translate(x, y);
    Font headerFont = Paint.RUNESCAPE_BOLD_FONT.deriveFont((float) headerHeight);
    draws.add(() -> canvas.drawText(
      scriptDefinition.name(),
      namePoint.x + 1,
      namePoint.y + headerHeight + 1,
      black,
      headerFont));
    draws.add(() -> canvas.drawText(
      scriptDefinition.name(),
      namePoint.x,
      namePoint.y + headerHeight,
      color,
      headerFont));
    textWidth = canvas.getFontMetrics(headerFont).stringWidth(scriptDefinition.name());
    x += textWidth + smallGap;
    totalWidth = Math.max(totalWidth, x);
    // Version
    y += headerHeight - subduedHeight;
    Point versionPoint = origin.translate(x, y);
    String versionString = "v" + scriptDefinition.version();
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
    totalWidth = Math.max(totalWidth, x);

    int contentY = (y += gap * 2);
    Paint.Origin contentOrigin = origin.translateSelf(gap, y); // Includes gaps

    Paint.Origin bottomOrigin = origin.translateSelf(0, totalHeight);
    // "with OSMB Toolkit"
    x = gap;
    y = 0;
    y -= subduedHeight + gap;
    Point withOsmbToolkitPoint = bottomOrigin.translate(x, y);
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
    totalWidth = Math.max(totalWidth, x);
    // Author logo
    if (getAuthorLogo() != null || !scriptDefinition.author().isEmpty()) {
      x = gap;
      y -= textHeight + smallGap;
      if (getAuthorLogo() != null) {
        Optional<Image> baseAuthorLogo = Paint.loadImage(getAuthorLogo());
        if (baseAuthorLogo.isPresent()) {
          Image authorLogo = Paint.applyCircleMask(Paint.resizeImage(baseAuthorLogo.get(), textHeight));
          Point authorLogoPoint = bottomOrigin.translate(x, y);
          draws.add(() -> Paint.drawImage(canvas, authorLogo, authorLogoPoint.x, authorLogoPoint.y));
          x += authorLogo.getWidth() + gap;
          totalWidth = Math.max(totalWidth, x);
        }
      }
      // Author
      if (!scriptDefinition.author().isEmpty()) {
        Point authorPoint = bottomOrigin.translate(x, y);
        draws.add(() -> canvas.drawText(
          scriptDefinition.author(),
          authorPoint.x + 1,
          authorPoint.y + textHeight + 1,
          black,
          font));
        draws.add(() -> canvas.drawText(
          scriptDefinition.author(),
          authorPoint.x,
          authorPoint.y + textHeight,
          color,
          font));
        textWidth = canvas.getFontMetrics(font).stringWidth(scriptDefinition.author());
        x += textWidth + gap;
        totalWidth = Math.max(totalWidth, x);
      }
    }
    // "Made by"
    x = gap;
    y -= subduedHeight + smallGap;
    Point madeByPoint = bottomOrigin.translate(x, y);
    String madeBy = "Made by";
    draws.add(() -> canvas.drawText(madeBy, madeByPoint.x + 1, madeByPoint.y + subduedHeight + 1, black, subduedFont));
    draws.add(() -> canvas.drawText(madeBy, madeByPoint.x, madeByPoint.y + subduedHeight, subduedColor, subduedFont));
    textWidth = canvas.getFontMetrics(subduedFont).stringWidth(madeBy);
    x += textWidth + gap;
    totalWidth = Math.max(totalWidth, x);

    canvas.fillRect(
      new Rectangle(origin.originX, origin.originY, totalWidth + smallGap, totalHeight),
      Color.BLACK.getRGB(),
      gameState != GameState.LOGGED_IN ? 0.8 : (bank != null && bank.isVisible()) ? 0.5 : 0.1);

    for (Runnable draw : draws) {
      draw.run();
    }
  }

  private String getModuleName() {
    String name = scriptDefinition.name();
    assert name != null;
    name = name.replace(" ", "");
    name = name.endsWith("Script") ? name : name + "Script";
    return name;
  }

  private Optional<Double> getRemoteVersion() {
    String jarUrl = getSourceUrl();
    if (jarUrl == null) return Optional.empty();
    String fileContent;

    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(3000)).build();
      HttpRequest request =
        HttpRequest.newBuilder().uri(URI.create(jarUrl)).GET().timeout(Duration.ofMillis(3000)).build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log(getClass(), "Failed to retrieve remote version. Response code: " + response.statusCode());
        return Optional.empty();
      }

      fileContent = response.body();
    } catch (Exception exception) {
      log(getClass(), "Failed to retrieve remote version. Exception: " + exception.getMessage());
      return Optional.empty();
    }

    Pattern versionPattern = Pattern.compile("version\\s*=\\s*(\\d+(\\.\\d+)?)");
    Matcher matcher = versionPattern.matcher(fileContent);

    if (matcher.find()) {
      String versionString = matcher.group(1);
      try {
        // Convert the captured string (e.g., "1.6") to a double
        return Optional.of(Double.parseDouble(versionString));
      } catch (NumberFormatException e) {
        log(getClass(), "Failed to parse double from remote version. String: " + versionString);
        return Optional.empty();
      }
    }

    log(getClass(), "Failed to parse remote version.");
    return Optional.empty();
  }

  private double getVersion() {
    return scriptDefinition.version();
  }
}
