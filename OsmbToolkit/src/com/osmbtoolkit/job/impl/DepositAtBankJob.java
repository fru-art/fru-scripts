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
  private final Set<Integer> items;

  public DepositAtBankJob(ToolkitScript script) {
    super(script);
    this.items = Collections.emptySet();
  }

  public DepositAtBankJob(ToolkitScript script, Set<Integer> items) {
    super(script);
    this.items = items;
  }

  @Override
  public boolean canExecute() {
    Optional<Bank> bank = Bank.getClosestBank(script);
    if (bank.isEmpty()) return false;
    ItemGroupResult inventory = script.pollFramesUntilInventory(items);
    boolean hasItems = (items.isEmpty() && !inventory.isEmpty()) || (!items.isEmpty() && inventory.containsAny(items));
    WorldPosition position = script.getWorldPosition();
    boolean isNearBank = position != null && bank.get().object.distance(position) < 5;
    return hasItems && (isNearBank || inventory.isFull());
  }

  @Override
  public boolean execute() {
    Optional<Bank> bank = Bank.getClosestBank(script);
    return bank.map(value -> items.isEmpty() ? value.depositAll() : value.deposit(items)).orElse(false);
  }
}
