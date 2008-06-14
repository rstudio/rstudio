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

package com.google.gwt.i18n.client.impl;

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

  /**
   * JS Object which contains a map of currency codes to CurrencyData
   * objects.  Each currency code is prefixed with a ':' to allow
   * enumeration to find only the values we added, and not values
   * which various JSVMs add to objects.
   */
  protected JavaScriptObject dataMap;

  /**
   * JS Object which contains a map of currency codes to localized
   * currency names.  This is kept separate from the CurrencyData
   * map above so that the names can be completely removed by the
   * compiler if they are not used.  As iteration is not required,
   * no prefix is added to currency codes in this map.
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
   */
  public final Iterator<CurrencyData> iterator() {
    ensureCurrencyMap();
    ArrayList<String> keys = new ArrayList<String>();
    loadCurrencyKeys(keys);
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
    return this.@com.google.gwt.i18n.client.impl.CurrencyList::dataMap[':' + code];
  }-*/;

  /**
   * Directly reference an entry in the currency names map JSO.
   * 
   * @param code ISO4217 currency code
   * @return currency name
   */
  protected final native String getNamesEntry(String code) /*-{
    return this.@com.google.gwt.i18n.client.impl.CurrencyList::namesMap[code] || code;
  }-*/;

  /**
   * Loads the currency map from a JS object literal.
   * 
   * Generated implementations override this method.
   */
  protected native void loadCurrencyMap() /*-{
    this.@com.google.gwt.i18n.client.impl.CurrencyList::dataMap = {
        ":USD": [ "USD", "$", 2 ],
        ":EUR": [ "EUR", "€", 2 ],
        ":GBP": [ "GBP", "UK£", 2 ],
        ":JPY": [ "JPY", "¥", 0 ],
     };
  }-*/;

  /**
   * Loads the currency names map from a JS object literal.
   * 
   * Generated implementations override this method.
   */
  protected native void loadNamesMap() /*-{
    this.@com.google.gwt.i18n.client.impl.CurrencyList::namesMap = {
        "USD": "US Dollar",
        "EUR": "Euro",
        "GBP": "British Pound Sterling",
        "JPY": "Japanese Yen",
     };
  }-*/;

  /**
   * Add currency codes contained in the map to an ArrayList.
   */
  private native void loadCurrencyKeys(ArrayList<String> keys) /*-{
    var map = this.@com.google.gwt.i18n.client.impl.CurrencyList::dataMap;
    for (var key in map) {
      if (key.charCodeAt(0) == 58) {
        keys.@java.util.ArrayList::add(Ljava/lang/Object;)(key.substring(1));
      }
    }
  }-*/;
}
