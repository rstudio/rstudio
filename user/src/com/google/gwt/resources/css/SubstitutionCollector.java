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
import com.google.gwt.resources.css.ast.CssEval;
import com.google.gwt.resources.css.ast.CssUrl;
import com.google.gwt.resources.css.ast.CssVisitor;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects all user-defined constant nodes in the stylesheet.
 */
public class SubstitutionCollector extends CssVisitor {
  private final Map<String, CssDef> substitutions = new HashMap<String, CssDef>();

  @Override
  public void endVisit(CssDef x, Context ctx) {
    substitutions.put(x.getKey(), x);
  }

  @Override
  public void endVisit(CssEval x, Context ctx) {
    substitutions.put(x.getKey(), x);
  }

  @Override
  public void endVisit(CssUrl x, Context ctx) {
    substitutions.put(x.getKey(), x);
  }

  public Map<String, CssDef> getSubstitutions() {
    return substitutions;
  }
}