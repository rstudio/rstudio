/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.uibinder.rebind.MortalLogger;

/**
 * Interprets an attribute's contents as a method call on a resource class (one
 * tied to an xmnls prefix via a "with://" url).
 * @deprecated soon to die, replaced by brace expressions
 */
@Deprecated
public class BundleAttributeParser implements AttributeParser {

  private final JClassType bundleClass;
  private String bundleInstance;
  private boolean isBundleStatic;

  public BundleAttributeParser(JClassType bundleClass, String bundleInstance,
      boolean isBundleStatic) {
    this.bundleClass = bundleClass;
    this.bundleInstance = bundleInstance;
    this.isBundleStatic = isBundleStatic;
  }

  public JClassType bundleClass() {
    return bundleClass;
  }

  public String bundleInstance() {
    return bundleInstance;
  }

  public String fullBundleClassName() {
    return bundleClass.getPackage().getName() + "." + bundleClass.getName();
  }

  public boolean isBundleStatic() {
    return isBundleStatic;
  }

  public String parse(String attribute, MortalLogger ignored) {
    StringBuilder b = new StringBuilder();
    String[] values = attribute.split(" ");
    boolean first = true;
    for (String value : values) {
      if (first) {
        first = false;
      } else {
        b.append(" + \" \" + ");
      }
      b.append(bundleInstance() + "." + parenthesizeDots(value) + "()");
    }
    return b.toString();
  }

  private String parenthesizeDots(String value) {
    return value.replaceAll("\\.", "().");
  }
}
