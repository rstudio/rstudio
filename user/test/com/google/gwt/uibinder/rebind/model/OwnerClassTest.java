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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.rebind.DummyMortalLogger;
import com.google.gwt.uibinder.rebind.JClassTypeAdapter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for the owner class descriptor.
 */
public class OwnerClassTest extends TestCase {

  private JClassTypeAdapter gwtTypeAdapter;
  private MortalLogger logger;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    logger = new DummyMortalLogger();
    gwtTypeAdapter = new JClassTypeAdapter();
  }

  /**
   * Empty uibinder class for sanity checking.
   */
  private static class EmptyOwnerClass { }

  @SuppressWarnings("deprecation")
  public void testOwnerClass_empty() throws Exception {
    JClassType ownerType = gwtTypeAdapter.adaptJavaClass(EmptyOwnerClass.class);
    JClassType labelType = gwtTypeAdapter.adaptJavaClass(Label.class);
    OwnerClass ownerClass = new OwnerClass(ownerType, logger);

    assertNull(ownerClass.getUiFactoryMethod(labelType));
    assertNull(ownerClass.getUiField("fieldName"));
    assertNull(ownerClass.getUiFieldForType(labelType));
    assertTrue(ownerClass.getUiFields().isEmpty());
    assertTrue(ownerClass.getUiHandlers().isEmpty());

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Uibinder class for testing of {@link UiFactory}.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class UiFactoryClass {
    @UiFactory
    Label createLabel() {
      throw new UnsupportedOperationException("Should never be called");
    }
  }

  public void testOwnerClass_uiFactory() throws Exception {
    JClassType ownerType = gwtTypeAdapter.adaptJavaClass(UiFactoryClass.class);
    JClassType labelType = gwtTypeAdapter.adaptJavaClass(Label.class);
    OwnerClass ownerClass = new OwnerClass(ownerType, logger);

    JMethod uiFactoryMethod = ownerClass.getUiFactoryMethod(labelType);
    assertNotNull(uiFactoryMethod);
    assertEquals("createLabel", uiFactoryMethod.getName());
    assertEquals(labelType, uiFactoryMethod.getReturnType());
    JParameter[] parameters = uiFactoryMethod.getParameters();
    assertNotNull(parameters);
    assertEquals(0, parameters.length);

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Uibinder class for testing bad usage of {@link UiFactory}.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class BadUiFactoryClass {
    @UiFactory
    int thisShouldntWork() {
      throw new UnsupportedOperationException("Should never be called");
    }
  }

  public void testOwnerClass_uiFactoryBadType() {
    JClassType ownerType = gwtTypeAdapter.adaptJavaClass(BadUiFactoryClass.class);
    try {
      new OwnerClass(ownerType, logger);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Uibinder class for testing bad usage of {@link UiFactory}.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class DuplicateUiFactoryClass {
    @UiFactory
    Label labelFactory1() {
      throw new UnsupportedOperationException("Should never be called");
    }

    @UiFactory
    Label labelFactory2() {
      throw new UnsupportedOperationException("Should never be called");
    }
  }

  public void testOwnerClass_uiFactoryDuplicateType() {
    JClassType ownerType = gwtTypeAdapter.adaptJavaClass(DuplicateUiFactoryClass.class);
    try {
      new OwnerClass(ownerType, logger);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Uibinder class for testing of {@link UiField}.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class UiFieldsClass {
    @UiField
    Label label1;

    @UiField(provided = true)
    Button button1;
  }

  @SuppressWarnings("deprecation")
  public void testOwnerClass_uiFields() throws Exception {
    JClassType ownerType = gwtTypeAdapter.adaptJavaClass(UiFieldsClass.class);
    JClassType labelType = gwtTypeAdapter.adaptJavaClass(Label.class);
    JClassType buttonType = gwtTypeAdapter.adaptJavaClass(Button.class);
    OwnerClass ownerClass = new OwnerClass(ownerType, logger);

    OwnerField labelField = ownerClass.getUiField("label1");
    OwnerField labelField2 = ownerClass.getUiFieldForType(labelType);
    assertNotNull(labelField);
    assertNotNull(labelField2);
    assertEquals(labelField, labelField2);
    assertFalse(labelField.isProvided());
    assertEquals(labelType, labelField.getType().getRawType());
    assertEquals("label1", labelField.getName());

    OwnerField buttonField = ownerClass.getUiField("button1");
    OwnerField buttonField2 = ownerClass.getUiFieldForType(buttonType);
    assertNotNull(buttonField);
    assertNotNull(buttonField2);
    assertEquals(buttonField, buttonField2);
    assertTrue(buttonField.isProvided());
    assertEquals(buttonType, buttonField.getType().getRawType());
    assertEquals("button1", buttonField.getName());

    Collection<OwnerField> uiFields = ownerClass.getUiFields();
    Set<OwnerField> uiFieldSet = new HashSet<OwnerField>(uiFields);
    Set<OwnerField> expectedFieldSet = new HashSet<OwnerField>();
    expectedFieldSet.add(labelField);
    expectedFieldSet.add(buttonField);
    assertEquals(expectedFieldSet, uiFieldSet);

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Uibinder class for testing bad usage of {@link UiField}.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class BadUiFieldsClass {
    @UiField
    int thisShouldntWork;
  }

  public void testOwnerClass_uiFieldsBadType() {
    JClassType ownerType = gwtTypeAdapter.adaptJavaClass(BadUiFieldsClass.class);
    try {
      new OwnerClass(ownerType, logger);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Uibinder class for testing of {@link UiHandler}.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class UiHandlersClass {
    @UiHandler("myField")
    void onMyFieldClicked(ClickEvent ev) {
      throw new UnsupportedOperationException("Should never be called");
    }

    @UiHandler({"myField", "myOtherField"})
    void onMouseOver(MouseOverEvent ev) {
      throw new UnsupportedOperationException("Should never be called");
    }
  }

  public void testOwnerClass_uiHandlers() throws Exception {
    JClassType ownerType = gwtTypeAdapter.adaptJavaClass(UiHandlersClass.class);
    OwnerClass ownerClass = new OwnerClass(ownerType, logger);

    // Assert the two expected handlers are there
    List<JMethod> uiHandlers = ownerClass.getUiHandlers();
    assertEquals(2, uiHandlers.size());
    JMethod clickMethod = null,
        mouseOverMethod = null;

    // Don't care about ordering
    for (JMethod method : uiHandlers) {
      if (method.getName().equals("onMyFieldClicked")) {
        clickMethod = method;
      } else if (method.getName().equals("onMouseOver")) {
        mouseOverMethod = method;
      }
    }

    assertNotNull(clickMethod);
    assertNotNull(mouseOverMethod);

    // Check the click handler
    JClassType clickEventType = gwtTypeAdapter.adaptJavaClass(ClickEvent.class);
    JParameter[] clickParams = clickMethod.getParameters();
    assertEquals(1, clickParams.length);
    assertEquals(clickEventType, clickParams[0].getType());
    assertTrue(clickMethod.isAnnotationPresent(UiHandler.class));
    UiHandler clickAnnotation = clickMethod.getAnnotation(UiHandler.class);
    String[] clickFields = clickAnnotation.value();
    assertEquals(1, clickFields.length);
    assertEquals("myField", clickFields[0]);

    // Check the mouse over handler
    JClassType mouseOverEventType = gwtTypeAdapter.adaptJavaClass(MouseOverEvent.class);
    JParameter[] mouseOverParams = mouseOverMethod.getParameters();
    assertEquals(1, mouseOverParams.length);
    assertEquals(mouseOverEventType, mouseOverParams[0].getType());
    assertTrue(mouseOverMethod.isAnnotationPresent(UiHandler.class));
    UiHandler mouseOverAnnotation = mouseOverMethod.getAnnotation(UiHandler.class);
    String[] mouseOverFields = mouseOverAnnotation.value();
    assertEquals(2, mouseOverFields.length);
    assertEquals("myField", mouseOverFields[0]);
    assertEquals("myOtherField", mouseOverFields[1]);

    gwtTypeAdapter.verifyAll();
  }

  /**
   * Parent class for testing inheritance of owner classes.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class ParentUiBinderClass {
    @UiField
    Label label1;

    @UiFactory
    Label createLabel() {
      throw new UnsupportedOperationException("Should never be called");
    }

    @UiHandler("label1")
    void onLabelMouseOver(MouseOverEvent ev) {
      throw new UnsupportedOperationException("Should never be called");
    }
  }

  /**
   * Child class for testing inheritance of owner classes.
   */
  @SuppressWarnings("unused") // We know these methods are unused
  private static class ChildUiBinderClass extends ParentUiBinderClass {
    @UiField(provided = true)
    Button button1;

    @UiFactory
    Button createButton() {
      throw new UnsupportedOperationException("Should never be called");
    }

    @UiHandler("button1")
    void onButtonClicked(ClickEvent ev) {
      throw new UnsupportedOperationException("Should never be called");
    }
  }

  @SuppressWarnings("deprecation")
  public void testOwnerClass_withParent() throws Exception {
    JClassType ownerType =
        gwtTypeAdapter.adaptJavaClass(ChildUiBinderClass.class);
    JClassType labelType = gwtTypeAdapter.adaptJavaClass(Label.class);
    JClassType buttonType = gwtTypeAdapter.adaptJavaClass(Button.class);
    OwnerClass ownerClass = new OwnerClass(ownerType, logger);

    // Test fields
    OwnerField labelField = ownerClass.getUiField("label1");
    OwnerField labelField2 = ownerClass.getUiFieldForType(labelType);
    assertNotNull(labelField);
    assertNotNull(labelField2);
    assertEquals(labelField, labelField2);
    assertFalse(labelField.isProvided());
    assertEquals(labelType, labelField.getType().getRawType());
    assertEquals("label1", labelField.getName());

    OwnerField buttonField = ownerClass.getUiField("button1");
    OwnerField buttonField2 = ownerClass.getUiFieldForType(buttonType);
    assertNotNull(buttonField);
    assertNotNull(buttonField2);
    assertEquals(buttonField, buttonField2);
    assertTrue(buttonField.isProvided());
    assertEquals(buttonType, buttonField.getType().getRawType());
    assertEquals("button1", buttonField.getName());

    Collection<OwnerField> uiFields = ownerClass.getUiFields();
    Set<OwnerField> uiFieldSet = new HashSet<OwnerField>(uiFields);
    Set<OwnerField> expectedFieldSet = new HashSet<OwnerField>();
    expectedFieldSet.add(labelField);
    expectedFieldSet.add(buttonField);
    assertEquals(expectedFieldSet, uiFieldSet);

    // Test factories
    JMethod labelFactoryMethod = ownerClass.getUiFactoryMethod(labelType);
    assertNotNull(labelFactoryMethod);
    assertEquals("createLabel", labelFactoryMethod.getName());
    assertEquals(labelType, labelFactoryMethod.getReturnType());
    JParameter[] labelParams = labelFactoryMethod.getParameters();
    assertNotNull(labelParams);
    assertEquals(0, labelParams.length);

    JMethod buttonFactoryMethod = ownerClass.getUiFactoryMethod(labelType);
    assertNotNull(buttonFactoryMethod);
    assertEquals("createLabel", buttonFactoryMethod.getName());
    assertEquals(labelType, buttonFactoryMethod.getReturnType());
    JParameter[] buttonParams = buttonFactoryMethod.getParameters();
    assertNotNull(buttonParams);
    assertEquals(0, buttonParams.length);

    // Test handlers
    List<JMethod> uiHandlers = ownerClass.getUiHandlers();
    assertEquals(2, uiHandlers.size());
    JMethod clickMethod = null,
        mouseOverMethod = null;

    for (JMethod method : uiHandlers) {
      if (method.getName().equals("onButtonClicked")) {
        clickMethod = method;
      } else if (method.getName().equals("onLabelMouseOver")) {
        mouseOverMethod = method;
      }
    }

    assertNotNull(clickMethod);
    assertNotNull(mouseOverMethod);

    JClassType clickEventType = gwtTypeAdapter.adaptJavaClass(ClickEvent.class);
    JParameter[] clickParams = clickMethod.getParameters();
    assertEquals(1, clickParams.length);
    assertEquals(clickEventType, clickParams[0].getType());
    assertTrue(clickMethod.isAnnotationPresent(UiHandler.class));
    UiHandler clickAnnotation = clickMethod.getAnnotation(UiHandler.class);
    String[] clickFields = clickAnnotation.value();
    assertEquals(1, clickFields.length);
    assertEquals("button1", clickFields[0]);

    JClassType mouseOverEventType = gwtTypeAdapter.adaptJavaClass(MouseOverEvent.class);
    JParameter[] mouseOverParams = mouseOverMethod.getParameters();
    assertEquals(1, mouseOverParams.length);
    assertEquals(mouseOverEventType, mouseOverParams[0].getType());
    assertTrue(mouseOverMethod.isAnnotationPresent(UiHandler.class));
    UiHandler mouseOverAnnotation = mouseOverMethod.getAnnotation(UiHandler.class);
    String[] mouseOverFields = mouseOverAnnotation.value();
    assertEquals(1, mouseOverFields.length);
    assertEquals("label1", mouseOverFields[0]);

    gwtTypeAdapter.verifyAll();
  }
}
