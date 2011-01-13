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
package com.google.gwt.uibinder.rebind.model;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.rebind.JClassTypeAdapter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderContext;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Tests for descriptors of potential owner field classes.
 */
public class OwnerFieldClassTest extends TestCase {

  private JClassTypeAdapter gwtTypeAdapter;
  private UiBinderContext uiBinderCtx;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    uiBinderCtx = new UiBinderContext();
    gwtTypeAdapter = new JClassTypeAdapter();
  }

  public void testOwnerFieldClass() throws Exception {
    // Get the JType for an HTML
    JClassType htmlType = gwtTypeAdapter.adaptJavaClass(HTML.class);

    // Now get its field class model
    OwnerFieldClass fieldClass = OwnerFieldClass.getFieldClass(htmlType,
        MortalLogger.NULL, uiBinderCtx);

    // Check the class model properties
    assertEquals(htmlType, fieldClass.getRawType());
    assertNull(fieldClass.getUiConstructor());

    // simple property: visible="" maps to setVisible
    JMethod visibleSetter = fieldClass.getSetter("visible");
    assertMethod(visibleSetter, "setVisible", JPrimitiveType.BOOLEAN);

    // all-upper-case property, Java Bean naming (all-upper-cased)
    JMethod htmlSetter = fieldClass.getSetter("HTML");
    assertMethod(htmlSetter, "setHTML",
        gwtTypeAdapter.adaptJavaClass(String.class));

    // all-upper-case property, GWT-legacy naming with the first char
    // lower-cased
    htmlSetter = fieldClass.getSetter("hTML");
    assertMethod(htmlSetter, "setHTML",
        gwtTypeAdapter.adaptJavaClass(String.class));

    // Check that the same instance of the model is returned if asked again
    assertSame(fieldClass,
        OwnerFieldClass.getFieldClass(htmlType, MortalLogger.NULL, uiBinderCtx));

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Class with lots of setters for testing.
   */
  @SuppressWarnings("unused")
  // We know these methods are unused
  private static class SettersTestClass {
    // No ambiguity in these setters
    public void setBlaBla(int x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void setBlaBle(String x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // To be used in subclass test
    public void setBlaBla2(int x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void setBlaBle2(String x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void setBli2(int x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void setBli2(double x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // Ambiguous, String parameter should win
    public void setBle(int y) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void setBle(String y) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // Ambiguous with no winner
    public void setBli(int y) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void setBli(double y) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // Not considered setters
    void setNothing(int x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void set() {
    }

    public int setNothing2(String x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void notASetter(String x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public static void setStatic(String x) {
      throw new UnsupportedOperationException("Should never get called");
    }
  }

  /**
   * 
   * base class for setters disambiguation tests.
   *
   */
  public class baseSetters {
    public baseSetters() {
    }
    
    // setvalue1 is not ambiguous
    public void setValue1(@SuppressWarnings("unused") boolean b) {
    }

    public void setValue1(@SuppressWarnings("unused") Boolean b) {
    }

    // derived wins
    public void setValue2(@SuppressWarnings("unused") Integer b) {
    }

    // this overload wins
    public void setValue3(@SuppressWarnings("unused") int b) {
    }
    
    // this is not ambiguous since derived
    // has the exact same signature
    public void setValue4(@SuppressWarnings("unused") int b) {
    }
    
    // setvalue5 is ambiguous
    public void setValue5(@SuppressWarnings("unused") float f) {
    }
    
    public void setValue5(@SuppressWarnings("unused") double d) {
    }
    
    // string always wins
    public void setValue6(@SuppressWarnings("unused") String s) {
    }

    public void setValue6(@SuppressWarnings("unused") char s) {
    }
    
    public void setValue6(@SuppressWarnings("unused") Object s) {
    }
    
    // primitive wins
    public void setValue7(@SuppressWarnings("unused") int s) {
    }
    
    public void setValue7(@SuppressWarnings("unused") StringBuffer s) {
    }
  }

  /**
   * 
   * derived class for setter disambiguation tests.
   *
   */
  public class derivedSetters extends baseSetters {
    public derivedSetters() {
      super();
    }
    
    public void setValue2(@SuppressWarnings("unused") int b) {
    }
    
    public void setValue3(@SuppressWarnings("unused") Integer b) {
    }
    
    public void setValue4(int b) {
    }
  }

  /**
   * Regression test.
   */
  public void testCheckBoxValueSetters() throws Exception {
      JClassType cbClassType = gwtTypeAdapter.adaptJavaClass(CheckBox.class);
      OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(cbClassType,
          MortalLogger.NULL, uiBinderCtx);
      JMethod setValueSetter = settersClass.getSetter("value");
      assertNotNull(setValueSetter);
  }  
  
  public void testDisambiguateClassHierarchySettersBase() throws Exception {
    // ensure that primitive types win over boxed primitive types.
    JClassType baseClassType = gwtTypeAdapter.adaptJavaClass(baseSetters.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(baseClassType,
        MortalLogger.NULL, uiBinderCtx);
    JMethod setValueSetter = settersClass.getSetter("value1");
    assertNotNull(setValueSetter);
  }

  public void testDisambiguateClassHierarchySettersDerived() throws Exception {
    // ensure that primitive types win over boxed primitive types
    // in a class hierarchy.
    JClassType derivedClass = gwtTypeAdapter.adaptJavaClass(derivedSetters.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(derivedClass,
        MortalLogger.NULL, uiBinderCtx);
    
    // base.value1(boolean) and base.value1(Boolean) is never ambiguous 
    // must return boolean
    assertNotNull(settersClass.getSetter("value1"));
    assertMethod(settersClass.getSetter("value1"), "setValue1", JPrimitiveType.BOOLEAN);
    
    // base.value2(Integer) and derived.value2(int) is not ambiguous - must be int
    assertNotNull(settersClass.getSetter("value2"));
    assertMethod(settersClass.getSetter("value2"), "setValue2", JPrimitiveType.INT);
    
    // base.value3 (int) and derived.value3(Integer) is not ambiguous - must be int.
    assertNotNull(settersClass.getSetter("value3"));
    
    // base.value4(int) and derived.value4(int) is not ambiguous.
    assertNotNull(settersClass.getSetter("value4"));
    
    // base.value5(float) and base.value5(double) is ambiguous
    try {
      settersClass.getSetter("value5");
      fail("Expected exception not thrown");
    } catch (UnableToCompleteException utce) {
      // Expected
    }
    
    // value6 has multiple overload but string always wins
    // base.value6(string), base.value6(char) and base.value6(object)
    assertNotNull(settersClass.getSetter("value6"));
    assertMethod(settersClass.getSetter("value6"), "setValue6", 
        gwtTypeAdapter.adaptJavaClass(String.class));
    
    // base.value7(object) and base.value7(int) is not ambiguous - must be int.
    assertNotNull(settersClass.getSetter("value7"));
    assertMethod(settersClass.getSetter("value7"), "setValue7", 
        JPrimitiveType.INT);
  }
  
  public void testOwnerFieldClass_setters() throws Exception {
    JClassType settersType = gwtTypeAdapter.adaptJavaClass(SettersTestClass.class);
    JClassType stringType = gwtTypeAdapter.adaptJavaClass(String.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(settersType,
        MortalLogger.NULL, uiBinderCtx);
    assertEquals(settersType, settersClass.getRawType());
    assertNull(settersClass.getUiConstructor());

    JMethod blaBlaSetter = settersClass.getSetter("blaBla");
    assertMethod(blaBlaSetter, "setBlaBla", JPrimitiveType.INT);
    JMethod blaBleSetter = settersClass.getSetter("blaBle");
    assertMethod(blaBleSetter, "setBlaBle", stringType);

    assertNull(settersClass.getSetter("nothing"));
    assertNull(settersClass.getSetter("nothing2"));
    assertNull(settersClass.getSetter("notASetter"));
    assertNull(settersClass.getSetter("aSetter"));
    assertNull(settersClass.getSetter("static"));

    gwtTypeAdapter.verifyAll();
  }

  public void testOwnerFieldClass_ambiguousSetters() throws Exception {
    JClassType settersType = gwtTypeAdapter.adaptJavaClass(SettersTestClass.class);
    JClassType stringType = gwtTypeAdapter.adaptJavaClass(String.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(settersType,
        MortalLogger.NULL, uiBinderCtx);
    assertEquals(settersType, settersClass.getRawType());

    JMethod bleSetter = settersClass.getSetter("ble");
    assertMethod(bleSetter, "setBle", stringType);

    try {
      settersClass.getSetter("bli");
      fail("Expected exception not thrown");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Class with overridden setters for testing.
   */
  @SuppressWarnings("unused")
  // We know these methods are unused
  private static class OverriddenSettersTestClass extends SettersTestClass {
    // Simple override of parent method
    @Override
    public void setBlaBla(int x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // setBlaBle is not overridden

    // Subclass adds ambiguity, String from this class wins
    public void setBlaBla2(String x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // Subclass adds ambiguity, String from superclass wins
    public void setBlaBle2(int x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // setBle had settled ambiguity, this shouldn't change it
    public void setBle(double x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // setBli2 ambiguous in superclass only

    // setBlo us ambiguous here only
    public void setBlo(int x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    public void setBlo(double x) {
      throw new UnsupportedOperationException("Should never get called");
    }

    // Solves superclass ambiguity
    public void setBli(String y) {
      throw new UnsupportedOperationException("Should never get called");
    }
  }

  public void testOwnerFieldClass_overriddenSetters() throws Exception {
    JClassType settersType = gwtTypeAdapter.adaptJavaClass(OverriddenSettersTestClass.class);
    JClassType stringType = gwtTypeAdapter.adaptJavaClass(String.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(settersType,
        MortalLogger.NULL, uiBinderCtx);
    assertEquals(settersType, settersClass.getRawType());

    // setBlaBla is not ambiguous, though overridden
    JMethod blaBlaSetter = settersClass.getSetter("blaBla");
    assertMethod(blaBlaSetter, "setBlaBla", JPrimitiveType.INT);

    // setBlaBle is not overridden, works from superclass
    JMethod blaBleSetter = settersClass.getSetter("blaBle");
    assertMethod(blaBleSetter, "setBlaBle", stringType);

    // setBlaBla2 is not ambiguous, subclass String wins
    JMethod blaBla2Setter = settersClass.getSetter("blaBla2");
    assertMethod(blaBla2Setter, "setBlaBla2", stringType);

    // setBlaBle2 is not ambiguous, superclass String wins
    JMethod blaBle2Setter = settersClass.getSetter("blaBle2");
    assertMethod(blaBle2Setter, "setBlaBle2", stringType);

    // setBle is disambiguated and overridden
    JMethod bleSetter = settersClass.getSetter("ble");
    assertMethod(bleSetter, "setBle", stringType);

    // setBli was ambiguous in the superclass, subclass String settles it
    JMethod bliSetter = settersClass.getSetter("bli");
    assertMethod(bliSetter, "setBli", stringType);

    // setBli2 is ambiguous in the superclass
    try {
      settersClass.getSetter("bli2");
      fail("Expected exception not thrown");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    // setBlo is ambiguous in the subclass
    try {
      settersClass.getSetter("blo");
      fail("Expected exception not thrown");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    // Ignored superclass setters are still ignored
    assertNull(settersClass.getSetter("nothing"));
    assertNull(settersClass.getSetter("nothing2"));
    assertNull(settersClass.getSetter("notASetter"));
    assertNull(settersClass.getSetter("aSetter"));
    assertNull(settersClass.getSetter("static"));

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Class with a {@link UiChild}-annotated methods.
   */
  @SuppressWarnings("unused")
  // We know these methods are unused
  private static class UiChildClass {
    public UiChildClass() {
      throw new UnsupportedOperationException("Should never get called");
    }

    @UiChild
    void addChild(Object child) {
      throw new UnsupportedOperationException("Should never get called");
    }

    @UiChild(tagname = "second", limit = 4)
    void doesNotStartWithAdd(Object child) {
      throw new UnsupportedOperationException("Should never get called");
    }
  }

  public void testOwnerFieldClass_withUiChildren() throws Exception {
    JClassType parentType = gwtTypeAdapter.adaptJavaClass(UiChildClass.class);
    OwnerFieldClass parentClass = OwnerFieldClass.getFieldClass(parentType,
        MortalLogger.NULL, uiBinderCtx);
    assertEquals(parentType, parentClass.getRawType());

    Map<String, Pair<JMethod, Integer>> childMethods = parentClass.getUiChildMethods();
    assertNotNull(childMethods);
    assertEquals(2, childMethods.size());

    Pair<JMethod, Integer> childPair = childMethods.get("child");
    assertEquals("addChild", childPair.left.getName());
    assertEquals(Integer.valueOf(-1), childPair.right);

    Pair<JMethod, Integer> secondPair = childMethods.get("second");
    assertEquals("doesNotStartWithAdd", secondPair.left.getName());
    assertEquals(Integer.valueOf(4), secondPair.right);

    gwtTypeAdapter.verifyAll();
  }

  public void testOwnerFieldClass_withNoUiChildren() throws Exception {
    JClassType parentType = gwtTypeAdapter.adaptJavaClass(Object.class);
    OwnerFieldClass parentClass = OwnerFieldClass.getFieldClass(parentType,
        MortalLogger.NULL, uiBinderCtx);
    assertEquals(parentType, parentClass.getRawType());

    Map<String, Pair<JMethod, Integer>> childMethods = parentClass.getUiChildMethods();
    assertNotNull(childMethods);
    assertEquals(0, childMethods.size());

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Class with {@link UiChild}-annotated methods.
   */
  @SuppressWarnings("unused")
  // We know these methods are unused
  private static class UiChildWithPoorMethodNames {
    public UiChildWithPoorMethodNames() {
      throw new UnsupportedOperationException("Should never get called");
    }

    @UiChild
    void poorlyNamedMethodWithoutTag(Object child) {
      throw new UnsupportedOperationException("Should never get called");
    }
  }

  public void testOwnerFieldClass_withBadlyNamedMethod() {
    JClassType parentType = gwtTypeAdapter.adaptJavaClass(UiChildWithPoorMethodNames.class);
    try {
      OwnerFieldClass.getFieldClass(parentType, MortalLogger.NULL, uiBinderCtx);
      fail("Class should error because @UiChild method has invalid name (and no tag specified).");
    } catch (UnableToCompleteException expected) {
      gwtTypeAdapter.verifyAll();
    }
  }

  /**
   * Class with a {@link UiConstructor}-annotated constructor.
   */
  @SuppressWarnings("unused")
  // We know these methods are unused
  private static class UiConstructorClass {
    @UiConstructor
    public UiConstructorClass(boolean visible) {
      throw new UnsupportedOperationException("Should never get called");
    }
  }

  public void testOwnerFieldClass_withUiConstructor() throws Exception {
    JClassType constructorsType = gwtTypeAdapter.adaptJavaClass(UiConstructorClass.class);
    OwnerFieldClass constructorsClass = OwnerFieldClass.getFieldClass(
        constructorsType, MortalLogger.NULL, uiBinderCtx);
    assertEquals(constructorsType, constructorsClass.getRawType());

    JConstructor constructor = constructorsClass.getUiConstructor();
    assertNotNull(constructor);
    assertEquals(constructorsType, constructor.getEnclosingType());

    JParameter[] parameters = constructor.getParameters();
    assertEquals(1, parameters.length);
    assertEquals(JPrimitiveType.BOOLEAN, parameters[0].getType());

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Class with (disallowed) multiple constructors annotated with
   * {@link UiConstructor}.
   */
  @SuppressWarnings("unused")
  // We know these methods are unused
  private static class MultiUiConstructorsClass {
    @UiConstructor
    public MultiUiConstructorsClass(boolean visible) {
      throw new UnsupportedOperationException("Should never get called");
    }

    @UiConstructor
    public MultiUiConstructorsClass(String size) {
      throw new UnsupportedOperationException("Should never get called");
    }
  }

  public void testOwnerFieldClass_withMultipleUiConstructors() {
    JClassType constructorsType = gwtTypeAdapter.adaptJavaClass(MultiUiConstructorsClass.class);

    try {
      OwnerFieldClass.getFieldClass(constructorsType, MortalLogger.NULL,
          uiBinderCtx);
      fail("Expected exception not thrown");
    } catch (UnableToCompleteException utce) {
      // Expected
    }
  }

  /**
   * Asserts that the given method has the proper name and parameters.
   *
   * @param method the actual method
   * @param methodName the expected method name
   * @param parameterTypes the expected parameter types
   */
  private void assertMethod(JMethod method, String methodName,
      JType... parameterTypes) {
    assertNotNull(method);
    assertEquals(methodName, method.getName());
    JParameter[] parameters = method.getParameters();
    assertEquals(parameterTypes.length, parameters.length);
    for (int i = 0; i < parameters.length; i++) {
      assertEquals("Parameter " + i + " of method " + methodName
          + " mismatch. Expected" + parameterTypes[i].getSimpleSourceName()
          + "; actual: " + parameters[i].getType().getSimpleSourceName(),
          parameterTypes[i], parameters[i].getType());
    }
  }
}
