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
package com.google.gwt.bikeshed.sample.stocks.server;

import com.google.gwt.bikeshed.sample.stocks.shared.PlayerInfo;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Game state for a single player.
 */
public class PlayerStatus extends PlayerInfo {

  /**
   * An impossible stock ticker.
   */
  private static final String IMPOSSIBLE_TICKER_SYMBOL = "XXXXXXXXXX";

  /**
   * This players favorite stocks.
   */
  private TreeSet<String> favorites = new TreeSet<String>();

  /**
   * A precompiled version of the favorites query.
   */
  private Pattern favoritesPattern;

  /**
   * The query used to retrieve favorites.
   */
  private String favoritesQuery;

  /**
   * The number of shares owned for each symbol.
   */
  private HashMap<String, Integer> sharesOwnedBySymbol = new HashMap<String, Integer>();

  /**
   * The total amount paid for each symbol.
   */
  private HashMap<String, Double> averagePriceBySymbol = new HashMap<String, Double>();

  public PlayerStatus(String name) {
    super(name);
    generateFavoritesQuery();
  }

  /**
   * Add a stock to the favorites list.
   * 
   * @param ticker the stock ticker
   */
  public void addFavorite(String ticker) {
    favorites.add(ticker);
    generateFavoritesQuery();
  }

  /**
   * Purchase stock.
   * 
   * @param ticker the stock ticker
   * @param quantity the number of shares to buy
   * @param price the price of the stock
   * @throws IllegalArgumentException if the stock cannot be purchased
   */
  public void buy(String ticker, int quantity, int price)
      throws IllegalArgumentException {
    // Verify that the player can afford the stock.
    int cash = getCash();
    int totalPrice = price * quantity;
    if (cash < totalPrice) {
      throw new IllegalArgumentException("You cannot afford that much stock");
    }

    // Update the number of shares owned.
    int current = getSharesOwned(ticker);
    double averagePrice = getAveragePrice(ticker);
    double totalPaid = averagePrice * current + totalPrice;

    // Update case and shares owned
    current += quantity;
    setCash(cash - totalPrice);
    sharesOwnedBySymbol.put(ticker, current);

    // Update average price
    averagePrice = totalPaid / current;
    averagePriceBySymbol.put(ticker, averagePrice);

    // Add this stock to the favorites list.
    addFavorite(ticker);
  }

  /**
   * Returns the total cost of the currently owned shared, using an average cost
   * basis method.
   * 
   * @param ticker the stock ticker
   */
  public int getAverageCostBasis(String ticker) {
    return (int) (Math.round(getAveragePrice(ticker) * getSharesOwned(ticker)));
  }

  public double getAveragePrice(String ticker) {
    Double current = averagePriceBySymbol.get(ticker);
    return current == null ? 0.0 : current;
  }

  /**
   * Get this players favorite pattern.
   * 
   * @return the pattern
   */
  public Pattern getFavoritesPattern() {
    return favoritesPattern;
  }

  /**
   * Get this players favorite query.
   * 
   * @return the query
   */
  public String getFavoritesQuery() {
    return favoritesQuery;
  }

  /**
   * Get the number of shares owned for a given stock.
   * 
   * @param ticker the stock ticker
   * @return the number of shares owned
   */
  public int getSharesOwned(String ticker) {
    Integer current = sharesOwnedBySymbol.get(ticker);
    return current == null ? 0 : current;
  }

  /**
   * Check if the stock ticker is in the favorites list.
   * 
   * @param ticker the stock sticker
   * @return true if a favorite, false if not
   */
  public boolean isFavorite(String ticker) {
    return favorites.contains(ticker);
  }

  /**
   * Remove a stock from the favorites list.
   * 
   * @param ticker the stock ticker
   */
  public void removeFavorite(String ticker) {
    favorites.remove(ticker);
    generateFavoritesQuery();
  }

  /**
   * Sell stock.
   * 
   * @param ticker the stock ticker
   * @param quantity the number of shares to sell
   * @param price the price of the stock
   * @throws IllegalArgumentException if the stock cannot be sold
   */
  public void sell(String ticker, int quantity, int price)
      throws IllegalArgumentException {
    // Verify that the player has enough stock to sell.
    int current = sharesOwnedBySymbol.get(ticker);
    if (quantity > current) {
      throw new IllegalArgumentException(
          "You cannot sell more stock than you own");
    }

    // Perform the transaction.
    int totalPrice = price * quantity;
    setCash(getCash() + totalPrice);

    current -= quantity;
    sharesOwnedBySymbol.put(ticker, current);

    if (current == 0) {
      sharesOwnedBySymbol.remove(ticker);
      averagePriceBySymbol.remove(ticker);
      removeFavorite(ticker);
    }
  }

  @Override
  public void setStockValue(int value) {
    super.setStockValue(value);
  }

  /**
   * Regenerate the favorites query.
   */
  private void generateFavoritesQuery() {
    StringBuilder sb = new StringBuilder(IMPOSSIBLE_TICKER_SYMBOL);
    for (String ticker : favorites) {
      sb.append('|');
      sb.append(ticker);
    }
    favoritesQuery = sb.toString();
    favoritesPattern = Pattern.compile(favoritesQuery);
  }
}
