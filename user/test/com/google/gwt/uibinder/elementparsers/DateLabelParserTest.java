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
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.user.client.ui.DateLabel;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

/**
 * Eponymous unit test.
 */
public class DateLabelParserTest extends TestCase {
  private static final String PARSED_TYPE = "com.google.gwt.user.client.ui.DateLabel";

  private static final MockJavaResource DATELABEL_SUBCLASS_NO_CONSTRUCTOR = new MockJavaResource(
      "my.MyDateLabel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.DateLabel;\n");
      code.append("public class MyDateLabel extends DateLabel {\n");
      code.append("}\n");
      return code;
    }
  };
  private static final MockJavaResource DATELABEL_SUBCLASS_FORMAT_CONSTRUCTOR = new MockJavaResource(
      "my.MyConstructedDateLabel") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.DateLabel;\n");
      code.append("import com.google.gwt.i18n.client.DateTimeFormat;\n");
      code.append("public class MyConstructedDateLabel extends DateLabel {\n");
      code.append("  public MyConstructedDateLabel(DateTimeFormat f) { super(f); }");
      code.append("}\n");
      return code;
    }
  };
  private static final MockJavaResource DATELABEL_SUBCLASS_FORMAT_AND_TZ_CONSTRUCTOR = new MockJavaResource(
      "my.MyConstructedDateLabel2") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.DateLabel;\n");
      code.append("import com.google.gwt.i18n.client.DateTimeFormat;\n");
      code.append("import com.google.gwt.i18n.client.TimeZone;\n");
      code.append("public class MyConstructedDateLabel2 extends DateLabel {\n");
      code.append("  public MyConstructedDateLabel2(DateTimeFormat f, TimeZone tz) { super(f, tz); }");
      code.append("}\n");
      return code;
    }
  };
  private static final MockJavaResource DATELABEL_SUBCLASS_TZ_CONSTRUCTOR = new MockJavaResource(
      "my.MyConstructedDateLabel3") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package my;\n");
      code.append("import com.google.gwt.user.client.ui.DateLabel;\n");
      code.append("import com.google.gwt.i18n.client.TimeZone;\n");
      code.append("public class MyConstructedDateLabel3 extends DateLabel {\n");
      code.append("  public MyConstructedDateLabel3(TimeZone tz) { super(); }");
      code.append("}\n");
      return code;
    }
  };
  private ElementParserTester tester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tester = new ElementParserTester(PARSED_TYPE, new DateLabelParser());
  }

  public void testHappyWithDefaultInstantiableSubclass()
      throws UnableToCompleteException, SAXException {
    tester = new ElementParserTester("my.MyDateLabel", new DateLabelParser(),
        DATELABEL_SUBCLASS_NO_CONSTRUCTOR);
    DateLabelParser parser = new DateLabelParser();
    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyDateLabel format='{someDateTimeFormat}' timezone='{someTimeZone}' /> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyDateLabel"), "fieldName",
        tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithSubclassWithDateTimeFormatConstructor()
      throws UnableToCompleteException, SAXException {
    DateLabelParser parser = new DateLabelParser();
    tester = new ElementParserTester("my.MyConstructedDateLabel",
        new DateLabelParser(), DATELABEL_SUBCLASS_FORMAT_CONSTRUCTOR);

    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyConstructedDateLabel format='{someDateTimeFormat}' timezone='{someTimeZone}' /> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyConstructedDateLabel"),
        "fieldName", tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertEquals("new my.MyConstructedDateLabel(someDateTimeFormat)",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithSubclassWithDateTimeFormatAndTimeZoneConstructor()
      throws UnableToCompleteException, SAXException {
    DateLabelParser parser = new DateLabelParser();
    tester = new ElementParserTester("my.MyConstructedDateLabel2",
        new DateLabelParser(), DATELABEL_SUBCLASS_FORMAT_AND_TZ_CONSTRUCTOR);

    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyConstructedDateLabel2 format='{someDateTimeFormat}' timezone='{someTimeZone}' /> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyConstructedDateLabel2"),
        "fieldName", tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertEquals(
        "new my.MyConstructedDateLabel2(someDateTimeFormat, someTimeZone)",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithSubclassWithTimeZoneFormatConstructor()
      throws UnableToCompleteException, SAXException {
    DateLabelParser parser = new DateLabelParser();
    tester = new ElementParserTester("my.MyConstructedDateLabel3",
        new DateLabelParser(), DATELABEL_SUBCLASS_TZ_CONSTRUCTOR);

    StringBuffer b = new StringBuffer();

    b.append("<ui:UiBinder xmlns:ui='" + ElementParserTester.BINDER_URI + "'");
    b.append("    xmlns:my='urn:import:my'");
    b.append("    xmlns:g='urn:import:com.google.gwt.user.client.ui'>");
    b.append("  <my:MyConstructedDateLabel3 format='{someDateTimeFormat}' timezone='{someTimeZone}' /> ");
    b.append("</ui:UiBinder>");

    parser.parse(tester.getElem(b.toString(), "my:MyConstructedDateLabel3"),
        "fieldName", tester.parsedType, tester.writer);
    FieldWriter w = tester.fieldManager.lookup("fieldName");
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithNoFormat() throws UnableToCompleteException,
      SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel>");
    b.append("</g:DateLabel>");

    FieldWriter w = tester.parse(b.toString());
    assertNull(w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithPredefinedFormat() throws UnableToCompleteException,
      SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel predefinedFormat='DATE_MEDIUM'>");
    b.append("</g:DateLabel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + DateLabel.class.getCanonicalName() + "("
        + DateTimeFormat.class.getCanonicalName() + ".getFormat("
        + PredefinedFormat.class.getCanonicalName() + ".DATE_MEDIUM))",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testHappyWithTimezoneOffset() throws UnableToCompleteException,
      SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel customFormat='zzzz' timezoneOffset='-7200'>");
    b.append("</g:DateLabel>");

    FieldWriter w = tester.parse(b.toString());
    assertEquals("new " + DateLabel.class.getCanonicalName() + "("
        + DateTimeFormat.class.getCanonicalName() + ".getFormat(\"zzzz\"), "
        + TimeZone.class.getCanonicalName() + ".createTimeZone(-7200))",
        w.getInitializer());

    assertTrue(tester.writer.statements.isEmpty());
    assertNull(tester.logger.died);
  }

  public void testChokeOnNonDateTimeFormat() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel format='someString' >");
    b.append("</g:DateLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about DateTimeFormat",
          tester.logger.died.contains("DateTimeFormat"));
    }
  }

  public void testChokeOnNonTimeZone() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel format='{someDateTimeFormat}' timezone='someString' >");
    b.append("</g:DateLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about TimeZone",
          tester.logger.died.contains("TimeZone"));
    }
  }

  public void testChokeOnUnknownPredefinedFormat() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel predefinedFormat='someString' >");
    b.append("</g:DateLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue("Expect to hear about PredefinedFormat",
          tester.logger.died.contains("PredefinedFormat"));
    }
  }

  public void testChokeOnMultipleFormats() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel predefinedFormat='DATE_FULL' customFormat='MM/dd'>");
    b.append("</g:DateLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(
          tester.logger.died,
          tester.logger.died.contains(DateLabelParser.AT_MOST_ONE_SPECIFIED_FORMAT));
    }
  }

  public void testChokeOnMultipleTimeZones() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel format='{someDateTimeFormat}' timezone='{someTimeZone}' timezoneOffset='-7200'>");
    b.append("</g:DateLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(
          tester.logger.died,
          tester.logger.died.contains(DateLabelParser.AT_MOST_ONE_SPECIFIED_TIME_ZONE));
    }
  }

  public void testChokeOnTimeZoneWithoutSpecifiedFormat() throws SAXException {
    StringBuffer b = new StringBuffer();
    b.append("<g:DateLabel timezoneOffset='-7200'>");
    b.append("</g:DateLabel>");

    try {
      tester.parse(b.toString());
      fail();
    } catch (UnableToCompleteException e) {
      assertTrue(
          tester.logger.died,
          tester.logger.died.contains(DateLabelParser.NO_TIMEZONE_WITHOUT_SPECIFIED_FORMAT));
    }
  }
}
