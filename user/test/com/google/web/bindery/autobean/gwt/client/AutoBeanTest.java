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
package com.google.web.bindery.autobean.gwt.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.AutoBeanFactory.Category;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor;
import com.google.web.bindery.autobean.shared.AutoBeanVisitor.ParameterizationVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Tests runtime behavior of AutoBean framework.
 */
public class AutoBeanTest extends GWTTestCase {

  /**
   * Static implementation of {@link HasCall}.
   */
  public static class CallImpl {
    public static Object seen;

    public static <T> T __intercept(AutoBean<HasCall> bean, T value) {
      assertNotNull(bean);
      seen = value;
      return value;
    }

    public static int add(AutoBean<HasCall> bean, int a, int b) {
      assertNotNull(bean);
      return ((Integer) bean.getTag("offset")) + a + b;
    }
  }

  /**
   * The factory being tested.
   */
  @Category(CallImpl.class)
  protected interface Factory extends AutoBeanFactory {
    AutoBean<HasBoolean> hasBoolean();

    AutoBean<HasCall> hasCall();

    AutoBean<HasChainedSetters> hasChainedSetters();

    AutoBean<HasList> hasList();

    AutoBean<HasComplexTypes> hasListOfList();

    AutoBean<HasMoreChainedSetters> hasMoreChainedSetters();

    AutoBean<Intf> intf();

    AutoBean<Intf> intf(RealIntf wrapped);

    AutoBean<OtherIntf> otherIntf();
  }

  interface HasBoolean {
    boolean getGet();

    boolean hasHas();

    boolean isIs();

    void setGet(boolean value);

    void setHas(boolean value);

    void setIs(boolean value);
  }

  interface HasCall {
    int add(int a, int b);
  }

  interface HasChainedSetters {
    int getInt();

    String getString();

    HasChainedSetters setInt(int value);

    HasChainedSetters setString(String value);
  }

  interface HasComplexTypes {
    List<List<Intf>> getList();

    List<Map<String, Intf>> getListOfMap();

    Map<Map<String, String>, List<List<Intf>>> getMap();
  }

  interface HasList {
    List<Intf> getList();

    void setList(List<Intf> list);
  }

  interface HasMoreChainedSetters extends HasChainedSetters {
    boolean isBoolean();

    HasMoreChainedSetters setBoolean(boolean value);

    HasMoreChainedSetters setInt(int value);
  }

  interface Intf {
    int getInt();

    String getProperty();

    String getString();

    void setInt(int number);

    // Avoid name conflicts in AbstractAutoBean
    void setProperty(String value);

    void setString(String value);
  }

  interface OtherIntf {
    HasBoolean getHasBoolean();

    Intf getIntf();

    UnreferencedInFactory getUnreferenced();

    void setHasBoolean(HasBoolean value);

    void setIntf(Intf intf);
  }

  static class RealIntf implements Intf {
    int i;
    String property;
    String string;

    @Override
    public boolean equals(Object o) {
      return (o instanceof Intf) && (((Intf) o).getInt() == getInt());
    }

    public int getInt() {
      return i;
    }

    @Override
    public String getProperty() {
      return property;
    }

    public String getString() {
      return string;
    }

    @Override
    public int hashCode() {
      return i;
    }

    public void setInt(int number) {
      this.i = number;
    }

    @Override
    public void setProperty(String value) {
      this.property = value;
    }

    public void setString(String value) {
      this.string = value;
    }

    public String toString() {
      return "toString";
    }
  }

  interface UnreferencedInFactory {
  }

  private static class ParameterizationTester extends ParameterizationVisitor {
    private final StringBuilder sb;
    private Stack<Boolean> isOpen = new Stack<Boolean>();

    private ParameterizationTester(StringBuilder sb) {
      this.sb = sb;
    }

    @Override
    public void endVisitType(Class<?> type) {
      if (isOpen.pop()) {
        sb.append(">");
      }
    }

    @Override
    public boolean visitParameter() {
      if (isOpen.peek()) {
        sb.append(",");
      } else {
        sb.append("<");
        isOpen.pop();
        isOpen.push(true);
      }
      return true;
    }

    @Override
    public boolean visitType(Class<?> type) {
      sb.append(type.getName());
      isOpen.push(false);
      return true;
    }
  }

  protected Factory factory;

  @Override
  public String getModuleName() {
    return "com.google.web.bindery.autobean.AutoBean";
  }

  public void testBooleanIsHasMethods() {
    HasBoolean b = factory.hasBoolean().as();
    assertFalse(b.getGet());
    assertFalse(b.hasHas());
    assertFalse(b.isIs());

    b.setGet(true);
    b.setHas(true);
    b.setIs(true);

    assertTrue(b.getGet());
    assertTrue(b.hasHas());
    assertTrue(b.isIs());
  }

  public void testCategory() {
    AutoBean<HasCall> call = factory.hasCall();
    call.setTag("offset", 1);
    assertEquals(6, call.as().add(2, 3));
    assertEquals(6, CallImpl.seen);
  }

  public void testChainedSetters() {
    AutoBean<HasChainedSetters> bean = factory.hasChainedSetters();
    bean.as().setInt(42).setString("Blah");
    assertEquals(42, bean.as().getInt());
    assertEquals("Blah", bean.as().getString());

    AutoBean<HasMoreChainedSetters> more = factory.hasMoreChainedSetters();
    more.as().setInt(42).setBoolean(true).setString("Blah");
    assertEquals(42, more.as().getInt());
    assertTrue(more.as().isBoolean());
    assertEquals("Blah", more.as().getString());
  }

  public void testDiff() {
    AutoBean<Intf> a1 = factory.intf();
    AutoBean<Intf> a2 = factory.intf();

    assertTrue(AutoBeanUtils.diff(a1, a2).isEmpty());

    a2.as().setInt(42);
    Map<String, Object> diff = AutoBeanUtils.diff(a1, a2);
    assertEquals(1, diff.size());
    assertEquals(42, diff.get("int"));
  }

  public void testDiffWithListPropertyAssignment() {
    AutoBean<HasList> a1 = factory.hasList();
    AutoBean<HasList> a2 = factory.hasList();

    assertTrue(AutoBeanUtils.diff(a1, a2).isEmpty());

    List<Intf> l1 = new ArrayList<Intf>();
    a1.as().setList(l1);
    List<Intf> l2 = new ArrayList<Intf>();
    a2.as().setList(l2);

    assertTrue(AutoBeanUtils.diff(a1, a2).isEmpty());

    l2.add(factory.intf().as());
    Map<String, Object> diff = AutoBeanUtils.diff(a1, a2);
    assertEquals(1, diff.size());
    assertEquals(l2, diff.get("list"));

    l1.add(l2.get(0));
    assertTrue(AutoBeanUtils.diff(a1, a2).isEmpty());
  }

  public void testDynamicMethods() {
    AutoBean<Intf> intf = factory.create(Intf.class);
    assertNotNull(intf);

    RealIntf real = new RealIntf();
    real.i = 42;
    intf = factory.create(Intf.class, real);
    assertNotNull(intf);
    assertEquals(42, intf.as().getInt());
  }

  public void testEquality() {
    AutoBean<Intf> a1 = factory.intf();
    AutoBean<Intf> a2 = factory.intf();

    assertNotSame(a1, a2);
    assertFalse(a1.equals(a2));

    // Make sure as() is stable
    assertSame(a1.as(), a1.as());
    assertEquals(a1.as(), a1.as());

    // When wrapping, use underlying object's equality
    RealIntf real = new RealIntf();
    real.i = 42;
    AutoBean<Intf> w = factory.intf(real);
    // AutoBean interface never equals wrapped object
    assertFalse(w.equals(real));
    // Wrapper interface should delegate hashCode(), equals(), and toString()
    assertEquals(real.hashCode(), w.as().hashCode());
    assertEquals(real, w.as());
    assertEquals(real.toString(), w.as().toString());
    assertEquals(w.as(), real);
  }

  public void testFactory() {
    AutoBean<Intf> auto = factory.intf();
    assertSame(factory, auto.getFactory());
  }

  public void testFreezing() {
    AutoBean<Intf> auto = factory.intf();
    Intf intf = auto.as();
    intf.setInt(42);
    auto.setFrozen(true);
    try {
      intf.setInt(55);
      fail("Should have thrown an exception");
    } catch (IllegalStateException expected) {
    }

    assertTrue(auto.isFrozen());
    assertEquals(42, intf.getInt());
  }

  public void testNested() {
    AutoBean<OtherIntf> auto = factory.otherIntf();
    OtherIntf other = auto.as();

    assertNull(other.getIntf());

    Intf intf = new RealIntf();
    intf.setString("Hello world!");
    other.setIntf(intf);
    Intf retrieved = other.getIntf();
    assertEquals("Hello world!", retrieved.getString());
    assertNotNull(AutoBeanUtils.getAutoBean(retrieved));
  }

  public void testParameterizationVisitor() {
    AutoBean<HasComplexTypes> auto = factory.hasListOfList();
    auto.accept(new AutoBeanVisitor() {
      int count = 0;

      @Override
      public void endVisit(AutoBean<?> bean, Context ctx) {
        assertEquals(3, count);
      }

      @Override
      public void endVisitCollectionProperty(String propertyName, AutoBean<Collection<?>> value,
          CollectionPropertyContext ctx) {
        check(propertyName, ctx);
      }

      @Override
      public void endVisitMapProperty(String propertyName, AutoBean<Map<?, ?>> value,
          MapPropertyContext ctx) {
        check(propertyName, ctx);
      }

      private void check(String propertyName, PropertyContext ctx) {
        count++;
        StringBuilder sb = new StringBuilder();
        ctx.accept(new ParameterizationTester(sb));

        if ("list".equals(propertyName)) {
          // List<List<Intf>>
          assertEquals(List.class.getName() + "<" + List.class.getName() + "<"
              + Intf.class.getName() + ">>", sb.toString());
        } else if ("listOfMap".equals(propertyName)) {
          // List<Map<String, Intf>>
          assertEquals(List.class.getName() + "<" + Map.class.getName() + "<"
              + String.class.getName() + "," + Intf.class.getName() + ">>", sb.toString());
        } else if ("map".equals(propertyName)) {
          // Map<Map<String, String>, List<List<Intf>>>
          assertEquals(Map.class.getName() + "<" + Map.class.getName() + "<"
              + String.class.getName() + "," + String.class.getName() + ">," + List.class.getName()
              + "<" + List.class.getName() + "<" + Intf.class.getName() + ">>>", sb.toString());
        } else {
          throw new RuntimeException(propertyName);
        }
      }
    });
  }

  /**
   * Make sure primitive properties can be returned.
   */
  public void testPrimitiveProperty() {
    AutoBean<Intf> auto = factory.intf();
    Intf intf = auto.as();

    assertNull(intf.getString());
    intf.setString("Hello world!");
    assertEquals("Hello world!", intf.getString());

    assertEquals(0, intf.getInt());
    intf.setInt(42);
    assertEquals(42, intf.getInt());
  }

  public void testTags() {
    AutoBean<Intf> auto = factory.intf();
    auto.setTag("test", 42);
    assertEquals(42, auto.getTag("test"));
  }

  public void testTraversal() {
    final AutoBean<OtherIntf> other = factory.otherIntf();
    final AutoBean<Intf> intf = factory.intf();
    final AutoBean<HasBoolean> hasBoolean = factory.hasBoolean();
    other.as().setIntf(intf.as());
    other.as().setHasBoolean(hasBoolean.as());
    intf.as().setInt(42);
    hasBoolean.as().setGet(true);
    hasBoolean.as().setHas(true);
    hasBoolean.as().setIs(true);

    class Checker extends AutoBeanVisitor {
      boolean seenHasBoolean;
      boolean seenIntf;
      boolean seenOther;

      @Override
      public void endVisitReferenceProperty(String propertyName, AutoBean<?> value,
          PropertyContext ctx) {
        if ("hasBoolean".equals(propertyName)) {
          assertSame(hasBoolean, value);
          assertEquals(HasBoolean.class, ctx.getType());
        } else if ("intf".equals(propertyName)) {
          assertSame(intf, value);
          assertEquals(Intf.class, ctx.getType());
        } else if ("unreferenced".equals(propertyName)) {
          assertNull(value);
          assertEquals(UnreferencedInFactory.class, ctx.getType());
        } else {
          fail("Unexpecetd property " + propertyName);
        }
      }

      @Override
      public void endVisitValueProperty(String propertyName, Object value, PropertyContext ctx) {
        if ("int".equals(propertyName)) {
          assertEquals(42, value);
          assertEquals(int.class, ctx.getType());
        } else if ("string".equals(propertyName) || "property".equals(propertyName)) {
          assertNull(value);
          assertEquals(String.class, ctx.getType());
        } else if ("get".equals(propertyName) || "has".equals(propertyName)
            || "is".equals(propertyName)) {
          assertEquals(boolean.class, ctx.getType());
          assertTrue((Boolean) value);
        } else {
          fail("Unknown value property " + propertyName);
        }
      }

      @Override
      public boolean visit(AutoBean<?> bean, Context ctx) {
        if (bean == hasBoolean) {
          seenHasBoolean = true;
        } else if (bean == intf) {
          seenIntf = true;
        } else if (bean == other) {
          seenOther = true;
        } else {
          fail("Unknown AutoBean");
        }
        return true;
      }

      void check() {
        assertTrue(seenHasBoolean);
        assertTrue(seenIntf);
        assertTrue(seenOther);
      }
    }
    Checker c = new Checker();
    other.accept(c);
    c.check();
  }

  public void testType() {
    assertEquals(Intf.class, factory.intf().getType());
  }

  /**
   * Ensure that a totally automatic bean can't be unwrapped, since the
   * generated mapper depends on the AutoBean.
   */
  public void testUnwrappingSimpleBean() {
    AutoBean<Intf> auto = factory.intf();
    try {
      auto.unwrap();
      fail();
    } catch (IllegalStateException expected) {
    }
  }

  public void testWrapped() {
    RealIntf real = new RealIntf();
    AutoBean<Intf> auto = factory.intf(real);
    Intf intf = auto.as();

    assertNotSame(real, intf);
    assertNull(intf.getString());
    assertEquals(0, intf.getInt());

    real.string = "blah";
    assertEquals("blah", intf.getString());
    real.i = 42;
    assertEquals(42, intf.getInt());

    intf.setString("bar");
    assertEquals("bar", real.string);

    intf.setInt(41);
    assertEquals(41, real.i);

    AutoBean<Intf> rewrapped = factory.intf(real);
    assertSame(auto, rewrapped);

    // Disconnect the wrapper, make sure it shuts down correctly.
    Intf unwrapped = auto.unwrap();
    assertSame(real, unwrapped);
    assertNull(AutoBeanUtils.getAutoBean(real));
    try {
      intf.setInt(42);
      fail("Should have thrown exception");
    } catch (IllegalStateException expected) {
    }
  }

  @Override
  protected void gwtSetUp() throws Exception {
    factory = GWT.create(Factory.class);
  }
}
