/*
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.bikeshed.sample.stocks.shared;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Information about a single player.
 */
public class PlayerInfo implements Serializable {
  
  private static final int MAX_STATUS = 5;

  /**
   * The initial amount of cash that the player starts with, in cents.
   */
  private static final int INITIAL_CASH = 10000 * 100;

  /**
   * The net worth of the player in cents.
   */
  private int cash = INITIAL_CASH;

  /**
   * The players name.
   */
  private String name;

  private LinkedList<String> status;
  
  /**
   * The net worth of the player in cents.
   */
  private int stockValue;

  public PlayerInfo(String name) {
    this.name = name;
  }

  /**
   * Visible for RPC.
   */
  PlayerInfo() {
  }
  
  public void addStatus(String message) {
    if (status == null) {
      status = new LinkedList<String>();
    }
    status.add(message);
    if (status.size() > MAX_STATUS) {
      status.removeFirst();
    }
  }

  public PlayerInfo copy() {
    PlayerInfo copy = new PlayerInfo(name);
    copy.setCash(cash);
    copy.setStockValue(stockValue);
    copy.setStatus(status);
    return copy;
  }

  /**
   * Get the player's current cash amount.
   * 
   * @return the cash amount
   */
  public int getCash() {
    return cash;
  }

  /**
   * Get the player's display name, which excludes domain information.
   * 
   * @return the display name of the player.
   */
  public String getDisplayName() {
    String displayName = name;
    int domain = displayName.indexOf('@');
    if (domain > 0) {
      displayName = displayName.substring(0, domain);
    }
    return displayName;
  }

  /**
   * Get the player's name.
   * 
   * @return the name of the player.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the players net worth, including cash and stock value.
   * 
   * @return the net worth in cents
   */
  public int getNetWorth() {
    return cash + stockValue;
  }

  public List<String> getStatus() {
    return status;
  }
  
  /**
   * Get the value of this player's stock.
   * 
   * @return the value of the stock in cents
   */
  public int getStockValue() {
    return stockValue;
  }

  /**
   * Set the amount of cash that the user has.
   * 
   * @param cash the user's cash in cents
   */
  protected void setCash(int cash) {
    this.cash = cash;
  }

  /**
   * Set the value of this player's stock.
   * 
   * @param value the stock value in cents
   */
  protected void setStockValue(int value) {
    this.stockValue = value;
  }

  private void setStatus(List<String> status) {
    this.status = status == null ? null : new LinkedList<String>(status);
  }
}
