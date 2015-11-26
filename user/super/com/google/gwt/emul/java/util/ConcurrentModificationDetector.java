/*
 * Copyright 2014 Google Inc.
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
package java.util;

import javaemul.internal.JsUtils;

/**
 * A helper to detect concurrent modifications to collections. This is implemented as a helper
 * utility so that we could remove the checks easily by a flag.
 */
class ConcurrentModificationDetector {

  private static final boolean API_CHECK =
    System.getProperty("jre.checks.api", "ENABLED").equals("ENABLED");

  private static final String MOD_COUNT_PROPERTY = "_gwt_modCount";

  public static void structureChanged(Object map) {
    if (!API_CHECK) {
      return;
    }
    // Ensure that modCount is initialized if it is not already.
    int modCount = JsUtils.getIntProperty(map, MOD_COUNT_PROPERTY) | 0;
    JsUtils.setIntProperty(map, MOD_COUNT_PROPERTY, modCount + 1);
  }

  public static void recordLastKnownStructure(Object host, Iterator<?> iterator) {
    if (!API_CHECK) {
      return;
    }
    int modCount = JsUtils.getIntProperty(host, MOD_COUNT_PROPERTY);
    JsUtils.setIntProperty(iterator, MOD_COUNT_PROPERTY, modCount);
  }

  public static void checkStructuralChange(Object host, Iterator<?> iterator) {
    if (!API_CHECK) {
      return;
    }
    if (JsUtils.getIntProperty(iterator, MOD_COUNT_PROPERTY)
        != JsUtils.getIntProperty(host, MOD_COUNT_PROPERTY)) {
      throw new ConcurrentModificationException();
    }
  }
}
