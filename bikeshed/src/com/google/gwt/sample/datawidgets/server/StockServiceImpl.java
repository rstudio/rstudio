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

  static class MyStockQuote extends StockQuote implements
      Comparable<MyStockQuote> {

    public MyStockQuote(String ticker, String name, int price) {
      super(ticker, name, price);
    }

    public int compareTo(MyStockQuote o) {
      return getTicker().compareTo(o.getTicker());
    }
  }

  static HashMap<String, String> companyNamesBySymbol = new HashMap<String, String>();

  // TODO(rice) - use a smarter data structure
  static TreeSet<MyStockQuote> quotes = new TreeSet<MyStockQuote>();

  private static final int MAX_RESULTS_TO_RETURN = 10000;

  static {
    int num = NasdaqStocks.SYMBOLS.length;
    for (int i = 0; i < num - 1; i += 2) {
      String symbol = NasdaqStocks.SYMBOLS[i];
      String companyName = NasdaqStocks.SYMBOLS[i + 1];
      MyStockQuote stock = new MyStockQuote(symbol, companyName, 0);
      quotes.add(stock);

      companyNamesBySymbol.put(symbol, companyName);
    }
  }
  
  public List<StockResponse> getStockQuotes(List<StockRequest> requests) {
    List<StockResponse> responses = new ArrayList<StockResponse>();
    for (StockRequest request : requests) {
      responses.add(getStockQuotes(request));
    }
    
    // TODO (rice) add favorites response
    return responses;
  }

  public List<String> getSymbols(String query) {
    List<String> symbols = new ArrayList<String>();
    if (query.length() > 0) {
      query = query.toUpperCase();
      int count = 0;
      for (MyStockQuote stock : quotes) {
        String symbol = stock.getTicker();
        if (match(symbol, query)) {
          symbols.add(symbol);
          count++;
          if (count > MAX_RESULTS_TO_RETURN) {
            break;
          }
        }
      }
    }
    return symbols;
  }

  private StockResponse getStockQuotes(StockRequest request)
      throws IllegalArgumentException {
    String query = request.getQuery();
    Range range = request.getRange();

    // Get all symbols for the query.
    List<String> symbols = getSymbols(query);
    
    if (symbols.size() == 0) {
      return new StockResponse.Search(new StockQuoteList(0), 0);
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
      return new StockResponse.Search(new StockQuoteList(0), 0);
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
          priceMap.put(symbol, new StockQuote(symbol,
              companyNamesBySymbol.get(symbol), iprice));
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
        quote = new StockQuote(symbol, "<NO SUCH TICKER SYMBOL>", 0);
        System.out.println("Bad symbol " + symbol);
      }
      toRet.add(quote);
    }

    return new StockResponse.Search(toRet, symbols.size());
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
}

