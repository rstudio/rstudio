/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.resources.rg;

import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * Evaluates the obfuscation style the user selected and formats the obfuscated
 * name accordingly.
 */
public enum CssObfuscationStyle {
  VERBOSE (true, false, true, true),
  STABLE_FULL_CLASSNAME (true, true, true, true),
  STABLE_SHORT_CLASSNAME (true, true, true, false),
  STABLE_NO_CLASSNAME (true, true, false, false),
  OBFUSCATED (false, false, false, false);

  static CssObfuscationStyle getObfuscationStyle(String name) {
    if (name.equalsIgnoreCase("pretty")) {
      return VERBOSE;
    } else if (name.equalsIgnoreCase("stable")) {
      return STABLE_FULL_CLASSNAME;
    } else if (name.equalsIgnoreCase("stable-shorttype")) {
      return STABLE_SHORT_CLASSNAME;
    } else if (name.equalsIgnoreCase("stable-notype")) {
      return STABLE_NO_CLASSNAME;
    }
    return OBFUSCATED;
  }

  private boolean isPretty;
  private boolean isStable;
  private boolean showClassName;
  private boolean showPackageName;
  
  CssObfuscationStyle(boolean isPretty, boolean isStable, boolean showClassName,
      boolean showPackageName) {
    this.isPretty = isPretty;
    this.isStable = isStable;
    this.showClassName = showClassName;
    this.showPackageName = showPackageName;
  }

  public String getPrettyName(String method, JClassType type, String obfuscatedName) {
    if (!isPretty()) {
      return obfuscatedName;
    }
    String toReturn = method;
    
    /* 
     * Note that by dropping the type, or using it's short name, you are 
     * allowing name collisions in the css selector names. These options should
     * only be used if you are sure that your GWT application is ensuring that 
     * there are no namespace collisions.
     */
    if (showClassName) {
      if (showPackageName) {
        toReturn = type.getQualifiedSourceName().replaceAll("[.$]", "-") + "-" + toReturn;
      } else {
        toReturn = type.getName() + "-" + toReturn;
      }
    } 
    
    /*
     * For stable styles the obfuscated class name is dropped from the pretty
     * output. This results in class names that are constant, no matter how 
     * many other selectors are added.
     */
    if (!isStable) {
      toReturn = obfuscatedName += "-" + toReturn; 
    }
    return toReturn;
  }

  public boolean isPretty() {
    return isPretty;
  }
}
