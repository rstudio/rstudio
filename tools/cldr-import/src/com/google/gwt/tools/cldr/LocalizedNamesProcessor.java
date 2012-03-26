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
import com.google.gwt.tools.cldr.RegionLanguageData.RegionPopulation;

import org.unicode.cldr.util.Factory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Extract localized names from CLDR data.
 */
public class LocalizedNamesProcessor extends Processor {

  private static class IndexedName implements Comparable<IndexedName> {

    private final int index;
    private final CollationKey key;

    public IndexedName(Collator collator, int index, String value) {
      this.index = index;
      this.key = collator.getCollationKey(value);
    }

    @Override
    public int compareTo(IndexedName o) {
      return key.compareTo(o.key);
    }

    /**
     * @return index of this name.
     */
    public int getIndex() {
      return index;
    }
  }

  /**
   * Split a list of region codes into an array.
   * 
   * @param regionList comma-separated list of region codes
   * @return array of region codes, null if none
   */
  private static String[] getRegionOrder(String regionList) {
    String[] split = null;
    if (regionList != null && regionList.length() > 0) {
      split = regionList.split(",");
    }
    return split;
  }

  private final RegionLanguageData regionLanguageData;

  public LocalizedNamesProcessor(File outputDir, Factory cldrFactory, LocaleData localeData) {
    super(outputDir, cldrFactory, localeData);
    regionLanguageData = new RegionLanguageData(cldrFactory);
  }

  @Override
  protected void cleanupData() {
    localeData.copyLocaleData("en", "default", "territory", "languages", "scripts", "variants");
    // Generate a sort order before removing duplicates
    for (GwtLocale locale : localeData.getNonEmptyLocales("territory")) {
      // TODO(jat): deal with language population data that has a script
      Map<String, String> map = localeData.getEntries("territory", locale);
      List<String> countryCodes = new ArrayList<String>();
      for (String regionCode : map.keySet()) {
        // only include real country codes
        if (!"ZZ".equals(regionCode) && regionCode.length() == 2) {
          countryCodes.add(regionCode);
        }
      }
      Locale javaLocale =
          new Locale(locale.getLanguageNotNull(), locale.getRegionNotNull(), locale
              .getVariantNotNull());
      Collator collator = Collator.getInstance(javaLocale);
      IndexedName[] names = new IndexedName[countryCodes.size()];
      for (int i = 0; i < names.length; ++i) {
        names[i] = new IndexedName(collator, i, map.get(countryCodes.get(i)));
      }
      Arrays.sort(names);
      StringBuilder buf = new StringBuilder();
      boolean first = true;
      for (int i = 0; i < names.length; ++i) {
        if (first) {
          first = false;
        } else {
          buf.append(',');
        }
        buf.append(countryCodes.get(names[i].getIndex()));
      }
      localeData.addEntry("territory", locale, "!sortorder", buf.toString());
    }
    Set<String> locales = cldrFactory.getAvailableLanguages();
    for (GwtLocale locale : localeData.getAllLocales()) {
      Set<RegionPopulation> regions = getRegionsForLocale(locale);
      StringBuilder buf = new StringBuilder();
      if (!locale.isDefault()) {
        int count = 0;
        for (RegionPopulation region : regions) {
          // only keep the first 10, and stop if there aren't many speakers
          if (++count > 10 || region.getLiteratePopulation() < 3000000) {
            break;
          }
          if (count > 1) {
            buf.append(',');
          }
          buf.append(region.getRegion());
        }
      }
      localeData.addEntry("territory", locale, "!likelyorder", buf.toString());
    }
    localeData.removeDuplicates("territory");
    localeData.removeDuplicates("language");
    localeData.removeDuplicates("script");
    localeData.removeDuplicates("variant");
  }

  @Override
  protected void loadData() throws IOException {
    System.out.println("Loading data for localized names");
    localeData.addVersions(cldrFactory);
    localeData.addEntries("territory", cldrFactory, "//ldml/localeDisplayNames/territories",
        "territory", "type");
    localeData.addEntries("language", cldrFactory, "//ldml/localeDisplayNames/languages",
        "language", "type");
    localeData.addEntries("script", cldrFactory, "//ldml/localeDisplayNames/scripts", "script",
        "type");
    localeData.addEntries("variant", cldrFactory, "//ldml/localeDisplayNames/variants", "variant",
        "type");
  }

  @Override
  protected void writeOutputFiles() throws IOException {
    for (GwtLocale locale : localeData.getNonEmptyLocales("territory")) {
      Map<String, String> namesMap = localeData.getEntries("territory", locale);
      List<String> regionCodesWithNames = new ArrayList<String>();
      for (String regionCode : namesMap.keySet()) {
        if (!regionCode.startsWith("!")) {
          // skip entries which aren't actually region codes
          regionCodesWithNames.add(regionCode);
        }
      }
      String[] sortOrder = getRegionOrder(namesMap.get("!sortorder"));
      String[] likelyOrder = getRegionOrder(namesMap.get("!likelyorder"));
      if (regionCodesWithNames.isEmpty() && sortOrder == null && likelyOrder == null) {
        // nothing to do
        return;
      }
      // sort for deterministic output
      Collections.sort(regionCodesWithNames);
      if (locale.isDefault()) {
        generateDefaultLocale(locale, namesMap, regionCodesWithNames, sortOrder, likelyOrder);
      }
      generateLocale(locale, namesMap, regionCodesWithNames, sortOrder, likelyOrder);
    }
  }

  /**
   * @param locale
   * @param namesMap
   * @param regionCodesWithNames
   * @param sortOrder
   * @param likelyOrder
   */
  private void generateDefaultLocale(GwtLocale locale, Map<String, String> namesMap,
      List<String> regionCodesWithNames, String[] sortOrder, String[] likelyOrder)
      throws IOException {
    PrintWriter pw = null;
    try {
      pw = createOutputFile("client/DefaultLocalizedNames.java");
      printHeader(pw);
      pw.println("package com.google.gwt.i18n.client;");
      pw.println();
      printVersion(pw, locale, "// ");
      pw.println();
      pw.println("/**");
      pw.println(" * Default LocalizedNames implementation.");
      pw.println(" */");
      pw.print("public class DefaultLocalizedNames extends " + "DefaultLocalizedNamesBase {");
      if (likelyOrder != null) {
        writeStringListMethod(pw, "loadLikelyRegionCodes", likelyOrder);
      }
      pw.println();
      pw.println("  @Override");
      pw.println("  protected void loadNameMap() {");
      pw.println("    super.loadNameMap();");
      for (String code : regionCodesWithNames) {
        String name = namesMap.get(code);
        if (name != null) {
          pw.println("    namesMap.put(\"" + quote(code) + "\", \"" + quote(name) + "\");");
        }
      }
      pw.println("  }");
      if (sortOrder != null) {
        writeStringListMethod(pw, "loadSortedRegionCodes", sortOrder);
      }
      pw.println("}");
    } finally {
      if (pw != null) {
        pw.close();
      }
    }
  }

  /**
   * @param locale
   * @param likelyOrder
   * @param sortOrder
   * @param regionCodesWithNames
   * @param namesMap
   */
  private void generateLocale(GwtLocale locale, Map<String, String> namesMap,
      List<String> regionCodesWithNames, String[] sortOrder, String[] likelyOrder)
      throws IOException {
    PrintWriter pw = null;
    try {
      pw = createFile("LocalizedNamesImpl", "java", locale.getAsString());
      printHeader(pw);
      pw.println("package com.google.gwt.i18n.client.impl.cldr;");
      pw.println();
      if (!regionCodesWithNames.isEmpty()) {
        pw.println("import com.google.gwt.core.client.JavaScriptObject;");
        pw.println();
      }
      printVersion(pw, locale, "// ");
      pw.println();
      pw.println("/**");
      pw.println(" * Localized names for the \"" + locale + "\" locale.");
      pw.println(" */");
      pw.print("public class LocalizedNamesImpl" + localeSuffix(locale) + " extends ");
      if (locale.isDefault()) {
        pw.print("LocalizedNamesImplBase");
      } else {
        pw.print("LocalizedNamesImpl" + localeSuffix(localeData.inheritsFrom(locale)));
      }
      pw.println(" {");
      if (!locale.isDefault()) {
        if (likelyOrder != null) {
          writeStringListMethod(pw, "loadLikelyRegionCodes", likelyOrder);
        }
        if (sortOrder != null) {
          writeStringListMethod(pw, "loadSortedRegionCodes", sortOrder);
        }
        if (!regionCodesWithNames.isEmpty()) {
          pw.println();
          pw.println("  @Override");
          pw.println("  protected void loadNameMapJava() {");
          pw.println("    super.loadNameMapJava();");
          for (String code : regionCodesWithNames) {
            String name = namesMap.get(code);
            if (name != null && !name.equals(code)) {
              pw.println("    namesMap.put(\"" + quote(code) + "\", \"" + quote(name) + "\");");
            }
          }
          pw.println("  }");
          pw.println();
          pw.println("  @Override");
          pw.println("  protected JavaScriptObject loadNameMapNative() {");
          pw.println("    return overrideMap(super.loadNameMapNative(), " + "loadMyNameMap());");
          pw.println("  }");
          pw.println();
          pw.println("  private native JavaScriptObject loadMyNameMap() /*-{");
          generateNativeMap(pw, regionCodesWithNames, namesMap);
          pw.println("  }-*/;");
        }
      } else if (!regionCodesWithNames.isEmpty()) {
        pw.println();
        pw.println("  @Override");
        pw.println("  protected native JavaScriptObject loadNameMapNative() " + "/*-{");
        generateNativeMap(pw, regionCodesWithNames, namesMap);
        pw.println("  }-*/;");
      }
      pw.println("}");
    } finally {
      if (pw != null) {
        pw.close();
      }
    }
  }

  /**
   * @param regionCodesWithNames
   * @param namesMap
   */
  private void generateNativeMap(PrintWriter pw, List<String> regionCodesWithNames,
      Map<String, String> namesMap) {
    pw.println("    return {");
    boolean firstLine = true;
    for (String code : regionCodesWithNames) {
      String name = namesMap.get(code);
      if (name != null && !name.equals(code)) {
        if (firstLine) {
          firstLine = false;
        } else {
          pw.println(",");
        }
        pw.print("        \"" + quote(code) + "\": \"" + quote(name) + "\"");
      }
    }
    pw.println();
    pw.println("    };");
  }

  /**
   * @param locale
   * @return region populations speaking this language
   */
  private Set<RegionPopulation> getRegionsForLocale(GwtLocale locale) {
    Set<RegionPopulation> retVal =
        regionLanguageData
            .getRegions(locale.getLanguageNotNull() + "_" + locale.getScriptNotNull());
    if (retVal.isEmpty()) {
      retVal = regionLanguageData.getRegions(locale.getLanguageNotNull());
    }
    return retVal;
  }

  /**
   * Generate a method which returns an array of string constants.
   * 
   * @param pw PrintWriter to write on
   * @param methodName the name of the method to create
   * @param values the list of string values to return.
   */
  private void writeStringListMethod(PrintWriter pw, String methodName, String[] values) {
    pw.println();
    pw.println("  @Override");
    pw.println("  public String[] " + methodName + "() {");
    pw.println("    return new String[] {");
    for (String code : values) {
      pw.println("        \"" + Processor.quote(code) + "\",");
    }
    pw.println("    };");
    pw.println("  }");
  }
}
