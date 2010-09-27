/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.TestAnnotatedMessages.Nested;
import com.google.gwt.i18n.client.gen.Colors;
import com.google.gwt.i18n.client.gen.Shapes;
import com.google.gwt.i18n.client.gen.TestMessages;
import com.google.gwt.i18n.client.resolutiontest.Inners;
import com.google.gwt.i18n.client.resolutiontest.Inners.ExtendsInnerInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.HasInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.InnerClass;
import com.google.gwt.i18n.client.resolutiontest.Inners.LocalizableSimpleInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.OuterLoc;
import com.google.gwt.i18n.client.resolutiontest.Inners.SimpleInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.HasInner.IsInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.InnerClass.InnerInner;
import com.google.gwt.i18n.client.resolutiontest.Inners.InnerClass.InnerInnerMessages;
import com.google.gwt.i18n.client.resolutiontest.Inners.InnerClass.LocalizableInnerInner;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Tests Internationalization. Assumes locale is set to piglatin_UK
 */
public class I18NTest extends GWTTestCase {

  /**
   * A test object to verify that when formatting a message with an arbitrary
   * object passed as an argument, that object's toString method is called.
   */
  public static class TestObjectToString {
    @Override
    public String toString() {
      return "TestObjectToString";
    }
  }

  private static final String HTML_TO_SANITIZE =
      "<em>this</em> gets escaped: <script>evil</script>";
  private static final String SANITIZED_HTML =
      "<em>this</em> gets escaped: &lt;script&gt;evil&lt;/script&gt;";
  private static final String STRING_WITH_B_TAG = "<b>text</b>";
  private static final String STRING_WITH_B_TAG_ESCAPED = "&lt;b&gt;text&lt;/b&gt;";

  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest";
  }

  @SuppressWarnings("unchecked") // intentional test of raw map
  public void testAnnotatedConstants() {
    TestAnnotatedConstants c = GWT.create(TestAnnotatedConstants.class);
    assertEquals(14, c.fourteen());
    assertFalse(c.isFalse());
    assertTrue(c.isTrue());
    assertArrayEquals(new String[] {"String array with one string"},
        c.singleString());
    assertArrayEquals(new String[] {"One", "Two", "Three,Comma"},
        c.threeStrings());
    assertEquals("Properties value #s need quoting!", c.propertiesQuoting());
    Map<String, String> stringMap = c.stringMap();
    assertTrue(stringMap.containsKey("key1"));
    assertTrue(stringMap.containsKey("key2"));
    assertEquals("value1", stringMap.get("key1"));
    assertEquals("value2", stringMap.get("key2"));
    assertEquals(2, stringMap.size());
    stringMap = c.rawMap();
    assertTrue(stringMap.containsKey("key1"));
    assertTrue(stringMap.containsKey("key2"));
    assertEquals("value1", stringMap.get("key1"));
    assertEquals("value2", stringMap.get("key2"));
    assertEquals(2, stringMap.size());
    assertEquals("Test me", c.testMe());
    assertEquals(13.7f, c.thirteenPointSeven());
    assertEquals(3.14, c.threePointOneFour());
    assertEquals("Once more, with meaning", c.withMeaning());
  }

  public void testAnnotatedConstantsGenMD5() {
    TestAnnotatedConstantsGenMD5 c = GWT.create(TestAnnotatedConstantsGenMD5.class);
    assertEquals(14, c.fourteen());
    assertFalse(c.isFalse());
    assertTrue(c.isTrue());
    assertArrayEquals(new String[] {"String array with one string"},
        c.singleString());
    assertArrayEquals(new String[] {"One", "Two"}, c.twoStrings());
    Map<String, String> stringMap = c.stringMap();
    assertTrue(stringMap.containsKey("key1"));
    assertTrue(stringMap.containsKey("key2"));
    assertEquals("value1", stringMap.get("key1"));
    assertEquals("value2", stringMap.get("key2"));
    assertEquals(2, stringMap.size());
    assertEquals("Test me", c.testMe());
    assertEquals(13.7f, c.thirteenPointSeven());
    assertEquals(3.14, c.threePointOneFour());
    assertEquals("Once more, with meaning", c.withMeaning());
  }

  public void testAnnotatedMessages() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("Estay emay", m.basicText());
    assertEquals("Oncay oremay, ithway eaningmay", m.withMeaning());
    assertEquals("PL: One argument: one", m.oneArgument("one"));
    assertEquals("PL: One argument (where am I?), which is optional",
        m.optionalArgument("where am I?"));
    assertEquals("Two arguments, second and first, inverted",
        m.invertedArguments("first", "second")); // from default locale
    assertEquals("PL: Don't tell me I can't {quote things in braces}",
        m.quotedText());
    assertEquals("PL: This {0} would be an argument if not quoted",
        m.quotedArg());
    assertEquals("PL: Total is US$11,305.01", m.currencyFormat(11305.01));
    assertEquals("PL: Default number format is 1,017.1",
        m.defaultNumberFormat(1017.1));
    @SuppressWarnings("deprecation")
    Date date = new Date(107, 11, 1, 12, 1, 2);
    assertEquals("PL: It is 12:01 on Saturday, 2007 December 01",
        m.getTimeDate(date));
    assertEquals("PL: 13 widgets", m.pluralWidgetsOther(13));
    assertEquals("Too many widgets to count (150) in pig-latin",
        m.pluralWidgetsOther(150));
    assertEquals("PL: A widget", m.pluralWidgetsOther(1));
    
    assertEquals("PL: A thingy", m.twoParamPlural("thingy", 1));
    assertEquals("PL: 42 thingies", m.twoParamPlural("thingies", 42));
    assertEquals("PL: Tons (249) of thingies", m.twoParamPlural("thingies", 249));
  }

  public void testAnnotatedMessagesAsSafeHtml() {
    // Duplicate of non-SafeHtml tests
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("Estay emay", m.basicTextAsSafeHtml().asString());
    assertEquals("Oncay oremay, ithway eaningmay", m.withMeaningAsSafeHtml().asString());
    assertEquals("PL: One argument: one", m.oneArgumentAsSafeHtml("one").asString());
    assertEquals("PL: One argument (where am I?), which is optional",
        m.optionalArgumentAsSafeHtml("where am I?").asString());
    assertEquals("Two arguments, second and first, inverted",
        m.invertedArgumentsAsSafeHtml("first", "second").asString()); // from default locale
    assertEquals("PL: Don't tell me I can't {quote things in braces}",
        m.quotedTextAsSafeHtml().asString());
    assertEquals("PL: This {0} would be an argument if not quoted",
        m.quotedArgAsSafeHtml().asString());
    assertEquals("PL: Total is US$11,305.01",
        m.currencyFormatAsSafeHtml(11305.01).asString());
    assertEquals("PL: Default number format is 1,017.1",
        m.defaultNumberFormatAsSafeHtml(1017.1).asString());
    @SuppressWarnings("deprecation")
    Date date = new Date(107, 11, 1, 12, 1, 2);
    assertEquals("PL: It is 12:01 on Saturday, 2007 December 01",
        m.getTimeDateAsSafeHtml(date).asString());
    assertEquals("PL: 13 widgets", m.pluralWidgetsOtherAsSafeHtml(13).asString());
    assertEquals("Too many widgets to count (150) in pig-latin",
        m.pluralWidgetsOtherAsSafeHtml(150).asString());

    assertEquals("PL: A widget", m.pluralWidgetsOtherAsSafeHtml(1).asString());

    assertEquals("PL: A thingy",
        m.twoParamPluralAsSafeHtml("thingy", 1).asString());
    assertEquals("PL: 42 thingies",
        m.twoParamPluralAsSafeHtml("thingies", 42).asString());
    assertEquals("PL: Tons (249) of thingies",
        m.twoParamPluralAsSafeHtml("thingies", 249).asString());

    // Additional SafeHtml-specific tests
    assertEquals("PL: One argument: " + STRING_WITH_B_TAG_ESCAPED,
        m.oneArgumentAsSafeHtml(STRING_WITH_B_TAG).asString());
    assertEquals("PL: One argument: " + SANITIZED_HTML,
        m.oneArgumentAsSafeHtml(SimpleHtmlSanitizer.sanitizeHtml(HTML_TO_SANITIZE)).asString());
    assertEquals(
        "Two arguments, " + STRING_WITH_B_TAG_ESCAPED + " and " + SANITIZED_HTML + ", inverted",
        m.invertedArgumentsAsSafeHtml(
            SimpleHtmlSanitizer.sanitizeHtml(HTML_TO_SANITIZE),
            STRING_WITH_B_TAG).asString());

    assertEquals("PL: A " + STRING_WITH_B_TAG_ESCAPED,
        m.twoParamPluralAsSafeHtml(STRING_WITH_B_TAG, 1).asString());
    assertEquals("PL: 42 " + STRING_WITH_B_TAG_ESCAPED,
        m.twoParamPluralAsSafeHtml(STRING_WITH_B_TAG, 42).asString());
    assertEquals("PL: Tons (249) of " + STRING_WITH_B_TAG_ESCAPED,
        m.twoParamPluralAsSafeHtml(STRING_WITH_B_TAG, 249).asString());

    assertEquals("PL: A " + SANITIZED_HTML,
        m.twoParamPluralAsSafeHtml(
            SimpleHtmlSanitizer.sanitizeHtml(HTML_TO_SANITIZE), 1).asString());
    assertEquals("PL: 42 " + SANITIZED_HTML,
        m.twoParamPluralAsSafeHtml(
            SimpleHtmlSanitizer.sanitizeHtml(HTML_TO_SANITIZE), 42).asString());
    assertEquals("PL: Tons (249) of " + SANITIZED_HTML,
        m.twoParamPluralAsSafeHtml(
            SimpleHtmlSanitizer.sanitizeHtml(HTML_TO_SANITIZE), 249).asString());
  }

  public void testAnnotationInheritance() {
    TestAnnotationGrandchild m = GWT.create(TestAnnotationGrandchild.class);
    assertEquals("foo", m.foo());
    assertEquals("bar_piglatin", m.bar());
    assertEquals("baz_piglatin", m.baz());
  }

  public void testAnnotationInheritanceAsSafeHtml() {
    // Duplicate of non-SafeHtml tests
    TestAnnotationGrandchild m = GWT.create(TestAnnotationGrandchild.class);
    assertEquals("foo", m.fooAsSafeHtml().asString());
    assertEquals("bar_piglatin", m.barAsSafeHtml().asString());
    assertEquals("baz_piglatin", m.bazAsSafeHtml().asString());
  }

  public void testBindings() {
    TestBinding b = GWT.create(TestBinding.class);
    assertEquals("default", b.a());
    TestLeafBundle c = GWT.create(TestLeafBundle.class);
    assertEquals("TestLeafBundle_piglatin_UK_WINDOWS", c.b());
    com.google.gwt.i18n.client.Wrapper2.TestBindingImpl d = GWT.create(com.google.gwt.i18n.client.Wrapper2.TestBindingImpl.class);
    assertEquals("default", d.a());
  }

  public void testColors() {
    Colors colors = GWT.create(Colors.class);
    assertNotNull(colors);
    // No piglatin version exists for grey
    assertEquals("ĝréý", colors.grey());
    assertEquals("ackblay", colors.black());
    assertEquals("ueblay", colors.blue());
  }

  public void testColorsAndShapes() {
    ColorsAndShapes s = GWT.create(ColorsAndShapes.class);
    assertEquals("ueblay", s.blue());
    assertEquals("ĝréý", s.grey());
  }

  public void testConstantBooleans() {
    TestConstants types = GWT.create(TestConstants.class);
    assertEquals(false, types.booleanFalse());
    assertEquals(true, types.booleanTrue());
  }

  public void testConstantDoubles() {
    TestConstants types = GWT.create(TestConstants.class);
    double delta = 0.0000001;
    assertEquals(3.14159, types.doublePi(), delta);
    assertEquals(0.0, types.doubleZero(), delta);
    assertEquals(1.0, types.doubleOne(), delta);
    assertEquals(-1.0, types.doubleNegOne(), delta);
    assertEquals(Double.MAX_VALUE, types.doublePosMax(), delta);
    assertEquals(Double.MIN_VALUE, types.doublePosMin(), delta);
    assertEquals(-Double.MAX_VALUE, types.doubleNegMax(), delta);
    assertEquals(-Double.MIN_VALUE, types.doubleNegMin(), delta);
  }

  public void testConstantFloats() {
    TestConstants types = GWT.create(TestConstants.class);
    double delta = 0.0000001;
    assertEquals(3.14159f, types.floatPi(), delta);
    assertEquals(0.0f, types.floatZero(), delta);
    assertEquals(1.0f, types.floatOne(), delta);
    assertEquals(-1.0f, types.floatNegOne(), delta);
    assertEquals(Float.MAX_VALUE, types.floatPosMax(), delta);
    assertEquals(Float.MIN_VALUE, types.floatPosMin(), delta);
    assertEquals(-Float.MAX_VALUE, types.floatNegMax(), delta);
    assertEquals(-Float.MIN_VALUE, types.floatNegMin(), delta);
  }

  /**
   * Exercises ConstantMap more than the other map tests.
   */
  public void testConstantMapABCD() {
    TestConstants types = GWT.create(TestConstants.class);

    Map<String, String> map = types.mapABCD();
    Map<String, String> expectedMap = getMapFromArrayUsingASimpleRule(new String[] {
        "A", "B", "C", "D"});
    assertNull(map.get("bogus"));
    compareMapsComprehensively(map, expectedMap);

    /*
     * Test if the returned map can be modified in any way. Things are working
     * as expected if exceptions are thrown in each case.
     */
    String failureMessage = "Should have thrown UnsupportedOperationException";
    /* test map operations */
    try {
      map.remove("keyA");
      fail(failureMessage + " on map.remove");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.put("keyA", "allA");
      fail(failureMessage + "on map.put of existing key");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.put("keyZ", "allZ");
      fail(failureMessage + "on map.put of new key");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.clear();
      fail(failureMessage + " on map.clear");
    } catch (UnsupportedOperationException e) {
    }

    /* test map.keySet() operations */
    try {
      map.keySet().add("keyZ");
      fail(failureMessage + " on map.keySet().add");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.keySet().remove("keyA");
      fail(failureMessage + " on map.keySet().remove");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.keySet().clear();
      fail(failureMessage + " on map.keySet().clear");
    } catch (UnsupportedOperationException e) {
    }

    /* test map.values() operations */
    try {
      map.values().add("valueZ");
      fail(failureMessage + " on map.values().add");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.values().remove("valueA");
      fail(failureMessage + " on map.values().clear()");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.values().clear();
      fail(failureMessage + " on map.values().clear()");
    } catch (UnsupportedOperationException e) {
    }

    /* test map.entrySet() operations */
    Map.Entry<String, String> firstEntry = map.entrySet().iterator().next();
    try {
      map.entrySet().clear();
      fail(failureMessage + "on map.entrySet().clear");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.entrySet().remove(firstEntry);
      fail(failureMessage + " on map.entrySet().remove");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.entrySet().add(firstEntry);
      fail(failureMessage + "on map.entrySet().add");
    } catch (UnsupportedOperationException e) {
    }
    try {
      firstEntry.setValue("allZ");
      fail(failureMessage + "on firstEntry.setValue");
    } catch (UnsupportedOperationException e) {
    }
    try {
      map.clear();
      fail(failureMessage + " on map.clear");
    } catch (UnsupportedOperationException e) {
    }
  }

  /**
   * Tests exercise the cache.
   */
  public void testConstantMapBACD() {
    TestConstants types = GWT.create(TestConstants.class);
    Map<String, String> map = types.mapBACD();
    Map<String, String> expectedMap = getMapFromArrayUsingASimpleRule(new String[] {
        "B", "A", "C", "D"});
    compareMapsComprehensively(map, expectedMap);
  }

  /**
   * Tests exercise the cache.
   */
  public void testConstantMapBBB() {
    TestConstants types = GWT.create(TestConstants.class);
    Map<String, String> map = types.mapBBB();
    Map<String, String> expectedMap = getMapFromArrayUsingASimpleRule(new String[] {"B"});
    compareMapsComprehensively(map, expectedMap);
  }

  /**
   * Tests exercise the cache and check if Map works as the declared return
   * type.
   */
  @SuppressWarnings("unchecked")
  public void testConstantMapDCBA() {
    TestConstants types = GWT.create(TestConstants.class);
    Map<String, String> map = types.mapDCBA();
    Map<String, String> expectedMap = getMapFromArrayUsingASimpleRule(new String[] {
        "D", "C", "B", "A"});
    compareMapsComprehensively(map, expectedMap);
  }

  /**
   * Tests focus on correctness of entries, since ABCD exercises the map.
   */
  public void testConstantMapEmpty() {
    TestConstants types = GWT.create(TestConstants.class);
    Map<String, String> map = types.mapEmpty();
    Map<String, String> expectedMap = new HashMap<String, String>();
    compareMapsComprehensively(map, expectedMap);
  }

  /**
   * Tests exercise the cache and check if Map works as the declared return
   * type.
   */
  public void testConstantMapXYZ() {
    TestConstants types = GWT.create(TestConstants.class);
    Map<String, String> map = types.mapXYZ();
    Map<String, String> expectedMap = new HashMap<String, String>();
    expectedMap.put("keyX", "valueZ");
    expectedMap.put("keyY", "valueZ");
    expectedMap.put("keyZ", "valueZ");
    compareMapsComprehensively(map, expectedMap);
  }

  public void testConstantStringArrays() {
    TestConstants types = GWT.create(TestConstants.class);
    String[] s;

    s = types.stringArrayABCDEFG();
    assertArrayEquals(new String[] {"A", "B", "C", "D", "E", "F", "G"}, s);

    s = types.stringArraySizeOneEmptyString();
    assertArrayEquals(new String[] {""}, s);

    s = types.stringArraySizeOneX();
    assertArrayEquals(new String[] {"X"}, s);

    s = types.stringArraySizeTwoBothEmpty();
    assertArrayEquals(new String[] {"", ""}, s);

    s = types.stringArraySizeThreeAllEmpty();
    assertArrayEquals(new String[] {"", "", ""}, s);

    s = types.stringArraySizeTwoWithEscapedComma();
    assertArrayEquals(new String[] {"X", ", Y"}, s);

    s = types.stringArraySizeOneWithBackslashX();
    assertArrayEquals(new String[] {"\\X"}, s);

    s = types.stringArraySizeThreeWithDoubleBackslash();
    assertArrayEquals(new String[] {"X", "\\", "Y"}, s);
  }

  public void testConstantStrings() {
    TestConstants types = GWT.create(TestConstants.class);
    assertEquals("string", types.getString());
    assertEquals("stringTrimsLeadingWhitespace",
        types.stringTrimsLeadingWhitespace());
    assertEquals("stringDoesNotTrimTrailingThreeSpaces   ",
        types.stringDoesNotTrimTrailingThreeSpaces());
    assertEquals("", types.stringEmpty());
    String jaBlue = types.stringJapaneseBlue();
    assertEquals("あお", jaBlue);
    String jaGreen = types.stringJapaneseGreen();
    assertEquals("みどり", jaGreen);
    String jaRed = types.stringJapaneseRed();
    assertEquals("あか", jaRed);
  }

  public void testConstantsWithLookup() {
    TestConstantsWithLookup l = GWT.create(TestConstantsWithLookup.class);
    Map<String, String> map = l.getMap("mapABCD");
    assertEquals("valueA", map.get("keyA"));
    map = l.getMap("mapDCBA");
    assertEquals("valueD", map.get("keyD"));
    assertEquals(l.mapABCD(), l.getMap("mapABCD"));
    assertEquals(l.mapDCBA(), l.getMap("mapDCBA"));
    assertEquals(l.mapBACD(), l.getMap("mapBACD"));
    assertEquals(l.getString(), l.getString("getString"));
    assertSame(l.stringArrayABCDEFG(), l.getStringArray("stringArrayABCDEFG"));
    assertEquals(l.booleanFalse(), l.getBoolean("booleanFalse"));
    assertEquals(l.floatPi(), l.getFloat("floatPi"), .001);
    assertEquals(l.doublePi(), l.getDouble("doublePi"), .001);
    try {
      // even though getString has the gwt.key "string", it is not the lookup
      // value
      l.getMap("string");
      fail("Should have thrown MissingResourceException");
    } catch (MissingResourceException e) {
      // success if the exception was caught
    }
  }

  public void testDictionary() {
    createDummyDictionaries();
    Dictionary d = Dictionary.getDictionary("testDic");
    assertEquals("3 {2},{2},{2}, one {0}, two {1} {1}",
        d.get("formattedMessage"));
    assertEquals("4", d.get("d"));
    Set<String> s = d.keySet();
    assertTrue(s.contains("a"));
    assertTrue(s.contains("b"));
    assertFalse(s.contains("c"));
    Collection<String> s2 = d.values();
    assertTrue(s2.contains("A"));
    assertTrue(s2.contains("B"));
    Iterator<String> iter = s2.iterator();
    assertEquals("3 {2},{2},{2}, one {0}, two {1} {1}", iter.next());
    assertEquals(4, s2.size());
    Dictionary empty = Dictionary.getDictionary("emptyDic");
    assertEquals(0, empty.keySet().size());
    boolean threwError = false;
    try {
      Dictionary.getDictionary("malformedDic");
    } catch (MissingResourceException e) {
      threwError = true;
    }
    assertTrue(threwError);
  }

  public void testIntConstant() {
    TestConstants types = GWT.create(TestConstants.class);
    assertEquals(0, types.intZero());
    assertEquals(1, types.intOne());
    assertEquals(-1, types.intNegOne());
    assertEquals(Integer.MAX_VALUE, types.intMax());
    assertEquals(Integer.MIN_VALUE, types.intMin());
  }

  public void testLocalizableInner() {
    // Check simple inner
    LocalizableSimpleInner s = GWT.create(Inners.LocalizableSimpleInner.class);
    assertEquals("getLocalizableInner", s.getLocalizableInner());

    LocalizableInnerInner localizableInnerInner = GWT.create(Inners.InnerClass.LocalizableInnerInner.class);
    assertEquals("localizableInnerInner", localizableInnerInner.string());

    // Check success of finding embedded
    OuterLoc lock = GWT.create(OuterLoc.class);
    assertEquals("piglatin", lock.string());

    assertEquals("InnerLoc", Inners.testInnerLoc());
  }

  public void testLocalizableInterfaceInner() {
    Inners inner = new Inners();

    // Simple Inner
    SimpleInner simpleInner = GWT.create(Inners.SimpleInner.class);
    assertEquals(0, simpleInner.intZero());
    assertEquals("Simple Inner", simpleInner.simpleInner());
    assertTrue(inner.testProtectedInner());

    // Has Inner
    HasInner hasInner = GWT.create(Inners.HasInner.class);
    assertEquals("Has Inner", hasInner.hasInner());
    assertEquals(0, hasInner.floatZero(), .0001);

    // Is Inner
    IsInner isInner = GWT.create(IsInner.class);
    assertEquals(2, isInner.isInner());

    // Inner Inner
    InnerInner innerInner = GWT.create(InnerInner.class);
    assertEquals(4.321, innerInner.innerInner(), .0001);
    assertEquals("outer", innerInner.outer());

    // Inner Inner Message
    InnerInnerMessages innerInnerMessages = GWT.create(InnerInnerMessages.class);
    assertEquals("I am a person",
        innerInnerMessages.innerClassMessages("person"));

    // Extends Inner Inner
    ExtendsInnerInner extendsInnerInner = GWT.create(ExtendsInnerInner.class);
    assertEquals("Extends Inner Inner", extendsInnerInner.extendsInnerInner());

    // Protected InnerClass
    InnerClass innerClass = new Inners.InnerClass();
    Map<String, String> extendsAnotherInner = innerClass.testExtendsAnotherInner();
    assertEquals("4.321", extendsAnotherInner.get("innerInner"));
    assertEquals("outer", extendsAnotherInner.get("outer"));

    // ExtendProtectedInner
    String extendProtectedInner = innerClass.testExtendsProtectedInner();
    assertEquals("Extend Protected Inner", extendProtectedInner);
  }

  public void testNestedAnnotations() {
    Nested m = GWT.create(Nested.class);
    // no translation exists in piglatin for nested dollar
    assertEquals("nested dollar", m.nestedDollar());
    assertEquals("estednay underscoray", m.nestedUnderscore());
  }

  public void testNestedAnnotationsAsSafeHtml() {
    // Duplicate of non-SafeHtml tests
    TestAnnotatedMessages.Nested m = GWT.create(TestAnnotatedMessages.Nested.class);
    // no translation exists in piglatin for nested dollar
    assertEquals("nested dollar", m.nestedDollarAsSafeHtml().asString());
    assertEquals("estednay underscoray", m.nestedUnderscoreAsSafeHtml().asString());
  }

  /**
   * Test that messages works with Number subclasses.
   */
  public void testNumber() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    BigInteger intVal = new BigInteger("1000000000000000000");
    assertEquals("Total is US$1,000,000,000,000,000,000.00",
        m.withNumberCurrency(intVal));
    BigDecimal decVal = new BigDecimal("1000000000000000000.01");
    assertEquals("Total is US$1,000,000,000,000,000,000.01",
        m.withNumberCurrency(decVal));
    assertEquals("Distance is 1.0E18", m.withNumberExponent(intVal));
    assertEquals("Distance is 100.0E6", m.withNumberExponent(1e8f));
  }

  public void testShapesFamily() {
    Shapes shapes = GWT.create(Shapes.class);
    // test overload
    assertEquals("aya irclecay", shapes.circle());
    ColorsAndShapesAndConcepts s = GWT.create(ColorsAndShapesAndConcepts.class);
    assertEquals("aya irclecay", s.circle());
    // test converge
    assertEquals("any primary color", s.shapeColor());
    assertEquals("trees", s.green());
    assertEquals("Îñţérñåţîöñåļîžåţîöñ", s.internationalization());
  }

  public void testSpecialPlurals() {
    TestAnnotatedMessages m = GWT.create(TestAnnotatedMessages.class);
    assertEquals("No widgets", m.specialPlurals(0));
    assertEquals("A widget", m.specialPlurals(1));
    assertEquals("2 widgets", m.specialPlurals(2));
  }

  public void testTestMessages() {
    TestMessages s = GWT.create(TestMessages.class);
    assertEquals("no args", s.args0());
    assertEquals("a,b,c,d,e,f,g,h,i,j", s.args10("a", "b", "c", "d", "e", "f",
        "g", "h", "i", "j"));
    String shouldHave = "x,y, \"a\",\"b\", \"x\", \"y\", \'a\', b, {0}, \'y\'";
    assertEquals(shouldHave, s.argsWithQuotes("x", "y"));
    assertEquals("repeatedArgs: a, b, a, b, a, b, a, b",
        s.testLotsOfUsageOfArgs("a", "b"));
    assertEquals("\"~\" ~~ \"~~~~ \"\"", s.testWithXs());
    assertEquals("お好你好好", s.unicode("好", "好"));
    assertEquals("", s.empty());
    assertEquals("{quoted}", s.quotedBraces());
  }

  public void testTestMessagesAsSafeHtml() {
    // Duplicate of non-SafeHtml tests
    TestMessages m = (TestMessages) GWT.create(TestMessages.class);
    assertEquals("no args", m.args0AsSafeHtml().asString());
    assertEquals("a,b,c,d,e,f,g,h,i,j", m.args10AsSafeHtml("a", "b", "c", "d", "e", "f",
        "g", "h", "i", "j").asString());
    String shouldHave = "x,y, \"a\",\"b\", \"x\", \"y\", \'a\', b, {0}, \'y\'";
    assertEquals(shouldHave, m.argsWithQuotesAsSafeHtml("x", "y").asString());
    assertEquals("repeatedArgs: a, b, a, b, a, b, a, b",
        m.testLotsOfUsageOfArgsAsSafeHtml("a", "b").asString());
    assertEquals("\"~\" ~~ \"~~~~ \"\"", m.testWithXsAsSafeHtml().asString());
    assertEquals("お好你好好", m.unicodeAsSafeHtml("好", "好").asString());
    assertEquals("", m.emptyAsSafeHtml().asString());
    assertEquals("{quoted}", m.quotedBracesAsSafeHtml().asString());
  }

  public void testTypedMessages() {
    TestTypedMessages typed = GWT.create(TestTypedMessages.class);
    String expected = "int(0) float(1.5), long(0), boolean(true), Object([], char(a), byte(127), short(-32768);";
    assertEquals(expected, typed.testAllTypes(0, (float) 1.5, 0, true,
        new ArrayList<String>(), 'a', Byte.MAX_VALUE, Short.MIN_VALUE));
    String lotsOfInts = typed.testLotsOfInts(1, 2, 3, 4);
    assertEquals("1, 2,3,4 ", lotsOfInts);
    String oneFloat = typed.simpleMessageTest((float) 2.25);
    assertEquals("2.25", oneFloat);
    String singleQuotes = typed.testSingleQuotes("arg");
    assertEquals("'A', 'arg', ','", singleQuotes);
    String testSomeObjectTypes = typed.testSomeObjectTypes(
        new TestObjectToString(), new StringBuffer("hello"), new Integer("34"),
        null);
    assertEquals("obj(TestObjectToString), StringBuffer(hello), "
        + "Integer(34), " + "null(null);", testSomeObjectTypes);
  }

  public void testTypedMessagesAsSafeHtml() {
    // Duplicate of non-SafeHtml tests
    TestTypedMessages m = (TestTypedMessages) GWT.create(TestTypedMessages.class);
    String expected = "int(0) float(1.5), long(0), boolean(true), Object([],"
        + " char(a), byte(127), short(-32768);";
    assertEquals(expected, m.testAllTypesAsSafeHtml(0, (float) 1.5, 0, true,
        new ArrayList<String>(), 'a', Byte.MAX_VALUE,
        Short.MIN_VALUE).asString());
    String lotsOfInts = m.testLotsOfIntsAsSafeHtml(1, 2, 3, 4).asString();
    assertEquals("1, 2,3,4 ", lotsOfInts);
    String oneFloat = m.simpleMessageTestAsSafeHtml((float) 2.25).asString();
    assertEquals("2.25", oneFloat);
    String singleQuotes = m.testSingleQuotesAsSafeHtml("arg").asString();
    assertEquals("'A', 'arg', ','", singleQuotes);
    String testSomeObjectTypes = m.testSomeObjectTypesAsSafeHtml(
        new TestObjectToString(), new StringBuffer("hello"), new Integer("34"),
        null).asString();
    assertEquals("obj(TestObjectToString), StringBuffer(hello), "
        + "Integer(34), null(null);",
      testSomeObjectTypes);

    // Additional SafeHtml-specific tests
    Object someObject = new Object() {
      @Override
      public String toString() {
        return STRING_WITH_B_TAG;
      }
    };
    testSomeObjectTypes = m.testSomeObjectTypesAsSafeHtml(
        new TestObjectToString(), new StringBuffer(STRING_WITH_B_TAG),
        new Integer("34"), someObject).asString();
    assertEquals("obj(TestObjectToString), StringBuffer("
        + STRING_WITH_B_TAG_ESCAPED + "), Integer(34), null("
        + STRING_WITH_B_TAG_ESCAPED + ");", testSomeObjectTypes);
  }

  private void assertArrayEquals(String[] shouldBe, String[] test) {
    assertEquals(shouldBe.length, test.length);
    for (int i = 0; i < test.length; i++) {
      assertEquals(shouldBe[i], test[i]);
    }
  }

  private <T> boolean compare(Collection<T> collection1,
      Collection<T> collection2) {
    if (collection1 == null) {
      return (collection2 == null);
    }
    if (collection2 == null) {
      return false;
    }
    if (collection1.size() != collection2.size()) {
      return false;
    }
    for (T element1 : collection1) {
      boolean found = false;
      for (T element2 : collection2) {
        if (element1.equals(element2)) {
          found = true;
          break;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }

  // compare the map, entrySet, keySet, and values
  private void compareMapsComprehensively(Map<String, String> map,
      Map<String, String> expectedMap) {
    // checking both directions to verify that the equals implementation is
    // correct both ways
    assertEquals(expectedMap, map);
    assertEquals(map, expectedMap);
    assertEquals(expectedMap.entrySet(), map.entrySet());
    assertEquals(map.entrySet(), expectedMap.entrySet());
    assertEquals(expectedMap.keySet(), map.keySet());
    assertEquals(map.keySet(), expectedMap.keySet());
    assertTrue(compare(expectedMap.values(), map.values()));
    assertTrue(compare(map.values(), expectedMap.values()));
  }

  private native void createDummyDictionaries() /*-{
    $wnd.testDic = new Object();
    $wnd.testDic.formattedMessage = "3 {2},{2},{2}, one {0}, two {1} {1}";
    $wnd.testDic.a="A";
    $wnd.testDic.b="B";
    $wnd.testDic.d=4;
    $wnd.emptyDic = new Object();
    $wnd.malformedDic = 4;
  }-*/;

  private Map<String, String> getMapFromArrayUsingASimpleRule(String array[]) {
    Map<String, String> map = new HashMap<String, String>();
    for (String str : array) {
      map.put("key" + str, "value" + str);
    }
    return map;
  }
}
