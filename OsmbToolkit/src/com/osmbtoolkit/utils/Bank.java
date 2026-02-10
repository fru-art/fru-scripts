package com.osmbtoolkit.utils;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.ObjectManager;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.WidgetManager;
import com.osmbtoolkit.script.ToolkitScript;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

public class Bank {
  private static final Map<String, String> BANK_TO_ACTION_MAP = Map.of(
    "Bank Chest", "Use",
    "Bank booth", "Bank",
    "Bank chest", "Use",
    "Grand Exchange booth", "Bank"
  );
  private static final RectangleArea GE = new RectangleArea(3162, 3489, 5, 1, 0);

  public final RSObject object;
  private final ToolkitScript script;

  public Bank(ToolkitScript script, RSObject object) {
    this.object = object;
    this.script = script;
  }

  public boolean deposit(Set<Integer> items) {
    if (items.isEmpty()) return true;

    Optional<com.osmb.api.ui.bank.Bank> ui = walkToAndOpen();
    if (ui.isEmpty()) {
      script.log(getClass(), "Failed to open bank for depositing");
      return false;
    }

    ItemGroupResult snapshot = script.pollFramesUntilInventoryVisible(items);
    for (Integer item : items) {
      int count = snapshot.getAmount(item);
      if (count <= 0) continue;
      if (!ui.get().deposit(item, count)) {
        script.log(getClass(), "Failed to deposit item " + item);
      }
    }

    return true;
  }

  public boolean depositAll() {
    return depositAll(Collections.emptySet());
  }
  public boolean depositAll(Set<Integer> itemsToIgnore) {
    Optional<com.osmb.api.ui.bank.Bank> ui = walkToAndOpen();
    return ui.map(bank -> bank.depositAll(itemsToIgnore)).orElse(false);
  }

  // TODO
//  public boolean withdraw() {
//    Optional<com.osmb.api.ui.bank.Bank> ui = walkToAndOpen();
//    if (ui.isEmpty()) return false;
//
//    return false;
//  }

  public Optional<com.osmb.api.ui.bank.Bank> walkToAndOpen() {
    WidgetManager widgetManager = script.getWidgetManager();
    if (widgetManager == null) return Optional.empty();

    // If UI is already open, return it immediately.
    com.osmb.api.ui.bank.Bank ui = script.getWidgetManager().getBank();
    if (ui != null && ui.isVisible()) return Optional.of(ui);

    String action = BANK_TO_ACTION_MAP.get(object.getName());
    object.interact(action);
    script.log(getClass(), "Interacted " + object.getName());

    AtomicReference<com.osmb.api.ui.bank.Bank> atomicUi = new AtomicReference<>();
    BooleanSupplier setUi = () -> {
      if (atomicUi.get() != null) return true;
      WidgetManager scopedWidgetManager = script.getWidgetManager();
      if (scopedWidgetManager == null) return false;
      com.osmb.api.ui.bank.Bank scopedUi = scopedWidgetManager.getBank();
      if (scopedUi == null || !scopedUi.isVisible()) return false;
      script.log(getClass(), "Found visible bank UI, breaking");
      atomicUi.set(scopedUi);
      return true;
    };

    script.pollFramesUntiLPositionReached(object.getWorldPosition(), setUi);
    return (atomicUi.get() == null || !atomicUi.get().isVisible()) ? Optional.empty() : Optional.of(atomicUi.get());
  }

  public static Optional<Bank> getClosestBank(ToolkitScript script) {
    ObjectManager objectManager = script.getObjectManager();
    if (objectManager == null) return Optional.empty();
    WorldPosition position = script.getWorldPosition();
    if (position == null) return Optional.empty();

    List<RSObject> banks = objectManager.getObjects(object -> {
      if (object == null) return false;
      String name = object.getName();
      if (name == null) return false;
      return BANK_TO_ACTION_MAP.containsKey(name) && object.isInteractable();
    }).stream().sorted(Comparator.comparingDouble(bank -> bank.distance(position))).toList();

    return banks.isEmpty() ? Optional.empty() : Optional.of(new Bank(script, banks.get(0)));
  }
}
