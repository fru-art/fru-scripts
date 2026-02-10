package com.osmbtoolkit.script;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.SceneManager;
import com.osmb.api.screen.Screen;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Shape;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.ui.tabs.Inventory;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.drawing.SceneProjector;
import com.osmb.api.visual.image.Image;
import com.osmbtoolkit.job.JobLoopScript;
import com.osmbtoolkit.options.Options;
import com.osmbtoolkit.options.impl.UpdateAvailableOptions;
import com.osmbtoolkit.utils.Paint;
import com.osmbtoolkit.utils.PausableTimer;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ToolkitScript extends JobLoopScript {
  public final PausableTimer pausableTimer;
  public final ScriptDefinition scriptDefinition;

  private final List<WeakReference<Runnable>> frameListeners = new ArrayList<>();
  private final ToolkitScriptOverlay overlay;
  private final List<WeakReference<Consumer<Canvas>>> paintListeners = new ArrayList<>();
  private final List<WeakReference<Consumer<Boolean>>> pauseListeners = new ArrayList<>();

  private Optional<Image> authorLogoImage;
  private boolean isBankVisible;
  private boolean isLoggedIn;
  private Optional<Image> logoImage;

  public ToolkitScript(Object scriptCore) {
    super(scriptCore);

    this.pausableTimer = new PausableTimer(this);
    this.pausableTimer.attachTo(this);
    this.scriptDefinition = this.getClass().getAnnotation(ScriptDefinition.class);
    assert this.scriptDefinition != null;

    this.overlay = new ToolkitScriptOverlay(this);

    try {
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      ge.registerFont(Paint.RUNESCAPE_FONT);
      ge.registerFont(Paint.RUNESCAPE_BOLD_FONT);
      ge.registerFont(Paint.RUNESCAPE_SMALL_FONT);
    } catch (Exception ignored) {
    }
  }

  public void addFrameListener(Runnable listener) {
    // Avoid duplicates: check if any existing weak ref points to this listener
    boolean exists = frameListeners.stream().anyMatch(ref -> ref.get() == listener);
    if (!exists) {
      frameListeners.add(new WeakReference<>(listener));
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
  public void onNewFrame() {
    super.onNewFrame();

    // Notify and clean up dead references in one pass
    Iterator<WeakReference<Runnable>> iterator = frameListeners.iterator();
    while (iterator.hasNext()) {
      Runnable listener = iterator.next().get();
      if (listener == null) {
        iterator.remove(); // Self-cleaning: referent was GC'ed
      } else {
        listener.run();
      }
    }

    // Set game state
    WidgetManager widgetManager = getWidgetManager();
    Bank bank = widgetManager == null ? null : widgetManager.getBank();
    GameState gameState = widgetManager == null ? null : widgetManager.getGameState();

    this.isBankVisible = gameState == GameState.LOGGED_IN && bank != null && bank.isVisible();
    this.isLoggedIn = gameState == GameState.LOGGED_IN;
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

    overlay.draw(canvas);
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

  public Optional<Image> getAuthorLogoImage() {
    // Return cached image even if optional is empty, as long as it's not null
    if (authorLogoImage != null) return authorLogoImage;

    String authorLogo =  getAuthorLogo();
    if (authorLogo == null) return Optional.empty();

    authorLogoImage = Paint.loadImage(authorLogo);
    return authorLogoImage;
  }

  public Optional<Polygon> getPlayerCube(WorldPosition worldPosition) {
    Optional<LocalPosition> localPosition = getPreciseLocalPosition(worldPosition);
    if (localPosition.isEmpty()) return Optional.empty();
    SceneProjector sceneProjector = getSceneProjector();
    if (sceneProjector == null) return Optional.empty();

    Polygon npcCube = sceneProjector.getTileCube(
      localPosition.get().getPreciseX(),
      localPosition.get().getPreciseY(),
      worldPosition.getPlane(),
      -20,
      240,
      true);
    if (npcCube == null) return Optional.empty();
    return Optional.of(npcCube.getResized(0.8));
  }

  public Optional<LocalPosition> getPreciseLocalPosition(WorldPosition worldPosition) {
    SceneManager sceneManager = getSceneManager();
    if (sceneManager == null) return Optional.empty();
    double x = worldPosition.getPreciseX();
    double y = worldPosition.getPreciseY();
    double localX = x - sceneManager.getSceneBaseTileX();
    double localY = y - sceneManager.getSceneBaseTileY();
    // Not within current or neighboring region
    if (localX >= 128 || localX <= -64 || localY >= 128 || localY <= -64) return Optional.empty();
    return Optional.of(new LocalPosition(localX, localY, worldPosition.getPlane()));
  }

  public String getLogo() {
    return null;
  }

  public Optional<Image> getLogoImage() {
    // Return cached image even if optional is empty, as long as it's not null
    if (logoImage != null) return logoImage;

    String logo = getLogo();
    if (logo == null) return Optional.empty();

    logoImage = Paint.loadImage(logo);
    return logoImage;
  }

  /**
   * In addition to ease-of-access, this method also decouples UI visibility checks from hidden usages of 'onPaint',
   * which could cause stack overflow if checked within 'onPaint'.
   */
  public boolean isGameScreenVisible() {
    return isLoggedIn && !isBankVisible;
  }

  public <T> boolean pollFramesUntilChange(Supplier<T> supplier, int timeout) {
    AtomicReference<T> value = new AtomicReference<>(supplier.get());
    return pollFramesUntil(() -> {
      Object nextValue = supplier.get();
      return !nextValue.equals(value.get());
    }, timeout);
  }

  public boolean pollFramesUntiLPositionReached(WorldPosition destination, BooleanSupplier breakCondition) {
    BooleanSupplier reachedCondition = () -> {
      if (breakCondition.getAsBoolean()) return true;
      WorldPosition position = getWorldPosition();
      return position != null && position.distanceTo(destination) <= 1.1;
    };

    if (!pollFramesUntilMovement(1_800, reachedCondition)) return false;
    pollFramesUntilNoMovement(reachedCondition);
    return true;
  }

  public ItemGroupResult pollFramesUntilInventoryVisible() {
    return pollFramesUntilInventoryVisible(Collections.emptySet());
  }
  public ItemGroupResult pollFramesUntilInventoryVisible(Set<Integer> itemsToRecognize) {
    Supplier<ItemGroupResult> getSnapshot = () -> {
      WidgetManager widgetManager = getWidgetManager();
      if (widgetManager == null) return null;
      Inventory inventory = widgetManager.getInventory();
      if (inventory == null) return null;
      if (!inventory.isOpen()) {
        inventory.open();
        pollFramesUntil(inventory::isOpen, 1_800);
      }
      return inventory.search(itemsToRecognize);
    };

    AtomicReference<ItemGroupResult> atomicSnapshot = new AtomicReference<>(getSnapshot.get());
    // Avoid calling poll if unnecessary since it will clear draw, etc
    if (atomicSnapshot.get() != null) return atomicSnapshot.get();

    pollFramesUntil(
      () -> {
        atomicSnapshot.set(getSnapshot.get());
        return atomicSnapshot.get() != null;
      }, Integer.MAX_VALUE);

    assert atomicSnapshot.get() != null;
    return atomicSnapshot.get();
  }

  public boolean pollFramesUntilMovement() {
    return pollFramesUntilMovement(Integer.MAX_VALUE, null);
  }
  public boolean pollFramesUntilMovement(int timeout) {
    return pollFramesUntilMovement(timeout, null);
  }
  public boolean pollFramesUntilMovement(int timeout, BooleanSupplier breakCondition) {
    AtomicReference<WorldPosition> lastPosition = new AtomicReference<>(getWorldPosition());
    return pollFramesUntil(
      () -> {
        if (breakCondition != null && breakCondition.getAsBoolean()) return true;
        WorldPosition position = getWorldPosition();
        if (position == null) return false;

        if (lastPosition.get() == null || position.distanceTo(lastPosition.get()) == 0) {
          lastPosition.set(position);
          return false;
        }

        return true;
      }, timeout);
  }

  public <T> boolean pollFramesUntilNoChange(Supplier<T> supplier, int changeTimeout) {
    return pollFramesUntilNoChange(supplier, changeTimeout, Integer.MAX_VALUE, () -> false);
  }
  public <T> boolean pollFramesUntilNoChange(Supplier<T> supplier, int changeTimeout, int noChangeTimeout, BooleanSupplier breakCondition) {
    long startTime = System.currentTimeMillis();

    while (true) {
      AtomicReference<T> value = new AtomicReference<>(supplier.get());
      AtomicBoolean broke = new AtomicBoolean(breakCondition.getAsBoolean());
      if (broke.get()) return true;
      AtomicBoolean changed =  new AtomicBoolean(false);
      AtomicBoolean timedOut =  new AtomicBoolean(false);

      pollFramesUntil(() -> {
        if (breakCondition.getAsBoolean()) {
          broke.set(true);
          return true;
        }

        T nextValue = supplier.get();
        if (nextValue != null && !nextValue.equals(value.get())) {
          value.set(nextValue);
          changed.set(true);
          return true;
        }
        if (System.currentTimeMillis() - startTime > noChangeTimeout) {
          timedOut.set(true);
          return true;
        }

        return changed.get() || timedOut.get();
      }, changeTimeout);

      if (broke.get() || !changed.get()) return true;
      if (timedOut.get()) return false;
    }
  }

  public void pollFramesUntilNoMovement() {
    pollFramesUntilNoMovement(() -> false);
  }

  public void pollFramesUntilNoMovement(BooleanSupplier breakCondition) {
    pollFramesUntilNoChange(() -> {
      // Don't return world position directly; I'm not sure how object equality works for world positions
      WorldPosition position = getWorldPosition();
      if (position == null) return "";
      return String.format("%.1f|%.1f", position.getPreciseX(), position.getPreciseY());
    }, 1_200, 0, breakCondition);
  }

  public void removeFrameListener(Runnable listener) {
    frameListeners.remove(listener);
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

  protected String getVersionSourceUrl() {
    return null;
  }

  protected void onPauseChanged(boolean paused) {
  }

  private String getModuleName() {
    String name = scriptDefinition.name();
    assert name != null;
    name = name.replace(" ", "");
    name = name.endsWith("Script") ? name : name + "Script";
    return name;
  }

  private Optional<Double> getRemoteVersion() {
    String jarUrl = getVersionSourceUrl();
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
