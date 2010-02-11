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
package com.google.gwt.sample.datawidgets.server;

import com.google.gwt.list.shared.Range;
import com.google.gwt.sample.datawidgets.client.StockService;
import com.google.gwt.sample.datawidgets.shared.StockQuote;
import com.google.gwt.sample.datawidgets.shared.StockQuoteList;
import com.google.gwt.sample.datawidgets.shared.StockRequest;
import com.google.gwt.sample.datawidgets.shared.StockResponse;
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

  private static final String IMPOSSIBLE_TICKER_SYMBOL = "XXXXXXXXXX";

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

  private TreeSet<String> favorites = new TreeSet<String>();

  private String favoritesQuery = IMPOSSIBLE_TICKER_SYMBOL;

  private HashMap<String,Integer> sharesOwnedBySymbol = new HashMap<String,Integer>();

  public void addFavorite(String ticker) {
    favorites.add(ticker);
    generateFavoritesQuery();
  }
  
  public StockResponse getStockQuotes(StockRequest request)
      throws IllegalArgumentException {
    
    String query = request.getSearchQuery();
    if (query == null | query.length() == 0) {
      query = ".*";
    }
    Range searchRange = request.getSearchRange();
    Range favoritesRange = request.getFavoritesRange();
    
    Result searchResults = query(query, searchRange);
    Result favorites = query(favoritesQuery, favoritesRange);
    
    return new StockResponse(searchResults.quotes, favorites.quotes, searchResults.numRows, favorites.numRows);
  }
  
  public void removeFavorite(String ticker) {
    favorites.remove(ticker);
    generateFavoritesQuery();
  }

  private void generateFavoritesQuery() {
    StringBuilder sb = new StringBuilder(IMPOSSIBLE_TICKER_SYMBOL);
    for (String ticker : favorites) {
      sb.append('|');
      sb.append(ticker);
    }
    favoritesQuery = sb.toString();
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

  private Result query(String query, Range range) {
    // Get all symbols for the query.
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
          Integer sharesOwned = sharesOwnedBySymbol.get(symbol);
          boolean favorite = favorites.contains(symbol);
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

