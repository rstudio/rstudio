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
package com.google.gwt.resources.css;

import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssExternalSelectors;
import com.google.gwt.resources.css.ast.CssSelector;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.css.ast.CssVisitor;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Collect all CSS class names in a stylesheet.
 */
public class ExtractClassNamesVisitor extends CssVisitor {
  /**
   * Extract all CSS class names in the provided stylesheet.
   */
  public static Set<String> exec(CssStylesheet sheet) {
    ExtractClassNamesVisitor v = new ExtractClassNamesVisitor();
    v.accept(sheet);
    return v.found;
  }

  private final Set<String> found = new HashSet<String>();

  /**
   * Package-protected for testing.
   */
  ExtractClassNamesVisitor() {
  }

  @Override
  public void endVisit(CssExternalSelectors x, Context ctx) {
    found.addAll(x.getClasses());
  }

  @Override
  public void endVisit(CssSelector x, Context ctx) {
    Matcher m = CssSelector.CLASS_SELECTOR_PATTERN.matcher(x.getSelector());
    while (m.find()) {
      found.add(m.group(1));
    }
  }

  /**
   * Package-protected for testing.
   *
   * @return
   */
  Set<String> getFoundClasses() {
    return found;
  }
}
