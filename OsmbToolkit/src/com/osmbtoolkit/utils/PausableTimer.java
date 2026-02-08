package com.osmbtoolkit.utils;

import com.osmbtoolkit.script.ToolkitScript;

import java.util.function.Consumer;

public class PausableTimer {
  public long startTime;
  private long lastUnpauseTimestamp;
  private long totalElapsedBeforeCurrentSession;
  private boolean paused;

  // Strong reference to the listener so it isn't GC'ed while the Timer exists
  private Consumer<Boolean> pauseListener;

  public PausableTimer() {
    reset();
  }

  public PausableTimer(ToolkitScript script) {
    this();
  }

  public void attachTo(ToolkitScript script) {
    // Create the listener and store it in a field (Strong Reference)
    this.pauseListener = isPaused -> {
      if (isPaused) pause(); else unpause();
    };

    script.addPauseListener(this.pauseListener);

    if (script.paused()) {
      pause();
    }
  }

  public void pause() {
    if (!paused) {
      totalElapsedBeforeCurrentSession += System.currentTimeMillis() - lastUnpauseTimestamp;
      paused = true;
    }
  }

  public void unpause() {
    if (paused) {
      lastUnpauseTimestamp = System.currentTimeMillis();
      paused = false;
    }
  }

  public void reset() {
    this.startTime = System.currentTimeMillis();
    this.lastUnpauseTimestamp = this.startTime;
    this.totalElapsedBeforeCurrentSession = 0;
    this.paused = false;
  }

  public long timeElapsed() {
    if (paused) return totalElapsedBeforeCurrentSession;
    return totalElapsedBeforeCurrentSession + (System.currentTimeMillis() - lastUnpauseTimestamp);
  }

  public String getTimeElapsedFormatted() {
    long ms = timeElapsed();
    long seconds = (ms / 1000) % 60;
    long minutes = (ms / (1000 * 60)) % 60;
    long hours = (ms / (1000 * 60 * 60)) % 24;
    long milliseconds = ms % 1000;
    return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
  }

  public boolean isPaused() {
    return paused;
  }
}