package com.osmbtoolkit.utils;

import com.osmb.api.input.MenuEntry;
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
  private static final Set<String> BANK_ACTIONS = Set.of(
    "bank",
    "use",
    "Bank",
    "Use");
  private static final Set<String> BANK_NAMES = Set.of(
    "Bank booth",
    "Bank chest",
    "Bank Chest",
    "Grand Exchange booth");
  private static final RectangleArea GE = new RectangleArea(3162, 3489, 5, 1, 0);

  public final RSObject object;
  private final ToolkitScript script;

  public Bank(ToolkitScript script, RSObject object) {
    this.object = object;
    this.script = script;
  }

  public boolean deposit(Set<Integer> items) {
    if (items == null || items.isEmpty()) return true;

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

    object.interact(entries -> {
      for (MenuEntry entry : entries) {
        if (BANK_ACTIONS.contains(entry.getAction())) return entry;
      }
      return null;
    });
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

    script.log(getClass(), "Waiting to reach " + object.getName());
    script.pollFramesUntilPositionReached(object.getWorldPosition(), setUi);

    boolean openedBank = atomicUi.get() != null && atomicUi.get().isVisible();
    if (openedBank) script.pollFramesHuman(() -> false, 1);
    return openedBank ? Optional.of(atomicUi.get()) : Optional.empty();
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
      return BANK_NAMES.contains(name) && object.isInteractable();
    }).stream()
      .sorted(Comparator.comparingDouble(bank -> bank.distance(position)))
      .toList();

    if (banks.isEmpty()) return Optional.empty();
    if (banks.size() == 1) return Optional.of(new Bank(script, banks.get(0)));

    // If there are nearby banks, get the closest bank by tile distance
    RSObject closestBankByDirectDistance = banks.get(0);
    List<RSObject> nearbyBanks = banks.stream()
      // Heuristic to only gauge banks near the closest bank by direct distance
      .filter(bank -> closestBankByDirectDistance.distance(position) - bank.distance(position) < 5)
      .toList();

    // Skip tile distance computation if no nearby banks
    if (nearbyBanks.size() == 1) return Optional.of(new Bank(script, banks.get(0)));

    Optional<RSObject> closestBankByTileDistance = nearbyBanks.stream()
      .min(Comparator.comparingInt(bank -> bank.getTileDistance(position)));
    assert closestBankByTileDistance.isPresent();

    return Optional.of(new Bank(script, closestBankByTileDistance.get()));
  }
}
