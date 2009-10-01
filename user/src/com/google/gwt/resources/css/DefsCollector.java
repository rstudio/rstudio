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
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssVisitor;

import java.util.HashSet;
import java.util.Set;

/**
 * Collects the names of all user-defined {@literal @def} constants in the
 * stylesheet.
 */
public class DefsCollector extends CssVisitor {
  private final Set<String> defs = new HashSet<String>();

  @Override
  public void endVisit(CssDef x, Context ctx) {
    defs.add(x.getKey());
  }

  public Set<String> getDefs() {
    return defs;
  }
}