package com.osmbtoolkit.job.impl;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmbtoolkit.job.Job;
import com.osmbtoolkit.script.ToolkitScript;
import com.osmbtoolkit.utils.Bank;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class DepositAtBankJob extends Job<ToolkitScript> {
  private final boolean ignoreMode;
  private final Set<Integer> items;

  public DepositAtBankJob(ToolkitScript script) {
    this(script, Collections.emptySet());
  }

  public DepositAtBankJob(ToolkitScript script, Set<Integer> items) {
    this(script, items, false);
  }

  public DepositAtBankJob(ToolkitScript script, Set<Integer> items, boolean ignoreMode) {
    super(script);
    this.ignoreMode = ignoreMode;
    this.items = items;
  }

  @Override
  public boolean canExecute() {
    Optional<Bank> bank = Bank.getClosestBank(script);
    if (bank.isEmpty()) {
      script.log(this.getClass(), "No closest bank found");
      return false;
    }
    ItemGroupResult inventory = script.pollFramesUntilInventoryVisible(items);
    boolean hasItems =
      (items.isEmpty() && !inventory.isEmpty()) || (!items.isEmpty() && inventory.containsAny(items));
    if (!hasItems) {
      script.log(this.getClass(), "No items found");
      return false;
    }
    WorldPosition position = script.getWorldPosition();
    boolean isNearBank = position != null && bank.get().object.distance(position) < 5;
    return isNearBank || inventory.isFull();
  }

  @Override
  public boolean execute() {
    Optional<Bank> bank = Bank.getClosestBank(script);
    if (bank.isEmpty()) return false;

    if (items == null || items.isEmpty()) {
      return bank.get().depositAll();
    } else if (ignoreMode) {
      return bank.get().depositAll(items);
    } else {
      return bank.get().deposit(items);
    }
  }
}
