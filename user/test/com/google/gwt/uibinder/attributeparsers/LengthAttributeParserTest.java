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

import static com.google.gwt.uibinder.attributeparsers.LengthAttributeParser.UNIT;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.test.UiJavaResources;

import junit.framework.TestCase;

/**
 * Tests {@link LengthAttributeParser}.
 */
public class LengthAttributeParserTest extends TestCase {

  private LengthAttributeParser parser;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    MortalLogger logger = MortalLogger.NULL;

    CompilationState state = CompilationStateBuilder.buildFrom(
        logger.getTreeLogger(), UiJavaResources.getUiResources());
    TypeOracle types = state.getTypeOracle();

    FieldReferenceConverter converter = new FieldReferenceConverter(null);
    DoubleAttributeParser doubleParser = new DoubleAttributeParser(converter,
        types.parse("double"), logger);

    JEnumType enumType = types.findType(
        Unit.class.getCanonicalName()).isEnum();
    EnumAttributeParser enumParser = new EnumAttributeParser(converter,
        enumType, logger);
    parser = new LengthAttributeParser(doubleParser, enumParser, logger);
  }

  public void testGood() throws UnableToCompleteException {
    assertEquals(lengthString("0", "PX"), parser.parse("0"));
    assertEquals(lengthString("0", "PT"), parser.parse("0pt"));

    assertEquals(lengthString("1", "PX"), parser.parse("1"));

    assertEquals(lengthString("1", "PX"), parser.parse("1px"));
    assertEquals(lengthString("1", "PCT"), parser.parse("1%"));
    assertEquals(lengthString("1", "CM"), parser.parse("1cm"));
    assertEquals(lengthString("1", "MM"), parser.parse("1mm"));
    assertEquals(lengthString("1", "IN"), parser.parse("1in"));
    assertEquals(lengthString("1", "PC"), parser.parse("1pc"));
    assertEquals(lengthString("1", "PT"), parser.parse("1pt"));
    assertEquals(lengthString("1", "EM"), parser.parse("1em"));
    assertEquals(lengthString("1", "EX"), parser.parse("1ex"));

    assertEquals(lengthString("1", "PX"), parser.parse("1PX"));
    assertEquals(lengthString("1", "PCT"), parser.parse("1PCT"));
    assertEquals(lengthString("1", "CM"), parser.parse("1CM"));
    assertEquals(lengthString("1", "MM"), parser.parse("1MM"));
    assertEquals(lengthString("1", "IN"), parser.parse("1IN"));
    assertEquals(lengthString("1", "PC"), parser.parse("1PC"));
    assertEquals(lengthString("1", "PT"), parser.parse("1PT"));
    assertEquals(lengthString("1", "EM"), parser.parse("1EM"));
    assertEquals(lengthString("1", "EX"), parser.parse("1EX"));

    assertEquals(lengthString("2.5", "EM"), parser.parse("2.5em"));

    assertEquals(lengthString("1", "EM"), parser.parse("1 em"));

    assertEquals("(double)foo.value(), " + UNIT + ".PX",
        parser.parse("{foo.value}px"));
    assertEquals("1, foo.unit()",
        parser.parse("1{foo.unit}"));
    assertEquals("(double)foo.value(), foo.unit()",
        parser.parse("{foo.value}{foo.unit}"));
  }

  public void testBad() {
    // Garbage.
    try {
      parser.parse("fnord");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    // Non-decimal value.
    try {
      parser.parse("xpx");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    // Raw unit, no value.
    try {
      parser.parse("px");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    // 0, but with invalid unit.
    try {
      parser.parse("0foo");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    // Too many braces cases.
    try {
      parser.parse("{{foo.value}px");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }

    try {
      parser.parse("1{{foo.unit}");
      fail("Expected UnableToCompleteException");
    } catch (UnableToCompleteException e) {
      /* pass */
    }
  }

  private String lengthString(String value, String unit) {
    return value + ", " + UNIT + "." + unit;
  }
}
