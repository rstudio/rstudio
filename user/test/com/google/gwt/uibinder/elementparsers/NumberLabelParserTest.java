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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.user.client.ui.NumberLabel;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * Eponymous unit test.
 */
public class NumberLabelParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.NumberLabel";

  private static final MockJavaResource NUMBERLABEL_SUBCLASS_NO_CONSTRUCTOR = new MockJavaResource(
      "my.MyNumberLabel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.NumberLabel;\n");
      code.append("public class MyNumberLabel extends NumberLabel {\n");
      code.append("}\n");
      return code;
    }
  };
  private static final MockJavaResource NUMBERLABEL_SUBCLASS_FORMAT_CONSTRUCTOR = new MockJavaResource(
      "my.MyConstructedNumberLabel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.NumberLabel;\n");
      code.append("import com.google.gwt.i18n.client.NumberFormat;\n");
      code.append("public class MyConstructedNumberLabel extends NumberLabel {\n");
      code.append("  public MyConstructedNumberLabel(NumberFormat f) { super(f); }");
      code.append("}\n");
      return code;
    }
  };
  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new NumberLabelParser());
  }

  public void testHappyWithDefaultInstantiableSubclass()
      throws UnableToCompleteException, SAXException {
    tester = new ElementParserTester("my.MyNumberLabel",
        new NumberLabelParser(), NUMBERLABEL_SUBCLASS_NO_CONSTRUCTOR);
    NumberLabelParser parser = new NumberLabelParser();
    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyNumberLabel format='{someDateTimeFormat}' currencyData='{someCurrencyData}' /> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyNumberLabel"), "fieldName",
        tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithSubclassWithNumberFormatConstructor()
      throws UnableToCompleteException, SAXException {
    NumberLabelParser parser = new NumberLabelParser();
    tester = new ElementParserTester("my.MyConstructedNumberLabel",
        new NumberLabelParser(), NUMBERLABEL_SUBCLASS_FORMAT_CONSTRUCTOR);

    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyConstructedNumberLabel format='{someNumberFormat}' /> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyConstructedNumberLabel"),
        "fieldName", tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertEquals("new my.MyConstructedNumberLabel(someNumberFormat)",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithNoFormat() throws UnableToCompleteException,
      SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel>");
    b.append("</g:NumberLabel>");

    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithCustomFormatAndCurrency()
      throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel customFormat='#\u00A0\u00A4' currencyCode='EUR'>");
    b.append("</g:NumberLabel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + NumberLabel.class.getCanonicalName() + "("
        + NumberFormat.class.getCanonicalName() + ".getFormat(\"#\u00A0\u00A4\", \"EUR\"))",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithCurrencyPredefinedFormat()
      throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel predefinedFormat='CURRENCY'>");
    b.append("</g:NumberLabel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + NumberLabel.class.getCanonicalName() + "("
        + NumberFormat.class.getCanonicalName() + ".getCurrencyFormat())",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithCurrencyPredefinedFormatAndCurrencCode()
      throws UnableToCompleteException, SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel predefinedFormat='CURRENCY' currencyCode='EUR'>");
    b.append("</g:NumberLabel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + NumberLabel.class.getCanonicalName() + "("
        + NumberFormat.class.getCanonicalName() + ".getCurrencyFormat(\"EUR\"))",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testChokeOnNonDateTimeFormat() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel format='someString' >");
    b.append("</g:NumberLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about NumberFormat",
          tester.logger.died.contains("NumberFormat"));
    }
  }

  public void testChokeOnNonCurrencyData() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel predefinedFormat='CURRENCY' currencyData='someString' >");
    b.append("</g:NumberLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about CurrencyData",
          tester.logger.died.contains("CurrencyData"));
    }
  }

  public void testChokeOnUnknownPredefinedFormat() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel predefinedFormat='someString' >");
    b.append("</g:NumberLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(tester.logger.died, tester.logger.died.contains(String.format(
          NumberLabelParser.UNKNOWN_PREDEFINED_FORMAT, "someString")));
    }
  }

  public void testChokeOnMultipleFormats() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel predefinedFormat='SCIENTIFIC' customFormat='#'>");
    b.append("</g:NumberLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(
          tester.logger.died,
          tester.logger.died.contains(NumberLabelParser.AT_MOST_ONE_SPECIFIED_FORMAT));
    }
  }

  public void testChokeOnMultipleCurrencies() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel predefinedFormat='CURRENCY' currencyData='{someCurrencyData}' currencyCode='EUR'>");
    b.append("</g:NumberLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(
          tester.logger.died,
          tester.logger.died.contains(NumberLabelParser.AT_MOST_ONE_SPECIFIED_CURRENCY));
    }
  }

  public void testChokeOnCurrencyWithoutSpecifiedFormat() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel currencyCode='EUR'>");
    b.append("</g:NumberLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(
          tester.logger.died,
          tester.logger.died.contains(NumberLabelParser.NO_CURRENCY_WITHOUT_SPECIFIED_FORMAT));
    }
  }

  public void testChokeOnCurrencyWithPredefinedFormat() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:NumberLabel predefinedFormat='PERCENT' currencyCode='EUR'>");
    b.append("</g:NumberLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(
          tester.logger.died,
          tester.logger.died.contains(NumberLabelParser.NO_CURRENCY_WITH_PREDEFINED_FORMAT));
    }
  }
}
