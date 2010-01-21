/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.css.ast.CssUnknownAtRule;

import junit.framework.TestCase;

import java.util.List;

/**
 * Tests how CssResource handles stylesheets with unknown at-rules.
 */
public class UnknownAtRuleTest extends TestCase {
  private static final String COMPLEX = "@complex {\n  with: arbitrary;\n  stuff: inside;\n}";
  private static final String EXTENDED = "@-extended-ident {\n  \n}";
  private static final String SIMPLE = "@simple;";

  public void test() throws UnableToCompleteException {
    CssStylesheet sheet = GenerateCssAst.exec(TreeLogger.NULL,
        getClass().getClassLoader().getResource(
            "com/google/gwt/resources/css/unknownAtRule.css"));

    List<CssNode> nodes = sheet.getNodes();
    assertEquals(3, nodes.size());
    assertEquals(SIMPLE, ((CssUnknownAtRule) nodes.get(0)).getRule());
    assertEquals(COMPLEX, ((CssUnknownAtRule) nodes.get(1)).getRule());
    assertEquals(EXTENDED, ((CssUnknownAtRule) nodes.get(2)).getRule());

    TextOutput out = new DefaultTextOutput(true);
    CssGenerationVisitor v = new CssGenerationVisitor(out);
    v.accept(sheet);

    assertEquals(SIMPLE + COMPLEX + EXTENDED, out.toString());
  }
}
