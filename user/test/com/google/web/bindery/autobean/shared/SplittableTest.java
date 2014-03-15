/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.autobean.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.autobean.gwt.client.impl.JsoSplittable;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.Coder;
import com.google.web.bindery.autobean.shared.impl.AutoBeanCodexImpl.EncodeState;
import com.google.web.bindery.autobean.shared.impl.SplittableList;
import com.google.web.bindery.autobean.shared.impl.SplittableSimpleMap;
import com.google.web.bindery.autobean.shared.impl.StringQuoter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Tests for the underlying Splittable implementation. This test class is not
 * indicative of code that users would write, it's simply doing spot-checks of
 * functionality that AbstractAutoBean depends on.
 */
public class SplittableTest extends GWTTestCase {

  /**
   *
   */
  private static final EncodeState testState = EncodeState.forTesting();

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.autobean.AutoBean";
  }

  public void testBasicProperties() {
    Splittable data = StringQuoter.split("{\"a\":true, \"b\":3, \"c\":\"string\", \"d\":null}");
    assertTrue("isBoolean", data.get("a").isBoolean());
    assertTrue("asBoolean", data.get("a").asBoolean());
    assertTrue("isNumber", data.get("b").isNumber());
    assertEquals(3.0, data.get("b").asNumber());
    assertTrue("isString", data.get("c").isString());
    assertEquals("string", data.get("c").asString());
    assertTrue("isNull", data.isNull("d"));
    assertNull("should be null", data.get("d"));
  }

  /**
   * Ensure that hashcodes don't leak into the payload.
   */
  public void testHashCode() {
    Splittable data = StringQuoter.split("{\"a\":\"b\"}");
    int hash = data.hashCode();
    String payload = data.getPayload();
    assertFalse(payload, payload.contains("$H"));
    assertFalse(payload, payload.contains(String.valueOf(hash)));
    assertEquals(hash, data.hashCode());
  }

  /**
   * Splittables are implemented by a couple of different concrete types. We'll
   * use this method to make sure that the correct implementation type is being
   * used in various circumstances.
   */
  public void testImplementationChoice() {
    Splittable s = StringQuoter.split("[1,false,\"true\"]");
    if (GWT.isScript()) {
      assertTrue("s should be JsoSplittable", s instanceof JsoSplittable);
      assertTrue("s[0] should be JsoSplittable", s.get(0) instanceof JsoSplittable);
      assertTrue("s[1] should be JsoSplittable", s.get(1) instanceof JsoSplittable);
      assertTrue("s[2] should be JsoSplittable", s.get(2) instanceof JsoSplittable);
    } else {
      // Using the same types in both pure-JRE and DevMode to avoid JSNI
      // overhead
      assertTrue("s should be JsonSplittable", s.getClass().getName().endsWith("JsonSplittable"));
      assertTrue("s[0] should be JsonSplittable", s.get(0).getClass().getName().endsWith(
          "JsonSplittable"));
      assertTrue("s[1] should be JsonSplittable", s.get(1).getClass().getName().endsWith(
          "JsonSplittable"));
      assertTrue("s[2] should be JsonSplittable", s.get(2).getClass().getName().endsWith(
          "JsonSplittable"));
    }
  }

  public void testIndexed() {
    Splittable s = StringQuoter.createIndexed();
    assertTrue(s.isIndexed());
    assertFalse(s.isKeyed());
    assertFalse(s.isString());
    assertEquals(0, s.size());

    string("foo").assign(s, 0);
    string("bar").assign(s, 1);
    string("baz").assign(s, 2);

    assertEquals(3, s.size());
    assertEquals("[\"foo\",\"bar\",\"baz\"]", s.getPayload());

    string("quux").assign(s, 1);
    assertEquals("[\"foo\",\"quux\",\"baz\"]", s.getPayload());

    Splittable s2 = s.deepCopy();
    assertNotSame(s, s2);
    assertEquals(s.size(), s2.size());
    for (int i = 0, j = s.size(); i < j; i++) {
      assertEquals(s.get(i).asString(), s2.get(i).asString());
    }

    s.setSize(2);
    assertEquals(2, s.size());
    assertEquals("[\"foo\",\"quux\"]", s.getPayload());

    // Make sure reified values aren't in the payload
    Object o = new Object();
    s.setReified("reified", o);
    assertFalse(s.getPayload().contains("reified"));
    assertFalse(s.getPayload().contains("__s"));
    assertSame(o, s.getReified("reified"));
  }

  public void testKeyed() {
    Splittable s = StringQuoter.createSplittable();
    assertFalse(s.isIndexed());
    assertTrue(s.isKeyed());
    assertFalse(s.isString());
    assertTrue(s.getPropertyKeys().isEmpty());

    string("bar").assign(s, "foo");
    string("quux").assign(s, "baz");

    // Actual iteration order is undefined
    assertEquals(new HashSet<String>(Arrays.asList("foo", "baz")), new HashSet<String>(s
        .getPropertyKeys()));

    assertFalse(s.isNull("foo"));
    assertTrue(s.isNull("bar"));

    assertEquals("bar", s.get("foo").asString());
    assertEquals("quux", s.get("baz").asString());

    String payload = s.getPayload();
    assertTrue(payload.startsWith("{"));
    assertTrue(payload.endsWith("}"));
    assertTrue(payload.contains("\"foo\":\"bar\""));
    assertTrue(payload.contains("\"baz\":\"quux\""));

    Splittable s2 = s.deepCopy();
    assertNotSame(s, s2);
    assertEquals("bar", s2.get("foo").asString());
    assertEquals("quux", s2.get("baz").asString());

    // Make sure reified values aren't in the payload
    Object o = new Object();
    s.setReified("reified", o);
    assertFalse("Should not see reified in " + s.getPayload(), s.getPayload().contains("reified"));
    assertFalse("Should not see __s in " + s.getPayload(), s.getPayload().contains("__s"));
    assertSame(o, s.getReified("reified"));
  }

  public void testNested() {
    Splittable s = StringQuoter.split("{\"a\":{\"foo\":\"bar\"}}");
    Splittable a = s.get("a");
    assertNotNull(a);
    assertEquals("bar", a.get("foo").asString());
    assertSame(a, s.get("a"));
    assertEquals(a, s.get("a"));
  }

  /**
   * Tests attributes of the {@link Splittable#NULL} field.
   */
  public void testNull() {
    Splittable n = Splittable.NULL;
    if (GWT.isScript()) {
      assertNull(n);
    } else {
      assertNotNull(n);
    }
    assertFalse("boolean", n.isBoolean());
    assertFalse("indexed", n.isIndexed());
    assertFalse("keyed", n.isKeyed());
    assertFalse("string", n.isString());
    assertEquals("null", n.getPayload());
  }

  /**
   * Extra tests in here due to potential to confuse {@code false} and
   * {@code null} values.
   */
  public void testSplittableListBoolean() {
    Coder boolCoder = AutoBeanCodexImpl.valueCoder(Boolean.class);
    Splittable s = StringQuoter.createIndexed();
    bool(false).assign(s, 0);
    assertFalse("0 should not be null", s.isNull(0));
    assertTrue("s[0] should be a boolean", s.get(0).isBoolean());
    assertFalse("s[0] should be false", s.get(0).asBoolean());
    assertNotNull("Null decode", ValueCodex.decode(Boolean.class, s.get(0)));
    Object decodedBoolean = boolCoder.decode(testState, s.get(0));
    assertNotNull("decode should not return null", decodedBoolean);
    assertFalse("decoded value should be false", (Boolean) decodedBoolean);

    bool(true).assign(s, 1);
    assertTrue("s[1] should be a boolean", s.get(1).isBoolean());
    assertTrue("s[1] should be true", s.get(1).asBoolean());
    assertTrue("boolCoder 1", (Boolean) boolCoder.decode(testState, s.get(1)));

    Splittable.NULL.assign(s, 2);
    assertTrue("3 should be null", s.isNull(3));
    assertEquals("payload", "[false,true,null]", s.getPayload());
    List<Boolean> boolList = new SplittableList<Boolean>(s, boolCoder, testState);
    assertEquals("boolList", Arrays.<Boolean> asList(false, true, null), boolList);
  }

  /**
   * Extra tests in here due to potential to confuse 0 and {@code null} values.
   */
  public void testSplittableListNumbers() {
    Coder intCoder = AutoBeanCodexImpl.valueCoder(Integer.class);
    Coder doubleCoder = AutoBeanCodexImpl.valueCoder(Double.class);
    Splittable s = StringQuoter.createIndexed();
    number(0).assign(s, 0);
    assertFalse("0 should not be null", s.isNull(0));
    assertTrue("s[0] should be a number", s.get(0).isNumber());
    assertNotNull("Null decode", ValueCodex.decode(Integer.class, s.get(0)));
    Object decodedInt = intCoder.decode(testState, s.get(0));
    assertNotNull("decode should not return null", decodedInt);
    assertEquals("intCoder 0", Integer.valueOf(0), decodedInt);
    assertEquals("doubleCoder 0", Double.valueOf(0), doubleCoder.decode(testState, s.get(0)));

    number(3.141592).assign(s, 1);
    assertEquals("intCoder 1", Integer.valueOf(3), intCoder.decode(testState, s.get(1)));
    assertEquals("doubleCoder 1", Double.valueOf(3.141592), doubleCoder.decode(testState, s.get(1)));

    number(42).assign(s, 2);
    Splittable.NULL.assign(s, 3);
    assertTrue("3 should be null", s.isNull(3));
    assertEquals("payload", "[0,3.141592,42,null]", s.getPayload());
    List<Double> doubleList = new SplittableList<Double>(s, doubleCoder, testState);
    assertEquals(Double.valueOf(0), doubleList.get(0));
    assertEquals("doubleList", Arrays.<Double> asList(0d, 3.141592, 42d, null), doubleList);

    // Don't share backing data between lists
    s = StringQuoter.split("[0,3.141592,42,null]");
    List<Integer> intList = new SplittableList<Integer>(s, intCoder, testState);
    assertEquals("intList", Arrays.<Integer> asList(0, 3, 42, null), intList);
  }

  public void testSplittableListString() {
    Splittable data = StringQuoter.split("[\"Hello\",\"World\"]");
    SplittableList<String> list =
        new SplittableList<String>(data, AutoBeanCodexImpl.valueCoder(String.class), testState);
    assertEquals(2, list.size());
    assertEquals(Arrays.asList("Hello", "World"), list);
    list.set(0, "Goodbye");
    assertEquals(Arrays.asList("Goodbye", "World"), list);
    assertEquals("[\"Goodbye\",\"World\"]", data.getPayload());
    list.remove(0);
    assertEquals(Arrays.asList("World"), list);
    assertEquals("[\"World\"]", data.getPayload());
    list.add("Wide");
    list.add("Web");
    assertEquals(Arrays.asList("World", "Wide", "Web"), list);
    assertEquals("[\"World\",\"Wide\",\"Web\"]", data.getPayload());

    assertEquals(data.getPayload(), normalize(data).getPayload());
  }

  public void testSplittableMapStringString() {
    Splittable data = StringQuoter.split("{\"foo\":\"bar\",\"baz\":\"quux\",\"isNull\":null}");
    assertTrue("isNull should be null", data.isNull("isNull"));
    assertFalse("isNull should not be undefined", data.isUndefined("isNull"));
    Map<String, String> map =
        new SplittableSimpleMap<String, String>(data, AutoBeanCodexImpl.valueCoder(String.class),
            AutoBeanCodexImpl.valueCoder(String.class), testState);
    assertEquals(3, map.size());
    assertEquals("bar", map.get("foo"));
    assertEquals("quux", map.get("baz"));
    assertTrue("Map should have isNull key", map.containsKey("isNull"));
    assertNull(map.get("isNull"));
    assertFalse("Map should not have unknown key", map.containsKey("unknown"));

    map.put("bar", "foo2");
    assertEquals("foo2", map.get("bar"));

    assertEquals(data.getPayload(), normalize(data).getPayload());
  }

  public void testString() {
    Splittable s = string("Hello '\" World!");
    assertFalse(s.isIndexed());
    assertFalse(s.isKeyed());
    assertTrue(s.isString());
    assertEquals("Hello '\" World!", s.asString());
    assertEquals(s.getPayload(), normalize(s).getPayload());
  }

  public void testStringEmpty() {
    Splittable s = string("");
    assertFalse(s.isIndexed());
    assertFalse(s.isKeyed());
    assertTrue(s.isString());
    assertEquals("", s.asString());
    assertEquals("\"\"", s.getPayload());
  }

  private Splittable bool(boolean value) {
    return StringQuoter.split(String.valueOf(value));
  }

  private Splittable number(double number) {
    return StringQuoter.split(String.valueOf(number));
  }

  private Splittable string(String value) {
    return StringQuoter.split(StringQuoter.quote(value));
  }

  private Splittable normalize(Splittable splittable) {
    return StringQuoter.split(splittable.getPayload());
  }
}
