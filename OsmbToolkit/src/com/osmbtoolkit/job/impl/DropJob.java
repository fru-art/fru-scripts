package com.osmbtoolkit.job.impl;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.ui.WidgetManager;
import com.osmb.api.ui.tabs.Inventory;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.script.ToolkitScript;

import java.util.Set;

public class DropJob extends Job<ToolkitScript> {
  private final Set<Integer> items;

  public DropJob(ToolkitScript script, Integer item) {
    this(script, Set.of(item));
  }
  public DropJob(ToolkitScript script, Set<Integer> items) {
    super(script);
    this.items = items;
  }

  @Override
  public boolean canExecute() {
    ItemGroupResult snapshot = script.pollFramesUntilInventoryVisible(items);
    return snapshot.containsAny(items) && snapshot.isFull();
  }

  @Override
  public boolean execute() {
    WidgetManager widgetManager = script.getWidgetManager();
    if (widgetManager == null) return false;
    Inventory inventory = widgetManager.getInventory();
    if (inventory == null) return false;

    return inventory.dropItems(items);
  }

  @Override
  public String toString() {
    return "Drop items";
  }
}
