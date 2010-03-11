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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to perform queries against the Google Finance server.
 */
public class GoogleFinance {
  
  private static final Pattern DATA_PATTERN =
    Pattern.compile("\"([^\"]*)\"\\s*:\\s*\"([^\"]*)\"");

  private static final Pattern QUOTE_PATTERN = Pattern.compile("\\{[^\\}]*\\}");

  public static void queryServer(Set<String> symbolsInRange,
      Map<String, StockServiceImpl.Quote> quotes) {
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
  
    // Send the request.
    String content = "";
    try {
      String urlString = sb.toString();
      URL url = new URL(urlString);
      InputStream urlInputStream = url.openStream();
      Scanner contentScanner = new Scanner(urlInputStream, "UTF-8");
      if (contentScanner.hasNextLine()) {
        // See http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
        content = contentScanner.useDelimiter("\\A").next();
      }
  
      // System.out.println(content);
    } catch (MalformedURLException mue) {
      System.err.println(mue);
    } catch (IOException ioe) {
      System.err.println(ioe);
    }
  
    Matcher matcher = QUOTE_PATTERN.matcher(content);
    while (matcher.find()) {
      String group = matcher.group();
  
      String symbol = null;
      String dprice = null;
      String change = null;
  
      Matcher dataMatcher = DATA_PATTERN.matcher(group);
      while (dataMatcher.find()) {
        String tag = dataMatcher.group(1);
        String data = dataMatcher.group(2);
        if (tag.equals("t")) {
          symbol = data;
        } else if (tag.equals("l_cur")) {
          dprice = data;
        } else if (tag.equals("c")) {
          change = data;
        }
      }
  
      if (symbol != null && dprice != null && change != null) {
        try {
          int price = (int) (Double.parseDouble(dprice) * 100);
          
          // Cache the quote (will be good for 5 seconds)
          quotes.put(symbol, new StockServiceImpl.Quote(price, change));
        } catch (NumberFormatException e) {
          System.out.println("Bad price " + dprice + " for symbol " + symbol);
        }
      }
    }
  }

}
