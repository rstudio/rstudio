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
import com.google.gwt.resources.css.ast.CssVisitor;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;

/**
 * Collects all {@code @external} declarations in the stylesheet. This visitor
 * will expand tail-globs.
 */
public class ExternalClassesCollector extends CssVisitor {
  public static final String GLOB_STRING = "*";

  private final SortedSet<String> allClasses = new TreeSet<String>();
  private final SortedSet<String> externalClasses = new TreeSet<String>();
  private final Set<String> globs = new HashSet<String>();

  /**
   * This is a short-circuit for <code>{@literal @external} *</code>.
   */
  private boolean matchAll;

  @Override
  public void endVisit(CssExternalSelectors x, Context ctx) {
    if (matchAll) {
      return;
    }

    for (String selector : x.getClasses()) {
      if (selector.equals(GLOB_STRING)) {
        matchAll = true;
        return;
      } else if (selector.endsWith(GLOB_STRING)) {
        globs.add(selector.substring(0, selector.length() - 1));
      } else {
        externalClasses.add(selector);
      }
    }
  }

  @Override
  public void endVisit(CssSelector x, Context ctx) {
    Matcher m = CssSelector.CLASS_SELECTOR_PATTERN.matcher(x.getSelector());

    while (m.find()) {
      allClasses.add(m.group(1));
    }
  }

  public SortedSet<String> getClasses() {
    if (matchAll) {
      return allClasses;
    }

    glob : for (String glob : globs) {
      for (String clazz : allClasses.tailSet(glob)) {
        if (clazz.startsWith(glob)) {
          externalClasses.add(clazz);
        } else {
          continue glob;
        }
      }
    }

    return externalClasses;
  }
}
