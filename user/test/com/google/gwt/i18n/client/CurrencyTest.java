/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;

import java.util.Iterator;

/**
 * Tests for CurrencyList and CurrencyData (in addition to locale-specific
 * tests in I18N_*Test).
 */
public class CurrencyTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_es_MX";
  }

  public void testCustom() {
    CurrencyData currencyData = new DefaultCurrencyData("CAD", "/", 3);
    NumberFormat format = NumberFormat.getCurrencyFormat(currencyData);
    String formatted = format.format(1.23);
    assertEquals("/1.230", formatted);
    format = NumberFormat.getFormat("#0.0000\u00A4", currencyData);
    formatted = format.format(1234.23);
    assertEquals("1234.2300/", formatted);
  }

  public void testIterator() {
    CurrencyList list = CurrencyList.get();
    boolean found = false;
    for (CurrencyData data : list) {
      String code = data.getCurrencyCode();
      if ("USD".equals(code)) {
        found = true;
      } else if ("ITL".equals(code)) {
        fail("ITL in undeprecated list");
      }
    }
    assertTrue("USD not found in currency list", found);
    Iterator<CurrencyData> it = list.iterator(true);
    found = false;
    while (it.hasNext()) {
      CurrencyData data = it.next();
      String code = data.getCurrencyCode();
      if ("ITL".equals(code)) {
        found = true;
      }
    }
    assertTrue("ITL not found in deprecated currency list", found);
  }
  
  public void testLookup() {
    CurrencyList list = CurrencyList.get();
    assertNotNull("USD lookup failed", list.lookup("USD"));
    assertNotNull("ITL lookup failed", list.lookup("ITL"));
    assertEquals("dólar estadounidense", list.lookupName("USD"));
    assertEquals("lira italiana", list.lookupName("ITL"));
  }
}
