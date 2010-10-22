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
package com.google.gwt.i18n.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class of {@link DefaultLocalizedNames}, used just to isolate all
 * hand-written code here from all generated code.
 */
public abstract class DefaultLocalizedNamesBase implements LocalizedNames {

  protected String[] likelyRegionCodes = null;

  protected String[] sortedRegionCodes = null;

  protected Map<String, String> namesMap = null;

  public final String[] getLikelyRegionCodes() {
    if (likelyRegionCodes == null) {
      likelyRegionCodes = loadLikelyRegionCodes();
    }
    return likelyRegionCodes;
  }
  public final String getRegionName(String regionCode) {
    if (needsNameMap()) {
      loadNameMap();
    }
    return getRegionNameImpl(regionCode);
  }

  public final String[] getSortedRegionCodes() {
    if (sortedRegionCodes == null) {
      sortedRegionCodes = loadSortedRegionCodes();
    }
    return sortedRegionCodes;
  }

  protected String getRegionNameImpl(String regionCode) {
    return namesMap.get(regionCode);
  }

  /**
   * Returns a possibly-empty array of country codes, ordered by the literate
   * population speaking this language.
   * 
   * The default implementation is an empty array.
   * 
   * @return a possibly-empty array of likely country codes
   */
  protected String[] loadLikelyRegionCodes() {
    return new String[0];
  }

  /**
   * Initializes {{@link #namesMap} to a map of region code (including
   * non-country codes) to localized names. Subclasses should generally call the
   * parent implementation and then change specific entries, though if most
   * entries are being changed they can just create their own map.
   */
  protected void loadNameMap() {
    namesMap = new HashMap<String, String>();
  }

  /**
   * Returns an array of currently valid country codes ordered by the collating
   * order of the locale.
   * 
   * @return an array of ordered country codes
   */
  protected abstract String[] loadSortedRegionCodes();

  /**
   * @return true if the name map needs to be loaded -- subclasses that provide
   *     alternate storage for the name map (such as in JSOs) should override
   *     this.
   */
  protected boolean needsNameMap() {
    return namesMap == null;
  }
}
