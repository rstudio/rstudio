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
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Collects all the locale data from CLDR, grouping it by how it will be used
 * and removing equivalent values that could be inherited.
 */
public class LocaleData {

  /**
   * Represents data about a single currency in a particular locale from CLDR.
   */
  public static class Currency {

    private static boolean equalsNullCheck(Object a, Object b) {
      if (a == null) {
        return b == null;
      }
      return a.equals(b);
    }

    private static int hashCodeNullCheck(Object obj) {
      return obj == null ? 0 : obj.hashCode();
    }

    private final String code;

    private int decimalDigits;

    private String decimalSeparator;

    private String displayName;

    private String groupingSeparator;

    private boolean inUse;

    private String pattern;

    private int rounding;

    private String symbol;

    public Currency(String code) {
      this.code = code;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Currency)) {
        return false;
      }
      Currency other = (Currency) obj;
      return code.equals(other.code) && equalsNullCheck(displayName, other.displayName)
          && equalsNullCheck(symbol, other.symbol) && equalsNullCheck(pattern, other.pattern)
          && equalsNullCheck(decimalSeparator, other.decimalSeparator)
          && equalsNullCheck(groupingSeparator, other.groupingSeparator)
          && decimalDigits == other.decimalDigits && inUse == other.inUse
          && rounding == other.rounding;
    }

    public String getCode() {
      return code;
    }

    /**
     * @return the number of decimal digits this currency is commonly displayed
     *         with.
     */
    public int getDecimalDigits() {
      return decimalDigits;
    }

    public String getDecimalSeparator() {
      return decimalSeparator;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getGroupingSeparator() {
      return groupingSeparator;
    }

    public String getPattern() {
      return pattern;
    }

    public int getRounding() {
      return rounding;
    }

    public String getSymbol() {
      return symbol;
    }

    @Override
    public int hashCode() {
      return code.hashCode() + 17 * hashCodeNullCheck(displayName) + 19 * hashCodeNullCheck(symbol)
          + 23 * hashCodeNullCheck(pattern) + 29 * hashCodeNullCheck(decimalSeparator) + 31
          * hashCodeNullCheck(groupingSeparator) + 37 * decimalDigits + (inUse ? 41 : 0)
          + 43 * rounding;
    }

    /**
     * @return true if this currency is still in regular use.
     */
    public boolean isInUse() {
      return inUse;
    }

    public void setDecimalDigits(int decimalDigits) {
      this.decimalDigits = decimalDigits;
    }

    public void setDecimalSeparator(String decimalSeparator) {
      this.decimalSeparator = decimalSeparator;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public void setGroupingSeparator(String groupingSeparator) {
      this.groupingSeparator = groupingSeparator;
    }

    public void setInUse(boolean inUse) {
      this.inUse = inUse;
    }

    public void setPattern(String pattern) {
      this.pattern = pattern;
    }

    public void setRounding(int rounding) {
      this.rounding = rounding;
    }

    public void setSymbol(String symbol) {
      this.symbol = symbol;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append(code);
      if (displayName != null || symbol != null) {
        buf.append(" (");
      }
      if (displayName != null) {
        buf.append(displayName);
      }
      if (symbol != null) {
        if (displayName != null) {
          buf.append("; ");
        }
        buf.append(symbol);
      }
      if (displayName != null || symbol != null) {
        buf.append(")");
      }
      return buf.toString();
    }
  }

  /**
   * Comparator that orders locales based the inheritance depth.
   */
  private class LocaleComparator implements Comparator<GwtLocale> {
    @Override
    public int compare(GwtLocale a, GwtLocale b) {
      Integer depthA = localeDepth.get(a);
      Integer depthB = localeDepth.get(b);
      int c = 0;
      if (depthA != null && depthB != null) {
        c = depthB - depthA;
      }
      if (c == 0) {
        c = a.compareTo(b);
      }
      return c;
    }
  }

  /**
   * Encapsulates the key for lookup values, comprising a locale and a category.
   */
  private static class MapKey {
    private final String category;
    private final GwtLocale locale;

    public MapKey(String category, GwtLocale locale) {
      this.category = category;
      this.locale = locale;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      MapKey other = (MapKey) obj;
      return locale.equals(other.locale) && category.equals(other.category);
    }

    public String getCategory() {
      return category;
    }

    public GwtLocale getLocale() {
      return locale;
    }

    @Override
    public int hashCode() {
      return category.hashCode() + 31 * locale.hashCode();
    }

    public MapKey inNewLocale(GwtLocale newLocale) {
      return new MapKey(category, newLocale);
    }

    @Override
    public String toString() {
      return "[cat=" + category + ", locale=" + locale + "]";
    }
  }

  /**
   * Return the CLDR locale name for a GWT locale.
   * 
   * @param locale
   * @return CLDR locale name for GWT locale
   */
  public static String getCldrLocale(GwtLocale locale) {
    return locale.isDefault() ? "root" : locale.toString();
  }

  /**
   * Get the value of a given category of territory data inherited by a locale.
   * 
   * @param locale the locale to search for
   * @param map the map containing territory=>value data
   * @return the requested value from the closest ancestor of the specified
   *         locale, or null if not found
   */
  private static String getTerritoryData(GwtLocale locale, Map<String, String> map) {
    if (map == null) {
      return null;
    }
    for (GwtLocale search : locale.getCompleteSearchList()) {
      String region = search.getRegion();
      if (region == null) {
        region = "001";
      }
      String value = map.get(region);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private final Map<GwtLocale, String> allLocales;

  private final GwtLocale defaultLocale;

  private final HashMap<GwtLocale, GwtLocale> inheritsFrom;

  private final Map<GwtLocale, Integer> localeDepth;

  private final GwtLocaleFactory localeFactory;

  private Map<MapKey, Map<String, String>> maps;

  /**
   * Construct a LocaleData object.
   * 
   * @param localeFactory
   * @param localeNames
   */
  public LocaleData(GwtLocaleFactory localeFactory, Collection<String> localeNames) {
    this.localeFactory = localeFactory;
    defaultLocale = localeFactory.getDefault();
    allLocales = new HashMap<GwtLocale, String>();
    for (String localeName : localeNames) {
      allLocales.put(getGwtLocale(localeName), localeName);
    }
    inheritsFrom = new HashMap<GwtLocale, GwtLocale>();
    buildInheritsFrom();
    localeDepth = new HashMap<GwtLocale, Integer>();
    maps = new HashMap<MapKey, Map<String, String>>();
    buildLocaleDepth();
  }

  /**
   * Add a single entry from an attribute on a CLDR node.
   * 
   * @param category
   * @param locale
   * @param cldrFactory
   * @param path
   * @param tag
   * @param key
   * @param attribute
   */
  public void addAttributeEntry(String category, GwtLocale locale, Factory cldrFactory,
      String path, String tag, String key, String attribute) {
    Map<String, String> map = getMap(category, locale);
    CLDRFile cldr = cldrFactory.make(allLocales.get(locale), true);
    XPathParts parts = new XPathParts();
    parts.set(cldr.getFullXPath(path));
    Map<String, String> attr = parts.findAttributes(tag);
    if (attr == null) {
      return;
    }
    String value = attr.get(attribute);
    map.put(key, value);
  }

  /**
   * Add currency entries for all locales.
   * 
   * @param category
   * @param cldrFactory
   * @param currencyFractions map of currency fraction data extracted from
   *          locale-independent data
   * @param defaultCurrencyFraction
   * @param stillInUse
   * @param rounding 
   */
  public void addCurrencyEntries(String category, Factory cldrFactory,
      Map<String, Integer> currencyFractions, int defaultCurrencyFraction, Set<String> stillInUse,
      Map<String, Integer> rounding) {
    for (GwtLocale locale : allLocales.keySet()) {
      // skip the "default" locale for now
      if (locale.isDefault()) {
        continue;
      }
      addCurrencyEntries(category, locale, cldrFactory, currencyFractions, defaultCurrencyFraction,
          stillInUse, rounding);
    }
    // run the "default" locale last, to override inherited entries
    GwtLocale locale = localeFactory.getDefault();
    addCurrencyEntries(category, locale, cldrFactory, currencyFractions, defaultCurrencyFraction,
        stillInUse, rounding);
  }

  public void addDateTimeFormatEntries(String group, Factory cldrFactory) {
    addAttributeEntries(group, cldrFactory, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/"
        + group + "Formats/default", "default", "default", "choice");
    addDateTimeFormatEntries(group, "full", cldrFactory);
    addDateTimeFormatEntries(group, "long", cldrFactory);
    addDateTimeFormatEntries(group, "medium", cldrFactory);
    addDateTimeFormatEntries(group, "short", cldrFactory);
  }

  public void addEntries(String category, Factory cldrFactory, String prefix, String tag,
      String keyAttribute) {
    for (GwtLocale locale : allLocales.keySet()) {
      addEntries(category, locale, cldrFactory, prefix, tag, keyAttribute);
    }
  }

  public void addEntries(String category, GwtLocale locale, Factory cldrFactory, String prefix,
      String tag, String keyAttribute) {
    Map<String, String> map = getMap(category, locale);
    CLDRFile cldr = cldrFactory.make(allLocales.get(locale), true);
    XPathParts parts = new XPathParts();
    Iterator<String> iterator = cldr.iterator(prefix);
    while (iterator.hasNext()) {
      String path = iterator.next();
      String fullXPath = cldr.getFullXPath(path);
      if (fullXPath == null) {
        fullXPath = path;
      }
      parts.set(fullXPath);
      if (parts.containsAttribute("alt")) {
        // ignore alternate strings
        continue;
      }
      Map<String, String> attr = parts.findAttributes(tag);
      if (attr == null) {
        continue;
      }
      String value = cldr.getStringValue(path);
      boolean draft = parts.containsAttribute("draft");
      String key = keyAttribute != null ? attr.get(keyAttribute) : "value";
      if (!draft || !map.containsKey(key)) {
        map.put(key, value);
      }
    }
  }

  public void addEntry(String category, GwtLocale locale, String key, String value) {
    Map<String, String> map = getMap(category, locale);
    map.put(key, value);
  }

  /**
   * @param period "month", "day", "quarter", "dayPeriod",
   * @param cldrFactory
   */
  public void addNameEntries(String period, Factory cldrFactory) {
    addEntries(period + "-abbrev", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" + period + "s/" + period
            + "Context[@type=\"format\"]/" + period + "Width[@type=\"abbreviated\"]", period,
        "type");
    addEntries(period + "-narrow", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" + period + "s/" + period
            + "Context[@type=\"format\"]/" + period + "Width[@type=\"narrow\"]", period, "type");
    addEntries(period + "-wide", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" + period + "s/" + period
            + "Context[@type=\"format\"]/" + period + "Width[@type=\"wide\"]", period, "type");
    addEntries(period + "-sa-abbrev", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" + period + "s/" + period
            + "Context[@type=\"stand-alone\"]/" + period + "Width[@type=\"abbreviated\"]", period,
        "type");
    addEntries(period + "-sa-narrow", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" + period + "s/" + period
            + "Context[@type=\"stand-alone\"]/" + period + "Width[@type=\"narrow\"]", period,
        "type");
    addEntries(period + "-sa-wide", cldrFactory,
        "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" + period + "s/" + period
            + "Context[@type=\"stand-alone\"]/" + period + "Width[@type=\"wide\"]", period, "type");
  }

  /**
   * Add entries from territory-oriented CLDR data.
   * 
   * @param category category to store resulting data under
   * @param cldrFactory
   * @param regionLanguageData
   * @param prefix the XPath prefix to iterate through
   * @param tag the tag to load
   * @param keyAttribute the attribute in the tag to use as the key
   */
  public void addTerritoryEntries(String category, Factory cldrFactory,
      RegionLanguageData regionLanguageData, String prefix, String tag, String keyAttribute) {
    CLDRFile supp = cldrFactory.getSupplementalData();
    Map<String, String> map = new HashMap<String, String>();
    XPathParts parts = new XPathParts();
    Iterator<String> iterator = supp.iterator(prefix);
    while (iterator.hasNext()) {
      String path = iterator.next();
      parts.set(supp.getFullXPath(path));
      Map<String, String> attr = parts.findAttributes(tag);
      if (attr == null || attr.get("alt") != null) {
        continue;
      }
      String key = attr.get(keyAttribute);
      String territories = attr.get("territories");
      String draft = attr.get("draft");
      for (String territory : territories.split(" ")) {
        if (draft == null || !map.containsKey(territory)) {
          map.put(territory, key);
        }
      }
    }

    if (regionLanguageData != null) {
      // find the choice used by most literate speakers of each language
      // based on region-based preferences.
      summarizeTerritoryEntries(category, regionLanguageData, tag, map);
    }
  }

  public void addVersions(Factory cldrFactory) {
    for (GwtLocale locale : allLocales.keySet()) {
      Map<String, String> map = getMap("version", locale);
      CLDRFile cldr = cldrFactory.make(allLocales.get(locale), true);
      XPathParts parts = new XPathParts();
      Iterator<String> iterator = cldr.iterator("//ldml/identity");
      while (iterator.hasNext()) {
        String path = iterator.next();
        String fullXPath = cldr.getFullXPath(path);
        if (fullXPath == null) {
          fullXPath = path;
        }
        parts.set(fullXPath);
        Map<String, String> attr = parts.getAttributes(2);
        if (attr == null) {
          continue;
        }
        for (Map.Entry<String, String> entry : attr.entrySet()) {
          map.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  /**
   * Add a redirect entry for each locale where all entries in the standalone
   * category match those in the base category.
   * 
   * @param baseCategory
   * @param standaloneCategory
   */
  public void computeRedirects(String baseCategory, String standaloneCategory) {
    for (GwtLocale locale : allLocales.keySet()) {
      MapKey baseKey = new MapKey(baseCategory, locale);
      MapKey standaloneKey = new MapKey(standaloneCategory, locale);
      Map<String, String> baseMap = maps.get(baseKey);
      Map<String, String> standaloneMap = maps.get(standaloneKey);
      if (baseMap != null && standaloneMap != null
          && (standaloneMap.isEmpty() || baseMap.equals(standaloneMap))) {
        addEntry(standaloneCategory + "-redirect", locale, "redirect", "yes");
      }
    }
  }

  /**
   * Copy data from one locale to another.
   * 
   * @param srcLocaleName source locale name
   * @param destLocaleName destination locale name
   * @param categories list of categories to copy
   */
  public void copyLocaleData(String srcLocaleName, String destLocaleName, String... categories) {
    GwtLocale src = localeFactory.fromString(srcLocaleName);
    GwtLocale dest = localeFactory.fromString(destLocaleName);
    for (String category : categories) {
      Map<String, String> srcMap = maps.get(new MapKey(category, src));
      if (srcMap == null || srcMap.isEmpty()) {
        continue;
      }
      Map<String, String> destMap = getMap(category, dest);
      destMap.putAll(srcMap);
    }
  }

  public Map<String, Map<String, String>> getAllEntries(String localeName) {
    GwtLocale locale = localeFactory.fromString(localeName);
    Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
    for (Map.Entry<MapKey, Map<String, String>> entry : maps.entrySet()) {
      Map<String, String> map = entry.getValue();
      if (entry.getKey().getLocale().equals(locale) && !map.isEmpty()) {
        result.put(entry.getKey().getCategory(), Collections.unmodifiableMap(entry.getValue()));
      }
    }
    return result;
  }

  /**
   * @return all locales present in the CLDR data.
   */
  public Set<GwtLocale> getAllLocales() {
    return Collections.unmodifiableSet(allLocales.keySet());
  }

  /**
   * Return all entries in a given category and locale.
   * 
   * @param category
   * @param locale
   * @return map of keys to localized values
   */
  public Map<String, String> getEntries(String category, GwtLocale locale) {
    MapKey mapKey = new MapKey(category, locale);
    Map<String, String> map = maps.get(mapKey);
    if (map == null) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(map);
  }

  /**
   * Return a single value.
   * 
   * @param category
   * @param locale
   * @param key
   * @return the requested value, or null if not present
   */
  public String getEntry(String category, GwtLocale locale, String key) {
    MapKey mapKey = new MapKey(category, locale);
    Map<String, String> map = maps.get(mapKey);
    if (map == null) {
      return null;
    }
    return map.get(key);
  }

  /**
   * @param localeName
   * @return GwtLocale instance for CLDR locale
   */
  public GwtLocale getGwtLocale(String localeName) {
    return "root".equals(localeName) ? localeFactory.getDefault() : localeFactory
        .fromString(localeName);
  }

  /**
   * Return a single value, following locale inheritance.
   * 
   * @param category
   * @param locale
   * @param key
   * @return the requested value, or null if not present
   */
  public String getInheritedEntry(String category, GwtLocale locale, String key) {
    for (GwtLocale search : locale.getCompleteSearchList()) {
      String value = getEntry(category, search, key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  /**
   * @return all locales that have some data associated with them.
   */
  public Set<GwtLocale> getNonEmptyLocales() {
    Set<GwtLocale> result = new HashSet<GwtLocale>();
    for (Map.Entry<MapKey, Map<String, String>> entry : maps.entrySet()) {
      Map<String, String> map = entry.getValue();
      if (map.isEmpty()) {
        continue;
      }
      result.add(entry.getKey().getLocale());
    }
    return result;
  }

  /**
   * @param category 
   * @return all locales that have some data associated with them in the
   *         specified category.
   */
  public Set<GwtLocale> getNonEmptyLocales(String category) {
    Set<GwtLocale> result = new HashSet<GwtLocale>();
    for (Map.Entry<MapKey, Map<String, String>> entry : maps.entrySet()) {
      Map<String, String> map = entry.getValue();
      if (!category.equals(entry.getKey().category) || map.isEmpty()) {
        continue;
      }
      result.add(entry.getKey().getLocale());
    }
    return result;
  }

  /**
   * Return the nearest ancestor locale of the supplied locale which has any
   * values present.
   * 
   * @param locale
   * @return GwtLocale of nearest ancestor
   */
  public GwtLocale inheritsFrom(GwtLocale locale) {
    GwtLocale parent = inheritsFrom.get(locale);
    while (parent != null && parent != defaultLocale) {
      for (Map.Entry<MapKey, Map<String, String>> entry : maps.entrySet()) {
        MapKey key = entry.getKey();
        // Version entries are always present, so ignore them
        if (!"version".equals(key.getCategory()) && key.getLocale().equals(parent)) {
          Map<String, String> map = entry.getValue();
          if (!map.isEmpty()) {
            return parent;
          }
        }
      }
      parent = inheritsFrom.get(parent);
    }
    return parent;
  }

  /**
   * Return the nearest ancestor locale of the supplied locale which has any
   * values present in the specified category.
   * 
   * @param category
   * @param locale
   * @return GwtLocale of nearest ancestor with the specified category
   */
  public GwtLocale inheritsFrom(String category, GwtLocale locale) {
    GwtLocale parent = inheritsFrom.get(locale);
    while (parent != null && parent != defaultLocale) {
      Map<String, String> map = getMap(category, parent);
      if (!map.isEmpty()) {
        return parent;
      }
      parent = inheritsFrom.get(parent);
    }
    return parent;
  }

  /**
   * Remove locale entries that completely duplicate their parent.
   */
  public void removeCompleteDuplicates() {
    removeCompleteDuplicates(null);
  }

  /**
   * Remove locale entries that completely duplicate their parent.
   * 
   * @param matchCategory
   */
  public void removeCompleteDuplicates(String matchCategory) {
    MapKey[] keys = getSortedMapKeys();
    for (MapKey key : keys) {
      String category = key.getCategory();
      if (matchCategory != null && !matchCategory.equals(category)) {
        continue;
      }
      GwtLocale locale = key.getLocale();
      GwtLocale parent = inheritsFrom(category, locale);
      if (parent == null) {
        continue;
      }
      MapKey parentKey = key.inNewLocale(parent);
      Map<String, String> parentMap = maps.get(parentKey);
      if (parentMap == null) {
        continue;
      }
      Map<String, String> map = maps.get(key);
      boolean allMatch = true;
      for (Map.Entry<String, String> entry : map.entrySet()) {
        if (!entry.getValue().equals(parentMap.get(entry.getKey()))) {
          allMatch = false;
          break;
        }
      }
      if (allMatch) {
        maps.remove(key);
      }
    }
  }

  /**
   * Remove entries that are duplicates of the entries in the parent locale.
   */
  public void removeDuplicates() {
    removeDuplicates(null);
  }

  /**
   * Remove entries that are duplicates of the entries in the parent locale.
   * 
   * @param matchCategory
   */
  public void removeDuplicates(String matchCategory) {
    MapKey[] keys = getSortedMapKeys();
    for (MapKey key : keys) {
      String category = key.getCategory();
      if (matchCategory != null && !matchCategory.equals(category)) {
        continue;
      }
      GwtLocale locale = key.getLocale();
      GwtLocale parent = inheritsFrom(category, locale);
      if (parent == null) {
        continue;
      }
      MapKey parentKey = key.inNewLocale(parent);
      Map<String, String> parentMap = maps.get(parentKey);
      if (parentMap == null) {
        continue;
      }
      Map<String, String> map = maps.get(key);
      Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<String, String> entry = it.next();
        String value = entry.getValue();
        if (value == null || value.equals(parentMap.get(entry.getKey()))) {
          it.remove();
        }
      }
    }
  }

  /**
   * Remove entries in the specified category and locale which match any of the
   * supplied keys.
   * 
   * @param category
   * @param locale
   * @param keys
   */
  public void removeEntries(String category, GwtLocale locale, Collection<String> keys) {
    Map<String, String> map = getMap(category, locale);
    map.keySet().removeAll(keys);
  }

  /**
   * Remove a single entry, if present.
   * 
   * @param category
   * @param locale
   * @param key
   */
  public void removeEntry(String category, GwtLocale locale, String key) {
    Map<String, String> map = getMap(category, locale);
    map.remove(key);
  }

  /**
   * Reset state, forgetting any cached values.
   */
  public void reset() {
    maps.clear();
  }

  /**
   * Summarize values known by territories to bare languages, based on
   * populations using a particular value.
   * 
   * @param category
   * @param key
   * @param values map of region codes to values
   */
  public void summarizeTerritoryEntries(String category, RegionLanguageData regionLanguageData,
      String key, Map<String, String> values) {
    for (GwtLocale locale : allLocales.keySet()) {
      if (locale.getRegion() != null || locale.getLanguage() == null) {
        // skip any that have a region or don't have a language
        continue;
      }
      String language = locale.getAsString();
      Map<String, Double> valueMap = new HashMap<String, Double>();
      for (RegionLanguageData.RegionPopulation langData : regionLanguageData.getRegions(language)) {
        String region = langData.getRegion();
        GwtLocale regionLocale = localeFactory.fromString(language + "_" + region);
        String value = getTerritoryData(regionLocale, values);
        if (value != null) {
          Double pop = valueMap.get(value);
          if (pop == null) {
            pop = 0.0;
          }
          pop += langData.getLiteratePopulation();
          valueMap.put(value, pop);
        }
      }
      double max = 0;
      String maxValue = null;
      for (Map.Entry<String, Double> entry : valueMap.entrySet()) {
        if (entry.getValue() > max) {
          max = entry.getValue();
          maxValue = entry.getKey();
        }
      }
      if (maxValue != null) {
        addEntry(category, locale, key, maxValue);
      }
    }

    // map locales to territory data
    for (GwtLocale locale : allLocales.keySet()) {
      if (getEntry(category, locale, key) != null) {
        // don't override what we set above
        continue;
      }
      String value = getTerritoryData(locale, values);
      if (value != null) {
        addEntry(category, locale, key, value);
      }
    }
  }

  private void addAttributeEntries(String category, Factory cldrFactory, String prefix, String tag,
      String key, String attribute) {
    for (GwtLocale locale : allLocales.keySet()) {
      addAttributeEntry(category, locale, cldrFactory, prefix, tag, key, attribute);
    }
  }

  /**
   * Add currency entries for the specified locale. If this locale is not the
   * default locale, also add default entries into the default locale to make
   * sure it has entries for any currency present in any locale. Note that this
   * means that the default locale must be processed last.
   * 
   * @param category
   * @param locale
   * @param cldrFactory
   * @param currencyFractions map of currency fraction data extracted from
   *          locale-independent data
   * @param defaultCurrencyFraction
   * @param stillInUse
   * @param rounding 
   */
  private void addCurrencyEntries(String category, GwtLocale locale, Factory cldrFactory,
      Map<String, Integer> currencyFractions, int defaultCurrencyFraction, Set<String> stillInUse,
      Map<String, Integer> rounding) {
    Map<String, String> outputMap = getMap(category, locale);
    Map<String, String> defaultMap = null;
    if (!locale.isDefault()) {
      defaultMap = getMap(category, localeFactory.getDefault());
    }
    Map<String, Currency> tempMap = new HashMap<String, Currency>();
    CLDRFile cldr = cldrFactory.make(allLocales.get(locale), true);
    XPathParts parts = new XPathParts();
    Iterator<String> iterator = cldr.iterator("//ldml/numbers/currencies");
    while (iterator.hasNext()) {
      String path = iterator.next();
      String fullPath = cldr.getFullXPath(path);
      if (fullPath == null) {
        fullPath = path;
      }
      parts.set(fullPath);
      Map<String, String> attr = parts.findAttributes("currency");
      if (attr == null) {
        continue;
      }
      String currencyCode = attr.get("type");
      Currency currency = tempMap.get(currencyCode);
      if (currency == null) {
        currency = new Currency(currencyCode);
        if (currencyFractions.containsKey(currencyCode)) {
          currency.setDecimalDigits(currencyFractions.get(currencyCode));
        } else {
          currency.setDecimalDigits(defaultCurrencyFraction);
        }
        currency.setInUse(stillInUse.contains(currencyCode));
        tempMap.put(currencyCode, currency);
        Integer roundingMult = rounding.get(currencyCode);
        if (roundingMult != null) {
          currency.setRounding(roundingMult);
        }
      }
      String field = parts.getElement(4);
      String value = cldr.getStringValue(fullPath);
      attr = parts.findAttributes(field);
      if (attr == null) {
        attr = Collections.emptyMap();
      }
      String draft = attr.get("draft");
      if ("symbol".equalsIgnoreCase(field)) {
        currency.setSymbol(value);
      } else if ("displayName".equalsIgnoreCase(field)) {
        if (attr.get("count") != null) {
          // We don't care about currency "count" names
          continue;
        }
        if (draft == null || currency.getDisplayName() == null) {
          // don't override non-draft name with draft name
          currency.setDisplayName(value);
        }
      } else if ("pattern".equalsIgnoreCase(field)) {
        currency.setPattern(value);
      } else if ("decimal".equalsIgnoreCase(field)) {
        currency.setDecimalSeparator(value);
      } else if ("group".equalsIgnoreCase(field)) {
        currency.setGroupingSeparator(value);
      } else {
        System.err.println("Ignoring unknown field \"" + field + "\" on currency data for \""
            + currencyCode + "\"");
      }
    }
    for (Currency currency : tempMap.values()) {
      String code = currency.getCode();
      outputMap.put(code, encodeCurrencyData(currency));
      if (defaultMap != null) {
        // Don't copy language-specific things to default
        currency.setDisplayName(code);
        currency.setSymbol(null);
        defaultMap.put(code, encodeCurrencyData(currency));
      }
    }
  }

  private void addDateTimeFormatEntries(String group, String length, Factory cldrFactory) {
    addEntries(group, cldrFactory, "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/" + group
        + "Formats/" + group + "FormatLength" + "[@type=\"" + length + "\"]/" + group
        + "Format[@type=\"standard\"]" + "/pattern[@type=\"standard\"]", group + "FormatLength",
        "type");
  }

  private void buildInheritsFrom() {
    for (GwtLocale locale : allLocales.keySet()) {
      GwtLocale parent = null;
      for (GwtLocale search : locale.getInheritanceChain()) {
        if (!search.equals(locale) && allLocales.containsKey(search)) {
          parent = search;
          break;
        }
      }
      inheritsFrom.put(locale, parent);
    }
  }

  /**
   * Build a depth map which is used to sort locales such that more derived
   * locales are processed before less derived locales.
   */
  private void buildLocaleDepth() {
    Set<GwtLocale> remaining = new HashSet<GwtLocale>(allLocales.keySet());
    localeDepth.put(defaultLocale, 0);
    remaining.remove(defaultLocale);
    while (!remaining.isEmpty()) {
      Set<GwtLocale> nextPass = new HashSet<GwtLocale>();
      for (GwtLocale locale : remaining) {
        GwtLocale parent = inheritsFrom.get(locale);
        if (localeDepth.containsKey(parent)) {
          int depth = localeDepth.get(parent);
          localeDepth.put(locale, depth + 1);
        } else {
          nextPass.add(locale);
        }
      }
      remaining = nextPass;
    }
  }

  /**
   * Encode the currency data as needed by CurrencyListGenerator.
   * 
   * @param currency
   * @return a string containing the property file entry for the specified
   *         currency
   */
  private String encodeCurrencyData(Currency currency) {
    StringBuilder buf = new StringBuilder();
    String skipped = "";
    String displayName = currency.getDisplayName();
    if (displayName == null) {
      displayName = currency.getCode();
    }
    buf.append(displayName);
    String symbol = currency.getSymbol();
    if (symbol != null && !currency.getCode().equals(symbol)) {
      buf.append('|');
      buf.append(symbol);
      skipped = "";
    } else {
      skipped = "|";
    }
    if (currency.getDecimalDigits() != 2) {
      buf.append(skipped).append('|');
      buf.append(currency.getDecimalDigits());
      skipped = "";
    } else {
      skipped += "|";
    }
    if (!currency.isInUse()) {
      buf.append(skipped).append("|1");
      skipped = "";
    } else {
      skipped += "|";
    }
    if (currency.getRounding() != 0) {
      buf.append(skipped).append("|").append(currency.getRounding());
    }
    return buf.toString();
  }

  /**
   * Get a map for a given class/locale combination.
   * 
   * @param category
   * @param locale
   * 
   * @return map for the specified class/locale
   */
  private Map<String, String> getMap(String category, GwtLocale locale) {
    MapKey mapKey = new MapKey(category, locale);
    Map<String, String> map = maps.get(mapKey);
    if (map == null) {
      map = new HashMap<String, String>();
      maps.put(mapKey, map);
    }
    return map;
  }

  /**
   * @return an array of map keys, ordered from most derived to least derived.
   */
  private MapKey[] getSortedMapKeys() {
    Set<MapKey> keySet = maps.keySet();
    MapKey[] keys = keySet.toArray(new MapKey[keySet.size()]);
    Arrays.sort(keys, new Comparator<MapKey>() {
      private final Comparator<GwtLocale> depthComparator = new LocaleComparator();

      @Override
      public int compare(MapKey a, MapKey b) {
        return depthComparator.compare(a.getLocale(), b.getLocale());
      }
    });
    return keys;
  }
}
