/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.converter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssEval;
import com.google.gwt.resources.css.ast.CssUrl;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Test for {@link DefCollectorVisitor}.
 */
public class DefCollectorVisitorTest extends TestCase {

  private CssEval cssEval;

  private CssDef cssDef;

  private CssUrl cssUrl;

  private TreeLogger treeLogger;

  @Override
  protected void setUp() throws Exception {
    cssEval = mock(CssEval.class);
    cssDef = mock(CssDef.class);
    cssUrl = mock(CssUrl.class);
    treeLogger = mock(TreeLogger.class);
  }

  public void testVisit_CssEval_KeyInMapping() {
    // given
    when(cssEval.getKey()).thenReturn("eval");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(false, treeLogger);
    defCollectorVisitor.visit(cssEval, null);

    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("eval"));
    assertEquals("EVAL", mapping.get("eval"));
  }

  public void testVisit_CssDef_KeyInMapping() {
    // given
    when(cssDef.getKey()).thenReturn("def");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(false, treeLogger);
    defCollectorVisitor.visit(cssDef, null);

    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("def"));
    assertEquals("DEF", mapping.get("def"));
  }

  public void testVisit_CssUrl_KeyInMapping() {
    // given
    when(cssUrl.getKey()).thenReturn("url");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(false, treeLogger);
    defCollectorVisitor.visit(cssUrl, null);

    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("url"));
    assertEquals("URL", mapping.get("url"));
  }

  public void testVisit_ConstantInCamelCase_UpperCaseKeyInMapping() {
    // given
    when(cssDef.getKey()).thenReturn("myConstantName");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(false, treeLogger);
    defCollectorVisitor.visit(cssDef, null);

    // then
    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("myConstantName"));
    assertEquals("MY_CONSTANT_NAME", mapping.get("myConstantName"));
  }

  public void testVisit_ConstantInUpperCase_UpperCaseKeyInMapping() {
    // given
    when(cssDef.getKey()).thenReturn("MY_UPPERCASE_CONSTANT");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(false, treeLogger);
    defCollectorVisitor.visit(cssDef, null);

    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("MY_UPPERCASE_CONSTANT"));
    assertEquals("MY_UPPERCASE_CONSTANT", mapping.get("MY_UPPERCASE_CONSTANT"));
  }

  public void testVisit_ConstantWithInvalidCharacter_InvalidCharacterReplaced() {
    // given
    when(cssDef.getKey()).thenReturn("my-invalid~constant");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(false, treeLogger);
    defCollectorVisitor.visit(cssDef, null);

    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("my-invalid~constant"));
    assertEquals("MY_INVALID_CONSTANT", mapping.get("my-invalid~constant"));
  }

  public void testVisit_TwoConstantsWithSameNameAfterConversionAndNotLenient_ThrowException() {
    // given
    when(cssDef.getKey()).thenReturn("myConstant");
    when(cssUrl.getKey()).thenReturn("my_constant");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(false, treeLogger);
    defCollectorVisitor.visit(cssDef, null);

    try {
      defCollectorVisitor.visit(cssUrl, null);
    } catch (Css2GssConversionException expected) {
    }
  }

  public void testVisit_TwoConstantsWithSameNameAfterConversionAndLenient_SecondConstantRenamed() {
    // given
    when(cssDef.getKey()).thenReturn("myConstant");
    when(cssUrl.getKey()).thenReturn("my_constant");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(true, treeLogger);
    defCollectorVisitor.visit(cssDef, null);
    defCollectorVisitor.visit(cssUrl, null);

    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("myConstant"));
    assertTrue(mapping.containsKey("my_constant"));
    assertEquals("MY_CONSTANT", mapping.get("myConstant"));
    assertEquals("MY_CONSTANT__RENAMED__0", mapping.get("my_constant"));
  }

  public void testVisit_ThreeConstantsWithSameNameAfterConversionAndLenient_SecondAndThirdConstantRenamed() {
    // given
    when(cssDef.getKey()).thenReturn("myConstant");
    when(cssUrl.getKey()).thenReturn("my_constant");
    when(cssEval.getKey()).thenReturn("my-constant");

    // when
    DefCollectorVisitor defCollectorVisitor = new DefCollectorVisitor(true, treeLogger);
    defCollectorVisitor.visit(cssDef, null);
    defCollectorVisitor.visit(cssUrl, null);
    defCollectorVisitor.visit(cssEval, null);

    Map<String, String> mapping = defCollectorVisitor.getDefMapping();

    // then
    assertTrue(mapping.containsKey("myConstant"));
    assertTrue(mapping.containsKey("my_constant"));
    assertTrue(mapping.containsKey("my-constant"));
    assertEquals("MY_CONSTANT", mapping.get("myConstant"));
    assertEquals("MY_CONSTANT__RENAMED__0", mapping.get("my_constant"));
    assertEquals("MY_CONSTANT__RENAMED__1", mapping.get("my-constant"));
  }
}
