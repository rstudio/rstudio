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
package com.google.gwt.sample.bikeshed.stocks.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * A list of NYSE and NASDAQ stocks (note: this is a snapshot plus some
 * corrections, not a definitive list).  Stocks are listed in the file
 * symbols.txt, which contains a series of lines of the form:
 * 
 * <ticker> <name>
 */
public class Stocks {

  public static final List<String> SYMBOLS = new ArrayList<String>();

  public static final HashMap<String, String> companyNamesBySymbol =
    new HashMap<String, String>();
  
  public static final TreeSet<String> stockTickers = new TreeSet<String>();

  static {
    try {
      BufferedReader reader = new BufferedReader(new FileReader("symbols.txt"));
      String s;
      while ((s = reader.readLine()) != null) {
        int index = s.indexOf(' ');
        String symbol = s.substring(0, index).trim();
        String name = s.substring(index + 1).trim();
        SYMBOLS.add(symbol);
        SYMBOLS.add(name);
      }
      reader.close();
    } catch (IOException e) {
    }
    
    int num = SYMBOLS.size();
    for (int i = 0; i < num - 1; i += 2) {
      String symbol = SYMBOLS.get(i);
      String companyName = SYMBOLS.get(i + 1);
      stockTickers.add(symbol);
      companyNamesBySymbol.put(symbol, companyName);
    }
  }
}
