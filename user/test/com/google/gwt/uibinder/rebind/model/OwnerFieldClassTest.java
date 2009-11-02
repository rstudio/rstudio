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
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.rebind.DummyMortalLogger;
import com.google.gwt.uibinder.rebind.JClassTypeAdapter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.user.client.ui.Label;

import junit.framework.TestCase;

/**
 * Tests for descriptors of potential owner field classes.
 */
public class OwnerFieldClassTest extends TestCase {

  private JClassTypeAdapter gwtTypeAdapter;
  private MortalLogger logger = new DummyMortalLogger();

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    gwtTypeAdapter = new JClassTypeAdapter();
  }

  public void testOwnerFieldClass() throws Exception {
    // Get the JType for a Label
    JClassType labelType = gwtTypeAdapter.adaptJavaClass(Label.class);

    // Now get its field class model
    OwnerFieldClass fieldClass = OwnerFieldClass.getFieldClass(labelType, logger);

    // Check the class model properties
    assertEquals(labelType, fieldClass.getRawType());
    assertNull(fieldClass.getUiConstructor());

    JMethod setter = fieldClass.getSetter("visible");
    assertMethod(setter, "setVisible", JPrimitiveType.BOOLEAN);

    // Check that the same instance of the model is returned if asked again
    assertSame(fieldClass, OwnerFieldClass.getFieldClass(labelType, logger));

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Class with lots of setters for testing.
   */
  @SuppressWarnings("unused") // We know these methods are unused
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

  public void testOwnerFieldClass_setters() throws Exception {
    JClassType settersType =
        gwtTypeAdapter.adaptJavaClass(SettersTestClass.class);
    JClassType stringType = gwtTypeAdapter.adaptJavaClass(String.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(settersType, logger);
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
    JClassType settersType =
        gwtTypeAdapter.adaptJavaClass(SettersTestClass.class);
    JClassType stringType = gwtTypeAdapter.adaptJavaClass(String.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(settersType, logger);
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
  @SuppressWarnings("unused") // We know these methods are unused
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
    JClassType settersType =
        gwtTypeAdapter.adaptJavaClass(OverriddenSettersTestClass.class);
    JClassType stringType = gwtTypeAdapter.adaptJavaClass(String.class);
    OwnerFieldClass settersClass = OwnerFieldClass.getFieldClass(settersType, logger);
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
   * Class with a {@link UiConstructor}-annotated constructor.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class UiConstructorClass {
    @UiConstructor
    public UiConstructorClass(boolean visible) {
      throw new UnsupportedOperationException("Should never get called");
    }
  }

  public void testOwnerFieldClass_withUiConstructor() throws Exception {
    JClassType constructorsType =
        gwtTypeAdapter.adaptJavaClass(UiConstructorClass.class);
    OwnerFieldClass constructorsClass =
        OwnerFieldClass.getFieldClass(constructorsType, logger);
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
  @SuppressWarnings("unused") // We know these methods are unused
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

  public void testOwnerFieldClass_withMultipleUiConstructors()
      throws Exception {
    JClassType constructorsType =
        gwtTypeAdapter.adaptJavaClass(MultiUiConstructorsClass.class);

    try {
      OwnerFieldClass.getFieldClass(constructorsType, logger);
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
