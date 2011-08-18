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

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

/**
 * Eponymous test class.
 */
public class IntPairAttributeParserTest extends TestCase {
  private IntPairAttributeParser parser;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MortalLogger logger = MortalLogger.NULL;

    CompilationState state = CompilationStateBuilder.buildFrom(
        logger.getTreeLogger(), UiJavaResources.getUiResources());
    TypeOracle types = state.getTypeOracle();

    FieldReferenceConverter converter = new FieldReferenceConverter(null);
    IntAttributeParser intParser = new IntAttributeParser(converter,
        types.parse("int"), logger);

    parser = new IntPairAttributeParser(intParser, logger);
  }

  public void testGood() throws UnableToCompleteException {
    assertEquals("1, 1", parser.parse(null, "1, 1"));
    assertEquals("123, 456", parser.parse(null, "123, 456"));
    assertEquals("(int)able.baker(), (int)charlie.delta()",
        parser.parse(null, "{able.baker}, {charlie.delta}"));
    assertEquals("0001, 0002", parser.parse(null, "0001, 0002"));
  }

  public void testBad() {
    try {
      parser.parse(null, "fnord");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    try {
      parser.parse(null, "1, 2, 3");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    try {
      parser.parse(null, "1");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    try {
      parser.parse(null, "1.2, 3.4");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }
}
