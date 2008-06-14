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
import com.google.gwt.i18n.client.gen.Colors;
import com.google.gwt.i18n.client.gen.Shapes;
import com.google.gwt.i18n.client.gen.TestMessages;
import com.google.gwt.i18n.client.impl.ConstantMap;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Tests Internationalization. Assumes locale is set to piglatin_UK
 */
public class I18NTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest";
  }

  public void testLocalizableInner() {
    // Check simple inner
    LocalizableSimpleInner s = (LocalizableSimpleInner) GWT.create(Inners.LocalizableSimpleInner.class);
    assertEquals("getLocalizableInner", s.getLocalizableInner());

    LocalizableInnerInner localizableInnerInner = (LocalizableInnerInner) GWT.create(Inners.InnerClass.LocalizableInnerInner.class);
    assertEquals("localizableInnerInner", localizableInnerInner.string());

    // Check success of finding embedded
    OuterLoc lock = (OuterLoc) GWT.create(OuterLoc.class);
    assertEquals("piglatin", lock.string());

    assertEquals("InnerLoc", Inners.testInnerLoc());
  }

  public void testLocalizableInterfaceInner() {
    Inners inner = new Inners();

    // Simple Inner
    SimpleInner simpleInner = (SimpleInner) GWT.create(Inners.SimpleInner.class);
    assertEquals(0, simpleInner.intZero());
    assertEquals("Simple Inner", simpleInner.simpleInner());
    assertTrue(inner.testProtectedInner());

    // Has Inner
    HasInner hasInner = (HasInner) GWT.create(Inners.HasInner.class);
    assertEquals("Has Inner", hasInner.hasInner());
    assertEquals(0, hasInner.floatZero(), .0001);

    // Is Inner
    IsInner isInner = (IsInner) GWT.create(IsInner.class);
    assertEquals(2, isInner.isInner());

    // Inner Inner
    InnerInner innerInner = (InnerInner) GWT.create(InnerInner.class);
    assertEquals(4.321, innerInner.innerInner(), .0001);
    assertEquals("outer", innerInner.outer());

    // Inner Inner Message
    InnerInnerMessages innerInnerMessages = (InnerInnerMessages) GWT.create(InnerInnerMessages.class);
    assertEquals("I am a person",
        innerInnerMessages.innerClassMessages("person"));

    // Extends Inner Inner
    ExtendsInnerInner extendsInnerInner = (ExtendsInnerInner) GWT.create(ExtendsInnerInner.class);
    assertEquals("Extends Inner Inner", extendsInnerInner.extendsInnerInner());

    // Protected InnerClass
    InnerClass innerClass = new Inners.InnerClass();
    String extendsAnotherInner = innerClass.testExtendsAnotherInner();
    assertEquals("{innerInner=4.321, outer=outer}", extendsAnotherInner);

    // ExtendProtectedInner
    String extendProtectedInner = innerClass.testExtendsProtectedInner();
    assertEquals("Extend Protected Inner", extendProtectedInner);
  }

  public void testAnnotatedConstants() {
    TestAnnotatedConstants c = GWT.create(TestAnnotatedConstants.class);
    assertEquals(14, c.fourteen());
    assertFalse(c.isFalse());
    assertTrue(c.isTrue());
    assertArrayEquals(new String[] {"String array with one string"}, c.singleString());
    assertArrayEquals(new String[] {"One", "Two", "Three,Comma"}, c.threeStrings());
    assertEquals("Properties value #s need quoting!", c.propertiesQuoting());
    Map<String,String> stringMap = c.stringMap();
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
    assertArrayEquals(new String[] {"String array with one string"}, c.singleString());
    assertArrayEquals(new String[] {"One", "Two"}, c.twoStrings());
    Map<String,String> stringMap = c.stringMap();
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
    assertEquals("PL: Don't tell me I can't {quote things in braces}", m.quotedText());
    assertEquals("PL: This {0} would be an argument if not quoted", m.quotedArg());
    assertEquals("PL: Total is US$11,305.01", m.currencyFormat(11305.01));
    assertEquals("PL: Default number format is 1,017.1", m.defaultNumberFormat(1017.1));
    assertEquals("PL: It is 12:01 PM on Saturday, December 1, 2007",
        m.getTimeDate(new Date(107, 11, 1, 12, 1, 2)));
    assertEquals("PL: 13 widgets", m.pluralWidgetsOther(13));
    assertEquals("Too many widgets to count (150) in pig-latin", m.pluralWidgetsOther(150));
  }
  
  public void testBindings() {
    TestBinding b = (TestBinding) GWT.create(TestBinding.class);
    assertEquals("default", b.a());
    TestLeafBundle c = (TestLeafBundle) GWT.create(TestLeafBundle.class);
    assertEquals("TestLeafBundle_piglatin_UK_win", c.b());
    com.google.gwt.i18n.client.Wrapper2.TestBindingImpl d = (com.google.gwt.i18n.client.Wrapper2.TestBindingImpl) GWT.create(com.google.gwt.i18n.client.Wrapper2.TestBindingImpl.class);
    assertEquals("default", d.a());
  }

  public void testColors() {
    Colors colors = (Colors) GWT.create(Colors.class);
    assertNotNull(colors);
    // No piglatin version exists for grey
    assertEquals("ĝréý", colors.grey());
    assertEquals("ackblay", colors.black());
  }

  public void testConstantBooleans() {
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);
    assertEquals(false, types.booleanFalse());
    assertEquals(true, types.booleanTrue());
  }

  public void testConstantDoubles() {
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);
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
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);
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
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);

    Map<String, String> map = types.mapABCD();
    assertEquals(4, map.size());
    assertEquals("valueA", map.get("keyA"));
    assertEquals("valueB", map.get("keyB"));
    assertEquals("valueC", map.get("keyC"));
    assertEquals("valueD", map.get("keyD"));

    assertNull(map.get("bogus"));

    Set<String> keys = map.keySet();
    Iterator<String> keyIter = keys.iterator();
    assertEquals("keyA", keyIter.next());
    assertEquals("keyB", keyIter.next());
    assertEquals("keyC", keyIter.next());
    assertEquals("keyD", keyIter.next());
    assertFalse(keyIter.hasNext());

    Collection<String> values = map.values();
    Iterator<String> valueIter = values.iterator();
    assertEquals("valueA", valueIter.next());
    assertEquals("valueB", valueIter.next());
    assertEquals("valueC", valueIter.next());
    assertEquals("valueD", valueIter.next());
    assertFalse(keyIter.hasNext());

    try {
      map.remove("keyA");
      fail("Should have thrown UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // good if an exception was caught
    }

    try {
      keys.clear();
      fail("Should have thrown UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // good if an exception was caught
    }

    // TODO: fixme -- values are supposed to be backed by the map and should
    // fail if modified
    // try {
    // Iterator nonmutableIter = keys.iterator();
    // nonmutableIter.next();
    // nonmutableIter.remove();
    // fail("Should have thrown UnsupportedOperationException");
    // } catch (UnsupportedOperationException e) {
    // // good if an exception was caught
    // }
  }

  /**
   * Tests focus on just the key order, since ABCD exercises the map.
   */
  public void testConstantMapBACD() {
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);

    ConstantMap map = (ConstantMap) types.mapBACD();

    Set<String> keys = map.keySet();
    Iterator<String> keyIter = keys.iterator();
    assertEquals("keyB", keyIter.next());
    assertEquals("keyA", keyIter.next());
    assertEquals("keyC", keyIter.next());
    assertEquals("keyD", keyIter.next());

    Collection<String> values = map.values();
    Iterator<String> valueIter = values.iterator();
    assertEquals("valueB", valueIter.next());
    assertEquals("valueA", valueIter.next());
    assertEquals("valueC", valueIter.next());
    assertEquals("valueD", valueIter.next());
  }

  /**
   * Tests focus on correctness of entries, since ABCD exercises the map.
   */
  public void testConstantMapBBB() {
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);

    ConstantMap map = (ConstantMap) types.mapBBB();

    assertEquals(1, map.size());

    Set<String> keys = map.keySet();
    assertEquals(1, keys.size());
    Iterator<String> keyIter = keys.iterator();
    assertEquals("keyB", keyIter.next());

    Collection<String> values = map.values();
    assertEquals(1, values.size());
    Iterator<String> valueIter = values.iterator();
    assertEquals("valueB", valueIter.next());
  }

  /**
   * Tests focus on just the key order, since ABCD exercises the map.
   */
  public void testConstantMapDBCA() {
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);

    ConstantMap map = (ConstantMap) types.mapDCBA();

    Set<String> keys = map.keySet();
    Iterator<String> keyIter = keys.iterator();
    assertEquals("keyD", keyIter.next());
    assertEquals("keyC", keyIter.next());
    assertEquals("keyB", keyIter.next());
    assertEquals("keyA", keyIter.next());

    Collection<String> values = map.values();
    Iterator<String> valueIter = values.iterator();
    assertEquals("valueD", valueIter.next());
    assertEquals("valueC", valueIter.next());
    assertEquals("valueB", valueIter.next());
    assertEquals("valueA", valueIter.next());
  }

  /**
   * Tests focus on correctness of entries, since ABCD exercises the map.
   */
  public void testConstantMapXYZ() {
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);

    ConstantMap map = (ConstantMap) types.mapXYZ();

    assertEquals(3, map.size());

    Set<String> keys = map.keySet();
    assertEquals(3, keys.size());
    Iterator<String> keyIter = keys.iterator();
    assertEquals("keyX", keyIter.next());
    assertEquals("keyY", keyIter.next());
    assertEquals("keyZ", keyIter.next());

    Collection<String> values = map.values();
    assertEquals(3, values.size());
    Iterator<String> valueIter = values.iterator();
    assertEquals("valueZ", valueIter.next());
    assertEquals("valueZ", valueIter.next());
    assertEquals("valueZ", valueIter.next());

    Set<Map.Entry<String, String>> entries = map.entrySet();
    assertEquals(3, entries.size());
    Iterator<Map.Entry<String, String>> entryIter = entries.iterator();
    Map.Entry<String, String> entry;

    entry = entryIter.next();
    assertEquals("keyX", entry.getKey());
    assertEquals("valueZ", entry.getValue());
    entry = entryIter.next();
    assertEquals("keyY", entry.getKey());
    assertEquals("valueZ", entry.getValue());
    entry = entryIter.next();
    assertEquals("keyZ", entry.getKey());
    assertEquals("valueZ", entry.getValue());
  }

  public void testConstantStringArrays() {
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);
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
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);
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
    TestConstantsWithLookup l = (TestConstantsWithLookup) GWT.create(TestConstantsWithLookup.class);
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

  // Uncomment for desk tests
  // /**
  // * Tests focus on correctness of entries, since ABCD exercises the map.
  // */
  // public void testConstantMapEmpty() {
  // TestConstants types = (TestConstants) GWT.create(TestConstants.class);
  //
  // ConstantMap map = (ConstantMap) types.mapEmpty();
  //
  // assertEquals(0, map.size());
  //
  // Set keys = map.keySet();
  // assertEquals(0, keys.size());
  // Iterator keyIter = keys.iterator();
  // assertFalse(keyIter.hasNext());
  //
  // Collection values = map.values();
  // assertEquals(0, values.size());
  // Iterator valueIter = values.iterator();
  // assertFalse(valueIter.hasNext());
  // }

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
    TestConstants types = (TestConstants) GWT.create(TestConstants.class);
    assertEquals(0, types.intZero());
    assertEquals(1, types.intOne());
    assertEquals(-1, types.intNegOne());
    assertEquals(Integer.MAX_VALUE, types.intMax());
    assertEquals(Integer.MIN_VALUE, types.intMin());
  }

  public void testShapesFamily() {
    Shapes shapes = (Shapes) GWT.create(Shapes.class);
    // test overload
    assertEquals("aya irclecay", shapes.circle());
    ColorsAndShapesAndConcepts s = (ColorsAndShapesAndConcepts) GWT.create(ColorsAndShapesAndConcepts.class);
    assertEquals("aya irclecay", s.circle());
    // test converge
    assertEquals("any primary color", s.shapeColor());
    assertEquals("trees", s.green());
  }

  public void testTestMessages() {
    TestMessages s = (TestMessages) GWT.create(TestMessages.class);
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

  public void testTypedMessages() {
    TestTypedMessages typed = (TestTypedMessages) GWT.create(TestTypedMessages.class);
    String expected = "int(0) float(1.2), long(0), boolean(true), Object([], char(a), byte(127), short(-32768);";
    assertEquals(expected, typed.testAllTypes(0, (float) 1.2, 0, true, new ArrayList<String>(),
        'a', Byte.MAX_VALUE, Short.MIN_VALUE));
    String lotsOfInts = typed.testLotsOfInts(1, 2, 3, 4);
    assertEquals("1, 2,3,4 ", lotsOfInts);
    String oneFloat = typed.simpleMessageTest((float) 2.3);
    assertEquals("2.3", oneFloat);
    String singleQuotes = typed.testSingleQuotes("arg");
    assertEquals("'A', 'arg', ','", singleQuotes);
    String testSomeObjectTypes = typed.testSomeObjectTypes(new I18NTest(),
        new StringBuffer("hello"), new Integer("34"), null);
    assertEquals(
        "this(null(com.google.gwt.i18n.client.I18NTest)), StringBuffer(hello), Integer(34), "
        + "null(null);", testSomeObjectTypes);
  }

  private void assertArrayEquals(String[] shouldBe, String[] test) {
    assertEquals(shouldBe.length, test.length);
    for (int i = 0; i < test.length; i++) {
      assertEquals(shouldBe[i], test[i]);
    }
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
}
