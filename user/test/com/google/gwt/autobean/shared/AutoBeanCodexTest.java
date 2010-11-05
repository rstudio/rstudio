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
package com.google.gwt.autobean.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.Arrays;
import java.util.Collections;
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
    AutoBean<HasAutoBean> hasAutoBean();

    /**
     * @return
     */
    AutoBean<HasCycle> hasCycle();

    AutoBean<HasList> hasList();

    /**
     * @return
     */
    AutoBean<HasMap> hasMap();

    AutoBean<HasSimple> hasSimple();

    AutoBean<Simple> simple();
  }

  interface HasAutoBean {
    Splittable getSimple();

    List<Splittable> getSimpleList();

    Splittable getString();

    void setSimple(Splittable simple);

    void setSimpleList(List<Splittable> simple);

    void setString(Splittable s);
  }

  /**
   * Used to test that cycles are detected.
   */
  interface HasCycle {
    List<HasCycle> getCycle();

    void setCycle(List<HasCycle> cycle);
  }

  interface HasList {
    List<Integer> getIntList();

    List<Simple> getList();

    void setIntList(List<Integer> list);

    void setList(List<Simple> list);
  }

  interface HasMap {
    Map<Simple, Simple> getComplexMap();

    Map<String, Simple> getSimpleMap();

    void setComplexMap(Map<Simple, Simple> map);

    void setSimpleMap(Map<String, Simple> map);
  }

  interface HasSimple {
    Simple getSimple();

    void setSimple(Simple s);
  }

  interface Simple {
    int getInt();

    String getString();

    void setInt(int i);

    void setString(String s);
  }

  protected Factory f;

  @Override
  public String getModuleName() {
    return "com.google.gwt.autobean.AutoBean";
  }

  public void testCycle() {
    AutoBean<HasCycle> bean = f.hasCycle();
    bean.as().setCycle(Arrays.asList(bean.as()));
    try {
      AutoBeanCodex.encode(bean);
      fail("Should not have encoded");
    } catch (UnsupportedOperationException expected) {
    }
  }

  public void testEmptyList() {
    AutoBean<HasList> bean = f.hasList();
    bean.as().setList(Collections.<Simple> emptyList());
    Splittable split = AutoBeanCodex.encode(bean);
    AutoBean<HasList> decodedBean = AutoBeanCodex.decode(f, HasList.class,
        split);
    assertNotNull(decodedBean.as().getList());
    assertTrue(decodedBean.as().getList().isEmpty());
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

    Splittable split = AutoBeanCodex.encode(bean);
    AutoBean<HasMap> decoded = AutoBeanCodex.decode(f, HasMap.class, split);
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
      assertEquals(entry.getKey().getString(),
          String.valueOf(entry.getValue().getInt()));
    }
  }

  public void testNull() {
    AutoBean<Simple> bean = f.simple();
    Splittable split = AutoBeanCodex.encode(bean);
    AutoBean<Simple> decodedBean = AutoBeanCodex.decode(f, Simple.class, split);
    assertNull(decodedBean.as().getString());
  }

  public void testSimple() {
    AutoBean<Simple> bean = f.simple();
    Simple simple = bean.as();
    simple.setInt(42);
    simple.setString("Hello World!");

    Splittable split = AutoBeanCodex.encode(bean);

    AutoBean<Simple> decodedBean = AutoBeanCodex.decode(f, Simple.class, split);
    assertTrue(AutoBeanUtils.diff(bean, decodedBean).isEmpty());

    AutoBean<HasSimple> bean2 = f.hasSimple();
    bean2.as().setSimple(simple);
    split = AutoBeanCodex.encode(bean2);

    AutoBean<HasSimple> decodedBean2 = AutoBeanCodex.decode(f, HasSimple.class,
        split);
    assertNotNull(decodedBean2.as().getSimple());
    assertTrue(AutoBeanUtils.diff(bean,
        AutoBeanUtils.getAutoBean(decodedBean2.as().getSimple())).isEmpty());

    AutoBean<HasList> bean3 = f.hasList();
    bean3.as().setIntList(Arrays.asList(1, 2, 3, null, 4, 5));
    bean3.as().setList(Arrays.asList(simple));
    split = AutoBeanCodex.encode(bean3);

    AutoBean<HasList> decodedBean3 = AutoBeanCodex.decode(f, HasList.class,
        split);
    assertNotNull(decodedBean3.as().getIntList());
    assertEquals(Arrays.asList(1, 2, 3, null, 4, 5),
        decodedBean3.as().getIntList());
    assertNotNull(decodedBean3.as().getList());
    assertEquals(1, decodedBean3.as().getList().size());
    assertTrue(AutoBeanUtils.diff(bean,
        AutoBeanUtils.getAutoBean(decodedBean3.as().getList().get(0))).isEmpty());
  }

  public void testSplittable() {
    AutoBean<Simple> simple = f.simple();
    simple.as().setString("Simple");
    AutoBean<HasAutoBean> bean = f.hasAutoBean();
    bean.as().setSimple(AutoBeanCodex.encode(simple));
    bean.as().setString(ValueCodex.encode("Hello ['\"] world"));
    List<Splittable> testList = Arrays.asList(AutoBeanCodex.encode(simple),
        null, AutoBeanCodex.encode(simple));
    bean.as().setSimpleList(testList);
    Splittable split = AutoBeanCodex.encode(bean);

    AutoBean<HasAutoBean> decoded = AutoBeanCodex.decode(f, HasAutoBean.class,
        split);
    Splittable toDecode = decoded.as().getSimple();
    AutoBean<Simple> decodedSimple = AutoBeanCodex.decode(f, Simple.class,
        toDecode);
    assertEquals("Simple", decodedSimple.as().getString());
    assertEquals("Hello ['\"] world",
        ValueCodex.decode(String.class, decoded.as().getString()));

    List<Splittable> list = decoded.as().getSimpleList();
    assertEquals(3, list.size());
    assertNull(list.get(1));
    assertEquals("Simple",
        AutoBeanCodex.decode(f, Simple.class, list.get(2)).as().getString());
  }

  @Override
  protected void gwtSetUp() throws Exception {
    f = GWT.create(Factory.class);
  }
}
