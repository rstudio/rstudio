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

/**
 * Provides an API for obtaining localized names for a locale.
 */
public interface LocalizedNames {

  /**
   * @return a possibly empty array of region codes, ordered by the literate
   *     population speaking the language of this locale.
   */
  String[] getLikelyRegionCodes();
  
  /**
   * Get the localized name of a given region in this locale.
   * 
   * @param regionCode
   * @return localized name
   */
  String getRegionName(String regionCode);

  /**
   * @return an array of region codes of currently valid countries, ordered
   *         according to the collating order of this locale.
   */
  String[] getSortedRegionCodes();
}
