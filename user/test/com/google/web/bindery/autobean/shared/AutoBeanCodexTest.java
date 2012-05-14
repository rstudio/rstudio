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
package com.google.web.bindery.autobean.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;
import com.google.web.bindery.autobean.shared.impl.EnumMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple encoding / decoding tests for the AutoBeanCodex.
 */
public class AutoBeanCodexTest extends GWTTestCase {
  /**
   * Protected so that the JRE-only test can instantiate instances.
   */
  protected interface Factory extends AutoBeanFactory {
    AutoBean<HasSplittable> hasAutoBean();

    AutoBean<HasCycle> hasCycle();

    AutoBean<HasDate> hasDate();

    AutoBean<HasEnum> hasEnum();

    AutoBean<HasList> hasList();

    AutoBean<HasLong> hasLong();

    AutoBean<HasMap> hasMap();

    AutoBean<HasSimple> hasSimple();

    AutoBean<Simple> simple();
  }

  /*
   * These enums are used to verify that a List<Enum> or Map<Enum, Enum> pulls
   * in the necessary metadata.
   */
  enum EnumReachableThroughList {
    FOO_LIST
  }

  enum EnumReachableThroughMapKey {
    FOO_KEY
  }

  enum EnumReachableThroughMapValue {
    FOO_VALUE
  }

  /**
   * Used to test that cycles are detected.
   */
  interface HasCycle {
    List<HasCycle> getCycle();

    void setCycle(List<HasCycle> cycle);
  }

  interface HasDate {
    Date getDate();

    void setDate(Date date);
  }

  interface HasEnum {
    MyEnum getEnum();

    List<MyEnum> getEnums();

    Map<MyEnum, Integer> getMap();

    List<EnumReachableThroughList> getParameterizedList();

    Map<EnumReachableThroughMapKey, EnumReachableThroughMapValue> getParameterizedMap();

    void setEnum(MyEnum value);

    void setEnums(List<MyEnum> value);

    void setMap(Map<MyEnum, Integer> value);
  }

  interface HasList {
    List<Integer> getIntList();

    List<Simple> getList();

    void setIntList(List<Integer> list);

    void setList(List<Simple> list);
  }

  interface HasLong {
    long getLong();

    void setLong(long l);
  }

  interface HasMap {
    Map<Simple, Simple> getComplexMap();

    Map<Map<String, String>, Map<String, String>> getNestedMap();

    Map<String, Simple> getSimpleMap();

    void setComplexMap(Map<Simple, Simple> map);

    void setNestedMap(Map<Map<String, String>, Map<String, String>> map);

    void setSimpleMap(Map<String, Simple> map);
  }

  interface HasSimple {
    Simple getSimple();

    void setSimple(Simple s);
  }

  interface HasSplittable {
    Splittable getSimple();

    List<Splittable> getSimpleList();

    Map<Splittable, Splittable> getSplittableMap();

    Splittable getString();

    void setSimple(Splittable simple);

    void setSimpleList(List<Splittable> simple);

    void setSplittableMap(Map<Splittable, Splittable> map);

    void setString(Splittable s);
  }

  enum MyEnum {
    FOO,
    /**
     * Contains a method that cannot even be called, but is enough to make
     * MyEnum.BAR.getClass().isEnum()==false, because BAR's class is now an
     * anonymous subclass of MyEnum.
     */
    BAR {
      @SuppressWarnings("unused")
      private void dummy() {
      }
    },
    // The eclipse formatter wants to put this annotation inline
    @PropertyName("quux")
    BAZ;
  }

  interface ReachableOnlyFromParameterization extends Simple {
  }

  interface Simple {
    int getInt();

    String getString();

    Boolean hasOtherBoolean();

    boolean isBoolean();

    void setBoolean(boolean b);

    void setInt(int i);

    void setOtherBoolean(Boolean b);

    void setString(String s);
  }

  @SuppressWarnings("deprecation")
  private static final Date[] DATES = {
    new Date(1900, 0, 1), new Date(2012, 3, 10), new Date(2100, 11, 31),
    new Date(-8640000000000000L), // lowest JavaScript Date value
    new Date(8640000000000000L) // highest JavaScript Date value
  };

  protected Factory f;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.autobean.AutoBean";
  }

  public void testCycle() {
    AutoBean<HasCycle> bean = f.hasCycle();
    bean.as().setCycle(Arrays.asList(bean.as()));
    try {
      checkEncode(bean);
      fail("Should not have encoded");
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testDate() {
    for (Date d : DATES) {
      AutoBean<HasDate> bean = f.hasDate();
      bean.as().setDate(d);
      AutoBean<HasDate> decodedBean = checkEncode(bean);
      assertEquals(d, decodedBean.as().getDate());
    }
  }

  /**
   * See issue 6331.
   */
  public void testDecodeDateFromNumericTimestamp() {
    for (Date d : DATES) {
      AutoBean<HasDate> decodedBean =
          AutoBeanCodex.decode(f, HasDate.class, "{\"date\": " + d.getTime() + "}");
      assertEquals(d, decodedBean.as().getDate());
    }
  }

  /**
   * See issue 6636.
   */
  public void testDecodeLongFromNumericValue() {
    final long[] longs = { 42L,
      -9007199254740991L, // lowest integral value that can be represented by a JavaScript Number
      9007199254740991L // highest integral value that van be represented by a JavaScript Number
    };
    for (long l : longs) {
      AutoBean<HasLong> decodedBean = AutoBeanCodex.decode(f, HasLong.class, "{\"long\": " + l + "}");
      assertEquals(l, decodedBean.as().getLong());
    }
  }

  public void testEmptyList() {
    AutoBean<HasList> bean = f.hasList();
    bean.as().setList(Collections.<Simple> emptyList());
    AutoBean<HasList> decodedBean = checkEncode(bean);
    assertNotNull(decodedBean.as().getList());
    assertTrue(decodedBean.as().getList().isEmpty());
  }

  public void testEnum() {
    EnumMap map = (EnumMap) f;
    assertEquals("BAR", map.getToken(MyEnum.BAR));
    assertEquals("quux", map.getToken(MyEnum.BAZ));
    assertEquals(MyEnum.BAR, map.getEnum(MyEnum.class, "BAR"));
    assertEquals(MyEnum.BAZ, map.getEnum(MyEnum.class, "quux"));

    List<MyEnum> arrayValue = Arrays.asList(MyEnum.FOO, MyEnum.BAR, null, MyEnum.BAZ);
    Map<MyEnum, Integer> mapValue = new HashMap<MyEnum, Integer>();
    mapValue.put(MyEnum.FOO, 0);
    mapValue.put(MyEnum.BAR, 1);
    mapValue.put(MyEnum.BAZ, 2);

    AutoBean<HasEnum> bean = f.hasEnum();
    bean.as().setEnum(MyEnum.BAZ);
    bean.as().setEnums(arrayValue);
    bean.as().setMap(mapValue);

    Splittable split = AutoBeanCodex.encode(bean);
    // Make sure the overridden form is always used
    assertFalse(split.getPayload().contains("BAZ"));

    AutoBean<HasEnum> decoded = checkEncode(bean);
    assertEquals(MyEnum.BAZ, decoded.as().getEnum());
    assertEquals(arrayValue, decoded.as().getEnums());
    assertEquals(mapValue, decoded.as().getMap());

    assertEquals(MyEnum.BAZ, AutoBeanUtils.getAllProperties(bean).get("enum"));
    bean.as().setEnum(null);
    assertNull(bean.as().getEnum());
    assertNull(AutoBeanUtils.getAllProperties(bean).get("enum"));
    decoded = checkEncode(bean);
    assertNull(decoded.as().getEnum());
  }

  /**
   * Ensures that enum types that are reachable only through a method
   * parameterization are included in the enum map.
   */
  public void testEnumReachableOnlyThroughParameterization() {
    EnumMap map = (EnumMap) f;
    assertEquals("FOO_LIST", map.getToken(EnumReachableThroughList.FOO_LIST));
    assertEquals("FOO_KEY", map.getToken(EnumReachableThroughMapKey.FOO_KEY));
    assertEquals("FOO_VALUE", map.getToken(EnumReachableThroughMapValue.FOO_VALUE));
    assertEquals(EnumReachableThroughList.FOO_LIST, map.getEnum(EnumReachableThroughList.class,
        "FOO_LIST"));
    assertEquals(EnumReachableThroughMapKey.FOO_KEY, map.getEnum(EnumReachableThroughMapKey.class,
        "FOO_KEY"));
    assertEquals(EnumReachableThroughMapValue.FOO_VALUE, map.getEnum(
        EnumReachableThroughMapValue.class, "FOO_VALUE"));
  }

  public void testLong() {
    long[] longs = { Long.MIN_VALUE + 1, // See issue 7308
        42L, Long.MAX_VALUE };
    for (long l : longs) {
      AutoBean<HasLong> bean = f.hasLong();
      bean.as().setLong(l);
      AutoBean<HasLong> decodedBean = checkEncode(bean);
      assertEquals(l, decodedBean.as().getLong());
    }
  }

  public void testMap() {
    AutoBean<HasMap> bean = f.hasMap();
    Map<String, Simple> map = new HashMap<String, Simple>();
    Map<Simple, Simple> complex = new HashMap<Simple, Simple>();
    bean.as().setSimpleMap(map);
    bean.as().setComplexMap(complex);

    for (int i = 0, j = 5; i < j; i++) {
      Simple s = f.simple().as();
      s.setInt(i);
      map.put(String.valueOf(i), s);

      Simple key = f.simple().as();
      key.setString(String.valueOf(i));
      complex.put(key, s);
    }

    AutoBean<HasMap> decoded = checkEncode(bean);
    map = decoded.as().getSimpleMap();
    complex = decoded.as().getComplexMap();
    assertEquals(5, map.size());
    for (int i = 0, j = 5; i < j; i++) {
      Simple s = map.get(String.valueOf(i));
      assertNotNull(s);
      assertEquals(i, s.getInt());
    }
    assertEquals(5, complex.size());
    for (Map.Entry<Simple, Simple> entry : complex.entrySet()) {
      assertEquals(entry.getKey().getString(), String.valueOf(entry.getValue().getInt()));
    }
  }

  /**
   * Verify that arbitrarily complicated Maps of Maps work.
   */
  public void testNestedMap() {
    Map<String, String> key = new HashMap<String, String>();
    key.put("a", "b");

    Map<String, String> value = new HashMap<String, String>();
    value.put("c", "d");

    Map<Map<String, String>, Map<String, String>> test =
        new HashMap<Map<String, String>, Map<String, String>>();
    test.put(key, value);

    AutoBean<HasMap> bean = f.hasMap();
    bean.as().setNestedMap(test);

    AutoBean<HasMap> decoded = checkEncode(bean);
    assertEquals(1, decoded.as().getNestedMap().size());
  }

  public void testNull() {
    AutoBean<Simple> bean = f.simple();
    AutoBean<Simple> decodedBean = checkEncode(bean);
    assertNull(decodedBean.as().getString());
  }

  public void testSimple() {
    AutoBean<Simple> bean = f.simple();
    Simple simple = bean.as();
    simple.setBoolean(true);
    simple.setInt(42);
    simple.setOtherBoolean(true);
    simple.setString("Hello World!");

    AutoBean<Simple> decodedBean = checkEncode(bean);
    assertTrue(AutoBeanUtils.diff(bean, decodedBean).isEmpty());
    assertTrue(decodedBean.as().isBoolean());
    assertTrue(decodedBean.as().hasOtherBoolean());

    AutoBean<HasSimple> bean2 = f.hasSimple();
    bean2.as().setSimple(simple);

    AutoBean<HasSimple> decodedBean2 = checkEncode(bean2);
    assertNotNull(decodedBean2.as().getSimple());
    assertTrue(AutoBeanUtils.diff(bean, AutoBeanUtils.getAutoBean(decodedBean2.as().getSimple()))
        .isEmpty());

    AutoBean<HasList> bean3 = f.hasList();
    bean3.as().setIntList(Arrays.asList(1, 2, 3, null, 4, 5));
    bean3.as().setList(Arrays.asList(simple));

    AutoBean<HasList> decodedBean3 = checkEncode(bean3);
    assertNotNull(decodedBean3.as().getIntList());
    assertEquals(Arrays.asList(1, 2, 3, null, 4, 5), decodedBean3.as().getIntList());
    assertNotNull(decodedBean3.as().getList());
    assertEquals(1, decodedBean3.as().getList().size());
    assertTrue(AutoBeanUtils.diff(bean,
        AutoBeanUtils.getAutoBean(decodedBean3.as().getList().get(0))).isEmpty());
  }

  public void testSplittable() {
    AutoBean<Simple> simple = f.simple();
    simple.as().setString("Simple");
    AutoBean<HasSplittable> bean = f.hasAutoBean();
    bean.as().setSimple(AutoBeanCodex.encode(simple));
    bean.as().setString(ValueCodex.encode("Hello ['\"] world"));
    List<Splittable> testList =
        Arrays.asList(AutoBeanCodex.encode(simple), null, AutoBeanCodex.encode(simple));
    bean.as().setSimpleList(testList);
    Map<Splittable, Splittable> testMap =
        Collections.singletonMap(ValueCodex.encode("12345"), ValueCodex.encode("5678"));
    bean.as().setSplittableMap(testMap);

    AutoBean<HasSplittable> decoded = checkEncode(bean);
    Splittable toDecode = decoded.as().getSimple();
    AutoBean<Simple> decodedSimple = AutoBeanCodex.decode(f, Simple.class, toDecode);
    assertEquals("Simple", decodedSimple.as().getString());
    assertEquals("Hello ['\"] world", ValueCodex.decode(String.class, decoded.as().getString()));
    assertEquals("12345", decoded.as().getSplittableMap().keySet().iterator().next().asString());
    assertEquals("5678", decoded.as().getSplittableMap().values().iterator().next().asString());

    List<Splittable> list = decoded.as().getSimpleList();
    assertEquals(3, list.size());
    assertNull(list.get(1));
    assertEquals("Simple", AutoBeanCodex.decode(f, Simple.class, list.get(2)).as().getString());
  }

  @Override
  protected void gwtSetUp() throws Exception {
    f = GWT.create(Factory.class);
  }

  private <T> AutoBean<T> checkEncode(AutoBean<T> bean) {
    Splittable split = AutoBeanCodex.encode(bean);
    AutoBean<T> decoded = AutoBeanCodex.decode(f, bean.getType(), split);
    assertTrue(AutoBeanUtils.deepEquals(bean, decoded));
    return decoded;
  }
}
