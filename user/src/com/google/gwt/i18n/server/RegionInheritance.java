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
package com.google.gwt.i18n.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generated maps of regions into parent regions, used for locale inheritance.
 * 
 * TODO(jat): make this actually be generated.
 */
public class RegionInheritance {

  private static Map<String, String> parentRegionMap;
  
  static {
    // TODO(jat): add support for multiple parent regions
    parentRegionMap = new HashMap<String, String>();
    // Data from CLDR supplementalData/territoryContainment
    // manually edited to remove multiple parents and non-UN data
    addChildren("001", "002", "009", "019", "142", "150"); // World
    addChildren("011", "BF", "BJ", "CI", "CV", "GH", "GM", "GN", "GW", "LR",
        "ML", "MR", "NE", "NG", "SH", "SL", "SN", "TG"); // Western", "Africa
    addChildren("013", "BZ", "CR", "GT", "HN", "MX", "NI", "PA",
        "SV"); // Central America
    addChildren("014", "BI", "DJ", "ER", "ET", "KE", "KM", "MG", "MU", "MW",
        "MZ", "RE", "RW", "SC", "SO", "TZ", "UG", "YT", "ZM",
        "ZW"); // Eastern Africa
    addChildren("142", "030", "035", "143", "145", "034", "062"); // Asia
    addChildren("143", "TM", "TJ", "KG", "KZ", "UZ"); // Central Asia
    addChildren("145", "AE", "AM", "AZ", "BH", "CY", "GE", "IL", "IQ", "JO",
        "KW", "LB", "OM", "PS", "QA", "SA", "NT", "SY", "TR", "YE",
        "YD"); // Western Asia
    addChildren("015", "DZ", "EG", "EH", "LY", "MA", "SD",
        "TN"); //Northern Africa
    addChildren("150", "039", "151", "154", "155"); // Europe
    addChildren("151", "BG", "BY", "CZ", "HU", "MD", "PL", "RO", "RU", "SU",
        "SK", "UA"); // Eastern Europe
    addChildren("154", "GG", "JE", "AX", "DK", "EE", "FI", "FO", "GB",
        "IE", "IM", "IS", "LT", "LV", "NO", "SE", "SJ"); // Northern Europe
    addChildren("155", "AT", "BE", "CH", "DE", "DD", "FR", "FX", "LI", "LU",
        "MC", "NL"); // Western Europe
    addChildren("017", "AO", "CD", "ZR", "CF", "CG", "CM", "GA", "GQ", "ST",
        "TD"); // Middle Africa
    addChildren("018", "BW", "LS", "NA", "SZ", "ZA"); // Southern Africa
    addChildren("019", "021", "419"); // Americas
    addChildren("002", "011", "014", "015", "017", "018"); // Africa
    addChildren("021", "BM", "CA", "GL", "PM", "US"); // Northern America
    addChildren("029", "AG", "AI", "AN", "AW", "BB", "BL", "BS", "CU", "DM",
        "DO", "GD", "GP", "HT", "JM", "KN", "KY", "LC", "MF", "MQ", "MS", "PR",
        "TC", "TT", "VC", "VG", "VI"); // Caribbean
    addChildren("030", "CN", "HK", "JP", "KP", "KR", "MN", "MO",
        "TW"); // Eastern Asia
    addChildren("035", "BN", "ID", "KH", "LA", "MM", "BU", "MY", "PH", "SG",
        "TH", "TL", "TP", "VN"); // South-Eastern Asia
    addChildren("039", "AD", "AL", "BA", "ES", "GI", "GR", "HR", "IT", "ME",
        "MK", "MT", "CS", "RS", "PT", "SI", "SM", "VA",
        "YU"); // Southern Europe
    addChildren("419", "005", "013", "029"); //Latin America and the Caribbean
    addChildren("005", "AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY",
        "PE", "PY", "SR", "UY", "VE"); // South America
    addChildren("053", "AU", "NF", "NZ"); // Australia and New Zealand
    addChildren("054", "FJ", "NC", "PG", "SB", "VU"); // Melanesia
    addChildren("057", "FM", "GU", "KI", "MH", "MP", "NR", "PW"); // Micronesia
    addChildren("061", "AS", "CK", "NU", "PF", "PN", "TK", "TO", "TV", "WF",
        "WS"); // Polynesia
    addChildren("034", "AF", "BD", "BT", "IN", "IR", "LK", "MV", "NP",
        "PK"); // Southern Asia
    addChildren("009", "053", "054", "057", "061", "QO"); // Oceania
    addChildren("QO", "AQ", "BV", "CC", "CX", "GS", "HM", "IO", "TF",
        "UM"); // Outlying Oceania
  }

  /**
   * Finds the first region which is a common parent of two regions.  If either
   * region is null or if there is no common parent, returns null.  Otherwise,
   * returns the region which contains both regions.
   *
   * @param region1
   * @param region2
   * @return common parent or null if none
   */
  public static String findCommonParent(String region1, String region2) {
    if (region1 == null || region2 == null) {
      return null;
    }
    List<String> parents1 = new ArrayList<String>();
    for (String parent = region1; parent != null;
        parent = parentRegionMap.get(parent)) {
      parents1.add(parent);
    }
    for (String parent = region2; parent != null;
        parent = parentRegionMap.get(parent)) {
      if (parents1.contains(parent)) {
        return parent;
      }
    }
    return null;
  }
  
  /**
   * Returns a set of all ancestors of a given region, including the region
   * itself, in order of inheritance (ie, this region first, top-most region
   * last).
   * 
   * @param child
   * @return list of ancestors
   */
  public static List<String> getAllAncestors(String child) {
    List<String> returnVal = new ArrayList<String>();
    Set<String> nextGroup = new HashSet<String>();
    if (child != null) {
      nextGroup.add(child);
    }
    while (!nextGroup.isEmpty()) {
      Set<String> ancestors = new HashSet<String>();
      for (String region : nextGroup) {
        ancestors.addAll(getImmediateParents(region));
      }
      ancestors.removeAll(returnVal);
      nextGroup.clear();
      nextGroup.addAll(ancestors);
      returnVal.addAll(ancestors);
    }
    return returnVal;
  }

  /**
   * Returns the set of immediate parents of a given region, not including
   * this region.
   *
   * @param region
   * @return set of immediate parents
   */
  public static Set<String> getImmediateParents(String region) {
    Set<String> returnVal = new HashSet<String>();
    if (parentRegionMap.containsKey(region)) {
      returnVal.add(parentRegionMap.get(region));
    }
    return returnVal;
  }

  /**
   * Returns true if parent is equal to the child or is an ancestor.  If both
   * are null, true is returned; otherwise if either is null false is returned.
   * 
   * @param parent
   * @param child
   * @return true if parent is an ancestor of child
   */
  public static boolean isParentOf(String parent, String child) {
    if (parent == child) {
      return true;
    }
    while (child != null) {
      if (child.equals(parent)) {
        return true;
      }
      child = parentRegionMap.get(child);
    }
    return false;
  }

  // @VisibleForTesting
  static Map<String, String> getInheritanceMap() {
    return parentRegionMap;
  }
  
  private static void addChildren(String parent, String... children) {
    for (String child : children) {
      String oldParent = parentRegionMap.put(child, parent);
      assert oldParent == null;
    }
  }
}
