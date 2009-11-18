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
package com.google.gwt.uibinder.attributeparsers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Converts css class names to a form safe to use as a Java identifier.
 */
public class CssNameConverter {
  /**
   * Thrown by {@link CssNameConverter#convertSet(Set)} on name collision.
   */
  public static class Failure extends Exception {
    Failure(String message, Object... params) {
      super(String.format(message, params));
    }
  }

  /**
   * @param className a css class name
   * @return the same name in a form safe to use as a Java identifier
   */
  public String convertName(String className) {
    String[] bits = className.split("\\-");
    StringBuilder b = new StringBuilder();
    for (String bit : bits) {
      if (b.length() == 0) {
        b.append(bit);
      } else {
        b.append(bit.substring(0, 1).toUpperCase());
        if (bit.length() > 1) {
          b.append(bit.substring(1));
        }
      }
    }
    String converted = b.toString();
    return converted;
  }

  /**
   * @param classNames css class names to convert
   * @return map of the same class names in a form safe for use as Java
   *         identifiers, with the order of the input set preserved
   * @throws Failure on collisions due to conversions
   */
  public Map<String, String> convertSet(Set<String> classNames) throws Failure {
    Map<String, String> rawToConverted = new LinkedHashMap<String, String>();
    Map<String, String> convertedToRaw = new LinkedHashMap<String, String>();
    for (String className : classNames) {
      String converted = convertName(className);
      String already = convertedToRaw.get(converted);
      if (already != null) {
        throw new Failure("CSS class name collision: \"%s\" and \"%s\"",
            already, className);
      }
      convertedToRaw.put(converted, className);
      rawToConverted.put(className, converted);
    }
    return rawToConverted;
  }

}
