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

import junit.framework.TestCase;

/**
 * Test parsing integer attributes.
 */
public class IntAttributeParserTest extends TestCase {
  private IntAttributeParser parser;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    CompilationState state = CompilationStateBuilder.buildFrom(TreeLogger.NULL,
        UiJavaResources.getUiResources());
    TypeOracle types = state.getTypeOracle();
    parser = new IntAttributeParser(new FieldReferenceConverter(null),
        types.parse("int"), MortalLogger.NULL);
  }

  public void testGood() throws UnableToCompleteException {
    assertEquals("1234", parser.parse(null, "1234"));
    assertEquals("-4321", parser.parse(null, "-4321"));
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
    assertEquals("(int)foo.bar().baz()", parser.parse(null, "{foo.bar.baz}"));
  }
}
