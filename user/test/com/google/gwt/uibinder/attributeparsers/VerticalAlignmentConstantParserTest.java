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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.test.UiJavaResources;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import junit.framework.TestCase;

/**
 * Test for {@link VerticalAlignmentConstantParser}.
 */
public class VerticalAlignmentConstantParserTest extends TestCase {
  private static final String HVA = HasVerticalAlignment.class.getCanonicalName();
  private static final String VAC = VerticalAlignmentConstant.class.getCanonicalName();
  private VerticalAlignmentConstantParser parser;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    CompilationState state = CompilationStateBuilder.buildFrom(TreeLogger.NULL,
        UiJavaResources.getUiResources());
    TypeOracle types = state.getTypeOracle();
    parser = new VerticalAlignmentConstantParser(new FieldReferenceConverter(
        null), types.parse(VAC), MortalLogger.NULL);
  }

  public void testFriendlyNames() throws UnableToCompleteException {
    assertEquals(HVA + ".ALIGN_TOP", parser.parse(null, "top"));
    assertEquals(HVA + ".ALIGN_MIDDLE", parser.parse(null, "middle"));
    assertEquals(HVA + ".ALIGN_BOTTOM", parser.parse(null, "bottom"));
    // capitalized
    assertEquals(HVA + ".ALIGN_TOP", parser.parse(null, "Top"));
    assertEquals(HVA + ".ALIGN_MIDDLE", parser.parse(null, "Middle"));
    assertEquals(HVA + ".ALIGN_BOTTOM", parser.parse(null, "Bottom"));
  }

  public void testUglyNames() throws UnableToCompleteException {
    assertEquals(HVA + ".ALIGN_TOP", parser.parse(null, "ALIGN_TOP"));
    assertEquals(HVA + ".ALIGN_MIDDLE", parser.parse(null, "ALIGN_MIDDLE"));
    assertEquals(HVA + ".ALIGN_BOTTOM", parser.parse(null, "ALIGN_BOTTOM"));
  }

  public void testBad() {
    try {
      parser.parse(null, "fnord");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }

  public void testFieldRef() throws UnableToCompleteException {
    assertEquals("foo.bar().baz()", parser.parse(null, "{foo.bar.baz}"));
  }
}
