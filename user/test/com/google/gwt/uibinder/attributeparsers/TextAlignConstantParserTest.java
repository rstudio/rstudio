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
package com.google.gwt.uibinder.attributeparsers;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.test.UiJavaResources;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.TextBoxBase.TextAlignConstant;

import junit.framework.TestCase;

/**
 * Test for {@link TextAlignConstantParser}.
 */
public class TextAlignConstantParserTest extends TestCase {
  private static final String TBB = TextBoxBase.class.getCanonicalName();
  private static final String TAC = TextAlignConstant.class.getCanonicalName();
  private TextAlignConstantParser parser;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    CompilationState state = CompilationStateBuilder.buildFrom(TreeLogger.NULL,
        UiJavaResources.getUiResources());
    TypeOracle types = state.getTypeOracle();
    parser = new TextAlignConstantParser(new FieldReferenceConverter(null),
        types.parse(TAC), MortalLogger.NULL);
  }

  public void testFriendlyNames() throws UnableToCompleteException {
    assertEquals(TBB + ".ALIGN_LEFT", parser.parse("left"));
    assertEquals(TBB + ".ALIGN_CENTER", parser.parse("center"));
    assertEquals(TBB + ".ALIGN_RIGHT", parser.parse("right"));
    assertEquals(TBB + ".ALIGN_JUSTIFY", parser.parse("justify"));
    // capitalized
    assertEquals(TBB + ".ALIGN_LEFT", parser.parse("Left"));
    assertEquals(TBB + ".ALIGN_CENTER", parser.parse("Center"));
    assertEquals(TBB + ".ALIGN_RIGHT", parser.parse("Right"));
    assertEquals(TBB + ".ALIGN_JUSTIFY", parser.parse("Justify"));
  }

  public void testUglyNames() throws UnableToCompleteException {
    assertEquals(TBB + ".ALIGN_LEFT", parser.parse("ALIGN_LEFT"));
    assertEquals(TBB + ".ALIGN_CENTER", parser.parse("ALIGN_CENTER"));
    assertEquals(TBB + ".ALIGN_RIGHT", parser.parse("ALIGN_RIGHT"));
    assertEquals(TBB + ".ALIGN_JUSTIFY", parser.parse("ALIGN_JUSTIFY"));
  }

  public void testBad() {
    try {
      parser.parse("fnord");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }

  public void testFieldRef() throws UnableToCompleteException {
    assertEquals("foo.bar().baz()", parser.parse("{foo.bar.baz}"));
  }
}
