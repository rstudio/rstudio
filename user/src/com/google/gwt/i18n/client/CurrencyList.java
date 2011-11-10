/*
 * Copyright 2008 Google Inc.
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

package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.impl.CurrencyDataImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Generated class containing all the CurrencyImpl instances.  This is just
 * the fallback in case the I18N module is not included.
 */
public class CurrencyList implements Iterable<CurrencyData> {

  /**
   * Inner class to avoid CurrencyList.clinit calls and allow this to be
   * completely removed from the generated code if instance isn't referenced
   * (such as when all you call is CurrencyList.get().getDefault() ).
   */
  private static class CurrencyListInstance {
    private static CurrencyList instance = GWT.create(CurrencyList.class);
  }

  /**
   * Return the singleton instance of CurrencyList.
   */
  public static CurrencyList get() {
    return CurrencyListInstance.instance;
  }

  /**
   * Add all entries in {@code override} to the original map, replacing
   * any existing entries.  This is used by subclasses that need to slightly
   * alter the data used by the parent locale.
   */
  protected static final native JavaScriptObject overrideMap(
      JavaScriptObject original, JavaScriptObject override) /*-{
    for (var key in override) {
      if (override.hasOwnProperty(key)) {
        original[key] = override[key];
      }
    }
    return original;
  }-*/;

  /**
   * Add currency codes contained in the map to an ArrayList.
   */
  private static native void loadCurrencyValuesNative(JavaScriptObject map, ArrayList<CurrencyData> collection) /*-{
    for (var key in map) {
      if (map.hasOwnProperty(key)) {
        collection.@java.util.ArrayList::add(Ljava/lang/Object;)(map[key]);
      }
    }
  }-*/;

  /**
   * Directly reference an entry in the currency names map JSO.
   * 
   * @param code ISO4217 currency code
   * @return currency name, or the currency code if not known
   */
  private static native String lookupNameNative(JavaScriptObject namesMap, String code) /*-{
    return namesMap[code] || code;
  }-*/;

  /**
   * Directly reference an entry in the currency map JSO.
   * 
   * @param code ISO4217 currency code
   * @return currency data
   */
  private static native CurrencyData lookupNative(JavaScriptObject dataMap, String code) /*-{
    return dataMap[code];
  }-*/;

  /**
   * Map of currency codes to CurrencyData.
   */
  protected HashMap<String, CurrencyData> dataMapJava;

  /**
   * JS map of currency codes to CurrencyData objects. Each currency code is
   * assumed to be a valid JS object key.
   */
  protected JavaScriptObject dataMapNative;

  /**
   * Map of currency codes to localized currency names. This is kept separate
   * from {@link #dataMapJava} above so that the names can be completely removed by
   * the compiler if they are not used.
   */
  protected HashMap<String, String> namesMapJava;

  /**
   * JS map of currency codes to localized currency names. This is kept separate
   * from {@link #dataMapNative} above so that the names can be completely
   * removed by the compiler if they are not used. Each currency code is assumed
   * to be a valid JS object key.
   */
  protected JavaScriptObject namesMapNative;
  
  /**
   * Return the default currency data for this locale.
   * 
   * Generated implementations override this method.
   */
  public CurrencyData getDefault() {
    if (GWT.isScript()) {
      return getDefaultNative();
    } else {
      return getDefaultJava();
    }
  }

  /**
   * Returns an iterator for the list of currencies.
   * 
   * Deprecated currencies will not be included.
   */
  @Override
  public final Iterator<CurrencyData> iterator() {
    return iterator(false);
  }

  /**
   * Returns an iterator for the list of currencies, optionally including
   * deprecated ones. 
   * 
   * @param includeDeprecated true if deprecated currencies should be included
   */
  public final Iterator<CurrencyData> iterator(boolean includeDeprecated) {
    ensureCurrencyMap();
    ArrayList<CurrencyData> collection = new ArrayList<CurrencyData>();
    if (GWT.isScript()) {
      loadCurrencyValuesNative(dataMapNative, collection);
    } else {
      for (CurrencyData item : dataMapJava.values()) {
        collection.add(item);
      }
    }
    if (!includeDeprecated) {
      ArrayList<CurrencyData> newCollection = new ArrayList<CurrencyData>();
      for (CurrencyData value : collection) {
        if (!value.isDeprecated()) {
          newCollection.add(value);
        }
      }
      collection = newCollection;
    }
    return Collections.unmodifiableList(collection).iterator();
  }

  /**
   * Lookup a currency based on the ISO4217 currency code.
   * 
   * @param currencyCode ISO4217 currency code
   * @return currency data, or null if code not found
   */
  public final CurrencyData lookup(String currencyCode) {
    ensureCurrencyMap();
    if (GWT.isScript()) {
      return lookupNative(dataMapNative, currencyCode);
    } else {
      return dataMapJava.get(currencyCode);
    }
  }

  /**
   * Lookup a currency name based on the ISO4217 currency code.
   * 
   * @param currencyCode ISO4217 currency code
   * @return name of the currency, or null if code not found
   */
  public final String lookupName(String currencyCode) {
    ensureNamesMap();
    if (GWT.isScript()) {
      return lookupNameNative(namesMapNative, currencyCode);
    } else {
      String result = namesMapJava.get(currencyCode);
      return (result == null) ? currencyCode : result;
    }
  }

  /**
   * Return the default currency data for this locale.
   * 
   * Generated implementations override this method.
   */
  protected CurrencyData getDefaultJava() {
    return new CurrencyDataImpl("USD", "$", 2, "US$", "$");
  }

  /**
   * Return the default currency data for this locale.
   * 
   * Generated implementations override this method.
   */
  protected native CurrencyData getDefaultNative() /*-{
    return [ "USD", "$", 2, "US$" ];
  }-*/;

  /**
   * Loads the currency map.
   * 
   * Generated implementations override this method.
   */
  protected HashMap<String, CurrencyData> loadCurrencyMapJava() {
    HashMap<String, CurrencyData> result = new HashMap<String, CurrencyData>();
    result.put("USD", new CurrencyDataImpl("USD", "$", 2, "US$", "$"));
    result.put("EUR", new CurrencyDataImpl("EUR", "€", 2, "€", "€"));
    result.put("GBP", new CurrencyDataImpl("GBP", "UK£", 2, "UK£", "£"));
    result.put("JPY", new CurrencyDataImpl("JPY", "¥", 0, "JP¥", "¥"));
    return result;
  }

  /**
   * Loads the currency map from a JS object literal.
   * 
   * Generated implementations override this method.
   */
  protected native JavaScriptObject loadCurrencyMapNative() /*-{
    return {
      "USD": [ "USD", "$", 2 ],
      "EUR": [ "EUR", "€", 2 ],
      "GBP": [ "GBP", "UK£", 2 ],
      "JPY": [ "JPY", "¥", 0 ],
    };
  }-*/;

  /**
   * Loads the currency names map.
   * 
   * Generated implementations override this method.
   */
  protected HashMap<String, String> loadNamesMapJava() {
    HashMap<String, String> result = new HashMap<String, String>();
    result.put("USD", "US Dollar");
    result.put("EUR", "Euro");
    result.put("GBP", "British Pound Sterling");
    result.put("JPY", "Japanese Yen");
    return result;
  }
  
  /**
   * Loads the currency names map from a JS object literal.
   * 
   * Generated implementations override this method.
   */
  protected native JavaScriptObject loadNamesMapNative() /*-{
    return {
      "USD": "US Dollar",
      "EUR": "Euro",
      "GBP": "British Pound Sterling",
      "JPY": "Japanese Yen",
    };
  }-*/;

  /**
   * Ensure that the map of currency data has been initialized.
   */
  private void ensureCurrencyMap() {
    if (GWT.isScript()) {
      if (dataMapNative == null) {
        dataMapNative = loadCurrencyMapNative();
      }
    } else {
      if (dataMapJava == null) {
        dataMapJava = loadCurrencyMapJava();
      }
    }
  }

  /**
   * Ensure that the map of currency data has been initialized.
   */
  private void ensureNamesMap() {
    if (GWT.isScript()) {
      if (namesMapNative == null) {
        namesMapNative = loadNamesMapNative();
      }
    } else {
      if (namesMapJava == null) {
        namesMapJava = loadNamesMapJava();
      }
    }
  }
}
