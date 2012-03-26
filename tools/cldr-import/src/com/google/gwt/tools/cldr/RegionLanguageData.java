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

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Encapsulates region/language literacy data from CLDR.
 */
public class RegionLanguageData {

  /**
   * Stores the populate literate in a given language.
   */
  public static class LanguagePopulation implements Comparable<LanguagePopulation> {
    private final String language;
    private final double literatePopulation;
    private final boolean official;

    public LanguagePopulation(String language, double literatePopulation, boolean official) {
      this.language = language;
      this.literatePopulation = literatePopulation;
      this.official = official;
    }

    @Override
    public int compareTo(LanguagePopulation other) {
      int c = -Double.compare(literatePopulation, other.literatePopulation);
      if (c == 0) {
        c = language.compareTo(other.language);
      }
      return c;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      LanguagePopulation other = (LanguagePopulation) obj;
      return language.equals(other.language)
          && (Double.compare(literatePopulation, other.literatePopulation) == 0);
    }

    public String getLanguage() {
      return language;
    }

    public double getLiteratePopulation() {
      return literatePopulation;
    }

    @Override
    public int hashCode() {
      long temp = Double.doubleToLongBits(literatePopulation);
      return language.hashCode() + 31 * (int) (temp ^ (temp >>> 32));
    }

    public boolean isOfficial() {
      return official;
    }

    @Override
    public String toString() {
      return "[lang=" + language + " pop=" + literatePopulation + "]";
    }
  }

  /**
   * Stores the region populations literate in a given language.
   */
  public static class RegionPopulation implements Comparable<RegionPopulation> {
    private final String region;
    private final double literatePopulation;
    private final boolean official;

    public RegionPopulation(String region, double literatePopulation, boolean official) {
      this.region = region;
      this.literatePopulation = literatePopulation;
      this.official = official;
    }

    @Override
    public int compareTo(RegionPopulation other) {
      int c = -Double.compare(literatePopulation, other.literatePopulation);
      if (c == 0) {
        c = region.compareTo(other.region);
      }
      return c;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      RegionPopulation other = (RegionPopulation) obj;
      return region.equals(other.region)
          && (Double.compare(literatePopulation, other.literatePopulation) == 0);
    }

    public double getLiteratePopulation() {
      return literatePopulation;
    }

    public String getRegion() {
      return region;
    }

    @Override
    public int hashCode() {
      long temp = Double.doubleToLongBits(literatePopulation);
      return region.hashCode() + 31 * (int) (temp ^ (temp >>> 32));
    }

    public boolean isOfficial() {
      return official;
    }

    @Override
    public String toString() {
      return "[region=" + region + " pop=" + literatePopulation + "]";
    }
  }

  private final Factory cldrFactory;

  private Map<String, SortedSet<LanguagePopulation>> regionMap;
  private Map<String, SortedSet<RegionPopulation>> languageMap;

  public RegionLanguageData(Factory cldrFactory) {
    this.cldrFactory = cldrFactory;
  }

  public Set<LanguagePopulation> getLanguages(String region) {
    ensureMaps();
    Set<LanguagePopulation> languageSet = regionMap.get(region);
    if (languageSet == null) {
      languageSet = Collections.emptySet();
    }
    return languageSet;
  }

  public Set<RegionPopulation> getRegions(String language) {
    ensureMaps();
    Set<RegionPopulation> regionSet = languageMap.get(language);
    if (regionSet == null) {
      regionSet = Collections.emptySet();
    }
    return regionSet;
  }

  private void ensureMaps() {
    if (regionMap != null) {
      return;
    }
    regionMap = new HashMap<String, SortedSet<LanguagePopulation>>();
    languageMap = new HashMap<String, SortedSet<RegionPopulation>>();
    CLDRFile supp = cldrFactory.getSupplementalData();
    XPathParts parts = new XPathParts();
    Iterator<String> iterator = supp.iterator("//supplementalData/territoryInfo/territory");
    while (iterator.hasNext()) {
      String path = iterator.next();
      parts.set(supp.getFullXPath(path));
      String language = parts.findAttributeValue("languagePopulation", "type");
      if (language == null) {
        continue;
      }
      String territory = parts.findAttributeValue("territory", "type");
      String literacyPercentStr = parts.findAttributeValue("territory", "literacyPercent");
      String populationStr = parts.findAttributeValue("territory", "population");
      String populationPercentStr =
          parts.findAttributeValue("languagePopulation", "populationPercent");
      String statusStr = parts.findAttributeValue("languagePopulation", "officialStatus");
      double literacyPercent = Double.parseDouble(literacyPercentStr) * .01;
      double population = Double.parseDouble(populationStr);
      double populationPercent = Double.parseDouble(populationPercentStr) * .01;
      double literatePopulation = population * populationPercent * literacyPercent;
      boolean official = "official".equals(statusStr);
      SortedSet<RegionPopulation> regPop = languageMap.get(language);
      if (regPop == null) {
        regPop = new TreeSet<RegionPopulation>();
        languageMap.put(language, regPop);
      }
      regPop.add(new RegionPopulation(territory, literatePopulation, official));
      SortedSet<LanguagePopulation> langPop = regionMap.get(territory);
      if (langPop == null) {
        langPop = new TreeSet<LanguagePopulation>();
        regionMap.put(territory, langPop);
      }
      langPop.add(new LanguagePopulation(language, literatePopulation, official));
    }
  }
}
