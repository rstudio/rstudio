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

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.AbstractListModel.DefaultRange;
import com.google.gwt.bikeshed.sample.stocks.client.StockService;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuoteList;
import com.google.gwt.bikeshed.sample.stocks.shared.StockRequest;
import com.google.gwt.bikeshed.sample.stocks.shared.StockResponse;
import com.google.gwt.bikeshed.sample.stocks.shared.Transaction;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class StockServiceImpl extends RemoteServiceServlet implements
    StockService {

  /**
   * The result of a query to the remote service that provides stock quotes.
   */
  private class Result {
    int numRows;
    StockQuoteList quotes;

    public Result(StockQuoteList quotes, int numRows) {
      this.quotes = quotes;
      this.numRows = numRows;
    }
  }

  static HashMap<String, String> companyNamesBySymbol = new HashMap<String, String>();

  static TreeSet<String> stockTickers = new TreeSet<String>();

  private static final int MAX_RESULTS_TO_RETURN = 10000;

  static {
    int num = NasdaqStocks.SYMBOLS.length;
    for (int i = 0; i < num - 1; i += 2) {
      String symbol = NasdaqStocks.SYMBOLS[i];
      String companyName = NasdaqStocks.SYMBOLS[i + 1];
      stockTickers.add(symbol);

      companyNamesBySymbol.put(symbol, companyName);
    }
  }

  /**
   * A mapping of usernames to {@link PlayerStatus}.
   */
  private Map<String, PlayerStatus> players = new HashMap<String, PlayerStatus>();

  public void addFavorite(String ticker) {
    ensurePlayer().addFavorite(ticker);
  }

  public StockResponse getStockQuotes(StockRequest request)
      throws IllegalArgumentException {

    String query = request.getSearchQuery();
    if (query == null | query.length() == 0) {
      query = ".*";
    }
    Range searchRange = request.getSearchRange();
    Range favoritesRange = request.getFavoritesRange();

    PlayerStatus player = ensurePlayer();
    Result searchResults = query(query, searchRange);
    Result favorites = query(player.getFavoritesQuery(), favoritesRange);

    return new StockResponse(searchResults.quotes, favorites.quotes,
        searchResults.numRows, favorites.numRows, player.getCash());
  }

  public void removeFavorite(String ticker) {
    ensurePlayer().removeFavorite(ticker);
  }

  public Transaction transact(Transaction transaction)
      throws IllegalArgumentException {
    // Get the current stock price.
    String ticker = transaction.getTicker();
    if (ticker == null || ticker.length() < 0) {
      throw new IllegalArgumentException("Stock could not be found");
    }
    Result result = query(ticker, new DefaultRange(0, 1));
    if (result.numRows != 1 || result.quotes.size() != 1) {
      throw new IllegalArgumentException("Could not resolve stock ticker");
    }
    StockQuote quote = result.quotes.get(0);

    // Perform the transaction with the user.
    int quantity = transaction.getQuantity();
    int price = quote.getPrice();
    if (transaction.isBuy()) {
      ensurePlayer().buy(ticker, quantity, price);
    } else {
      ensurePlayer().sell(ticker, quantity, price);
    }

    return new Transaction(true, ticker, quantity, price);
  }

  /**
   * Ensure that a {@link PlayerStatus} for the current player exists and return
   * it.
   * 
   * @return the {@link PlayerStatus} for the current player
   */
  private PlayerStatus ensurePlayer() {
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    String userId = user.getUserId();
    PlayerStatus player = players.get(userId);
    if (player == null) {
      player = new PlayerStatus();
      players.put(userId, player);
    }
    return player;
  }

  private List<String> getTickers(String query) {
    List<String> tickers = new ArrayList<String>();
    if (query.length() > 0) {
      query = query.toUpperCase();
      int count = 0;
      for (String ticker : stockTickers) {
        if (match(ticker, query)) {
          tickers.add(ticker);
          count++;
          if (count > MAX_RESULTS_TO_RETURN) {
            break;
          }
        }
      }
    }
    return tickers;
  }

  private boolean match(String symbol, String query) {
    if (symbol.startsWith(query)) {
      return true;
    }

    try {
      if (symbol.matches(query)) {
        return true;
      }
    } catch (PatternSyntaxException e) {
      // ignore
    }

    return false;
  }

  /**
   * Query the remote service to retrieve current stock prices.
   * 
   * @param query the query string
   * @param range the range of results requested
   * @return the stock quotes
   */
  private Result query(String query, Range range) {
    // Get all symbols for the query.
    PlayerStatus player = ensurePlayer();
    List<String> symbols = getTickers(query);

    if (symbols.size() == 0) {
      return new Result(new StockQuoteList(0), 0);
    }

    int start = range.getStart();
    int end = Math.min(start + range.getLength(), symbols.size());

    // Get the symbols that are in range.
    Set<String> symbolsInRange = new HashSet<String>();
    if (end > start) {
      symbolsInRange.addAll(symbols.subList(start, end));
    }

    // Build the URL string.
    StringBuilder sb = new StringBuilder(
        "http://www.google.com/finance/info?client=ig&q=");
    boolean first = true;
    for (String symbol : symbolsInRange) {
      if (!first) {
        sb.append(',');
      }
      sb.append(symbol);
      first = false;
    }

    if (first) {
      // No symbols
      return new Result(new StockQuoteList(0), 0);
    }

    // Send the request.
    String content = "";
    try {
      String urlString = sb.toString();
      URL url = new URL(urlString);
      InputStream urlInputStream = url.openStream();
      Scanner contentScanner = new Scanner(urlInputStream, "UTF-8");
      if (contentScanner.hasNextLine()) {
        // See
        // http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        content = contentScanner.useDelimiter("\\A").next();
      }

      // System.out.println(content);
    } catch (MalformedURLException mue) {
      System.err.println(mue);
    } catch (IOException ioe) {
      System.err.println(ioe);
    }

    // Parse response.
    Map<String, StockQuote> priceMap = new HashMap<String, StockQuote>();
    Pattern pattern = Pattern.compile("\\{[^\\}]*\\}");
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      String group = matcher.group();

      String symbol = null;
      String price = null;

      Pattern dataPattern = Pattern.compile("\"([^\"]*)\"\\s*:\\s*\"([^\"]*)\"");
      Matcher dataMatcher = dataPattern.matcher(group);
      while (dataMatcher.find()) {
        String tag = dataMatcher.group(1);
        String data = dataMatcher.group(2);
        if (tag.equals("t")) {
          symbol = data;
        } else if (tag.equals("l_cur")) {
          price = data;
        }
      }

      if (symbol != null && price != null) {
        int iprice = 0;
        try {
          iprice = (int) (Double.parseDouble(price) * 100);
          String name = companyNamesBySymbol.get(symbol);
          Integer sharesOwned = player.getSharesOwned(symbol);
          boolean favorite = player.isFavorite(symbol);
          priceMap.put(symbol, new StockQuote(symbol, name, iprice,
              sharesOwned == null ? 0 : sharesOwned.intValue(), favorite));
        } catch (NumberFormatException e) {
          System.out.println("Bad price " + price + " for symbol " + symbol);
        }
      }
    }

    // Convert the price map to a StockQuoteList.
    StockQuoteList toRet = new StockQuoteList(start);
    for (int i = start; i < end; i++) {
      String symbol = symbols.get(i);
      StockQuote quote = priceMap.get(symbol);
      if (quote == null) {
        System.out.println("Bad symbol " + symbol);
      } else {
        toRet.add(quote);
      }
    }

    return new Result(toRet, symbols.size());
  }
}
