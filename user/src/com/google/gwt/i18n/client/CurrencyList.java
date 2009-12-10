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

import java.util.ArrayList;
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

  // This helper method exists because we can't call JSO instance methods
  // directly from JSNI.
  protected static boolean isDeprecated(CurrencyData currencyData) {
    return currencyData.isDeprecated();
  }

  /**
   * JS Object which contains a map of currency codes to CurrencyDataImpl
   * objects.  Each currency code is assumed to be a valid JS object key.
   */
  protected JavaScriptObject dataMap;

  /**
   * JS Object which contains a map of currency codes to localized currency
   * names. This is kept separate from {@link #dataMap} above so that the names
   * can be completely removed by the compiler if they are not used. Each
   * currency code is assumed to be a valid JS object key.
   */
  protected JavaScriptObject namesMap;

  /**
   * Return the default currency data for this locale.
   * 
   * Generated implementations override this method.
   */
  public native CurrencyData getDefault() /*-{
    return [ "USD", "$", 2, "US$" ];
  }-*/;

  /**
   * Returns an iterator for the list of currencies.
   * 
   * Deprecated currencies will not be included.
   */
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
    ArrayList<String> keys = new ArrayList<String>();
    loadCurrencyKeys(keys, includeDeprecated);
    final Iterator<String> it = keys.iterator();
    return new Iterator<CurrencyData>() {

      public boolean hasNext() {
        return it.hasNext();
      }

      public CurrencyData next() {
        return getEntry(it.next());
      }

      public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
      }
    };
  }

  /**
   * Lookup a currency based on the ISO4217 currency code.
   * 
   * @param currencyCode ISO4217 currency code
   * @return currency data, or null if code not found
   */
  public final CurrencyData lookup(String currencyCode) {
    ensureCurrencyMap();
    return getEntry(currencyCode);
  }

  /**
   * Lookup a currency name based on the ISO4217 currency code.
   * 
   * @param currencyCode ISO4217 currency code
   * @return name of the currency, or null if code not found
   */
  public final String lookupName(String currencyCode) {
    ensureNamesMap();
    return getNamesEntry(currencyCode);
  }

  /**
   * Ensure that the map of currency data has been initialized.
   */
  protected final void ensureCurrencyMap() {
    if (dataMap == null) {
      loadCurrencyMap();
    }
  }

  /**
   * Ensure that the map of currency data has been initialized.
   */
  protected final void ensureNamesMap() {
    if (namesMap == null) {
      loadNamesMap();
    }
  }

  /**
   * Directly reference an entry in the currency map JSO.
   * 
   * @param code ISO4217 currency code
   * @return currency data
   */
  protected final native CurrencyData getEntry(String code) /*-{
    return this.@com.google.gwt.i18n.client.CurrencyList::dataMap[code];
  }-*/;

  /**
   * Directly reference an entry in the currency names map JSO.
   * 
   * @param code ISO4217 currency code
   * @return currency name, or the currency code if not known
   */
  protected final native String getNamesEntry(String code) /*-{
    return this.@com.google.gwt.i18n.client.CurrencyList::namesMap[code] || code;
  }-*/;

  /**
   * Loads the currency map from a JS object literal.
   * 
   * Generated implementations override this method.
   */
  protected native void loadCurrencyMap() /*-{
    this.@com.google.gwt.i18n.client.CurrencyList::dataMap = {
        "USD": [ "USD", "$", 2 ],
        "EUR": [ "EUR", "€", 2 ],
        "GBP": [ "GBP", "UK£", 2 ],
        "JPY": [ "JPY", "¥", 0 ],
     };
  }-*/;

  /**
   * Loads the currency names map from a JS object literal.
   * 
   * Generated implementations override this method.
   */
  protected native void loadNamesMap() /*-{
    this.@com.google.gwt.i18n.client.CurrencyList::namesMap = {
        "USD": "US Dollar",
        "EUR": "Euro",
        "GBP": "British Pound Sterling",
        "JPY": "Japanese Yen",
     };
  }-*/;

  /**
   * Add all entries in {@code override} to the currency data map, replacing
   * any existing entries.  This is used by subclasses that need to slightly
   * alter the data used by the parent locale.
   * 
   * @param override JS object with currency code -> CurrencyData pairs
   */
  protected final native void overrideCurrencyMap(JavaScriptObject override) /*-{
    var map = this.@com.google.gwt.i18n.client.CurrencyList::dataMap;
    for (var key in override) {
      if (override.hasOwnProperty(key)) {
        map[key] = override[key];
      }
    }
  }-*/;

  /**
   * Add all entries in {@code override} to the currency name map, replacing
   * any existing entries.  This is used by subclasses that need to slightly
   * alter the data used by the parent locale.
   * 
   * @param override JS object with currency code -> name pairs
   */
  protected final native void overrideNamesMap(JavaScriptObject override) /*-{
    var map = this.@com.google.gwt.i18n.client.CurrencyList::namesMap;
    for (var key in override) {
      if (override.hasOwnProperty(key)) {
        map[key] = override[key];
      }
    }
  }-*/;

  /**
   * Add currency codes contained in the map to an ArrayList.
   */
  private native void loadCurrencyKeys(ArrayList<String> keys,
      boolean includeDeprecated) /*-{
    var map = this.@com.google.gwt.i18n.client.CurrencyList::dataMap;
    for (var key in map) {
      if (map.hasOwnProperty(key)) {
        if (includeDeprecated
            || !@com.google.gwt.i18n.client.CurrencyList::isDeprecated(Lcom/google/gwt/i18n/client/CurrencyData;)(map[key])) {
          keys.@java.util.ArrayList::add(Ljava/lang/Object;)(key);
        }
      }
    }
  }-*/;
}
