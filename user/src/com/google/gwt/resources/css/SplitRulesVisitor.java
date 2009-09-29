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
import com.google.gwt.resources.css.ast.CssModVisitor;
import com.google.gwt.resources.css.ast.CssNodeCloner;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssSelector;

/**
 * Splits rules with compound selectors into multiple rules.
 */
public class SplitRulesVisitor extends CssModVisitor {
  @Override
  public void endVisit(CssRule x, Context ctx) {
    if (x.getSelectors().size() == 1) {
      return;
    }

    for (CssSelector sel : x.getSelectors()) {
      CssRule newRule = new CssRule();
      newRule.getSelectors().add(sel);
      newRule.getProperties().addAll(
          CssNodeCloner.clone(CssProperty.class, x.getProperties()));
      ctx.insertBefore(newRule);
    }
    ctx.removeMe();
    return;
  }
}