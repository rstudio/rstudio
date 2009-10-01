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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.resources.rg.CssTestCase;

/**
 * Tests CSS reordering visitors.
 */
public class CssReorderTest extends CssTestCase {
  public void testPropertyMerging() throws UnableToCompleteException {
    test(TreeLogger.NULL, "propertyMerging", false, makeVisitors());
  }

  public void testSelectorMerging() throws UnableToCompleteException {
    test(TreeLogger.NULL, "selectorMerging", false, makeVisitors());
  }

  private CssVisitor[] makeVisitors() {
    return new CssVisitor[] {
        new SplitRulesVisitor(), new AliasDetector(),
        new MergeIdenticalSelectorsVisitor(), new AliasDetector(),
        new MergeRulesByContentVisitor(), new AliasDetector()};
  }
}
