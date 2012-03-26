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
package com.google.gwt.tools.cldr;

import com.google.gwt.i18n.shared.GwtLocale;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Loads data needed to produce DateTimeFormatInfo implementations.
 */
public class CurrencyDataProcessor extends Processor {

  private Map<String, Integer> currencyFractions = new HashMap<String, Integer>();
  private int defaultCurrencyFraction;
  private Map<String, Integer> rounding = new HashMap<String, Integer>();

  private Set<String> stillInUse = new HashSet<String>();

  public CurrencyDataProcessor(File outputDir, Factory cldrFactory, LocaleData localeData) {
    super(outputDir, cldrFactory, localeData);
  }

  @Override
  protected void cleanupData() {
    localeData.removeDuplicates("currency");
  }

  @Override
  protected void loadData() throws IOException {
    System.out.println("Loading data for currencies");
    localeData.addVersions(cldrFactory);
    loadLocaleIndependentCurrencyData();
    localeData.addCurrencyEntries("currency", cldrFactory, currencyFractions,
        defaultCurrencyFraction, stillInUse, rounding);
  }

  @Override
  protected void printHeader(PrintWriter pw) {
    printPropertiesHeader(pw);
    pw.println();
    pw.println("#");
    pw.println("# The key is an ISO4217 currency code, and the value is of the " + "form:");
    pw.println("#   display name|symbol|decimal digits|not-used-flag|rounding");
    pw.println("# If a symbol is not supplied, the currency code will be used");
    pw.println("# If # of decimal digits is omitted, 2 is used");
    pw.println("# If a currency is not generally used, not-used-flag=1");
    pw.println("# If a currency should be rounded to a multiple of of the least significant");
    pw.println("#   digit, rounding will be present");
    pw.println("# Trailing empty fields can be omitted");
    pw.println();
  }

  @Override
  protected void writeOutputFiles() throws IOException {
    for (GwtLocale locale : localeData.getNonEmptyLocales()) {
      String path = "client/impl/cldr/CurrencyData";
      PrintWriter pw = createOutputFile(path + Processor.localeSuffix(locale) + ".properties");
      printHeader(pw);
      printVersion(pw, locale, "# ");
      Map<String, String> map = localeData.getEntries("currency", locale);
      String[] keys = new String[map.size()];
      map.keySet().toArray(keys);
      Arrays.sort(keys);

      for (String key : keys) {
        pw.print(key);
        pw.print(" = ");
        pw.println(map.get(key));
      }
      pw.close();
    }
  }

  private void loadLocaleIndependentCurrencyData() {
    CLDRFile supp = cldrFactory.getSupplementalData();

    // load the table of default # of decimal places and rounding for each currency
    defaultCurrencyFraction = 0;
    XPathParts parts = new XPathParts();
    Iterator<String> iterator = supp.iterator("//supplementalData/currencyData/fractions/info");
    while (iterator.hasNext()) {
      String path = iterator.next();
      parts.set(supp.getFullXPath(path));
      Map<String, String> attr = parts.findAttributes("info");
      if (attr == null) {
        continue;
      }
      String curCode = attr.get("iso4217");
      int digits = Integer.valueOf(attr.get("digits"));
      if ("DEFAULT".equalsIgnoreCase(curCode)) {
        defaultCurrencyFraction = digits;
      } else {
        currencyFractions.put(curCode, digits);
      }
      int roundingDigits = Integer.valueOf(attr.get("rounding"));
      if (roundingDigits != 0) {
        rounding.put(curCode, roundingDigits);
      }
    }

    // find which currencies are still in use in some region, everything else
    // should be marked as deprecated
    iterator = supp.iterator("//supplementalData/currencyData/region");
    while (iterator.hasNext()) {
      String path = iterator.next();
      parts.set(supp.getFullXPath(path));
      Map<String, String> attr = parts.findAttributes("currency");
      if (attr == null) {
        continue;
      }
      String region = parts.findAttributeValue("region", "iso3166");
      String curCode = attr.get("iso4217");
      if ("ZZ".equals(region) || "false".equals(attr.get("tender")) || "XXX".equals(curCode)) {
        // ZZ is an undefined region, XXX is an unknown currency code (and needs
        // to be special-cased because it is listed as used in Anartica!)
        continue;
      }
      String to = attr.get("to");
      if (to == null) {
        stillInUse.add(curCode);
      }
    }
  }
}
