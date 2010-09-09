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
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.rebind.JClassTypeAdapter;
import com.google.gwt.uibinder.rebind.MortalLogger;
import com.google.gwt.uibinder.rebind.UiBinderContext;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;

import junit.framework.TestCase;

/**
 * Tests for the owner field descriptor.
 */
public class OwnerFieldTest extends TestCase {

  private JClassTypeAdapter gwtTypeAdapter;
  private JClassType ownerType;
  private UiBinderContext uiBinderCtx;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    uiBinderCtx = new UiBinderContext();
    gwtTypeAdapter = new JClassTypeAdapter();
    ownerType = gwtTypeAdapter.adaptJavaClass(this.getClass());
  }

  // Fields for testing

  @UiField
  Label someField;

  @UiField(provided = true)
  Button providedField;

  @UiField
  int badTypeField;

  Label nonAnnotatedField;

  public void testOwnerField() throws Exception {
    JClassType labelType = gwtTypeAdapter.adaptJavaClass(Label.class);
    JClassType buttonType = gwtTypeAdapter.adaptJavaClass(Button.class);

    JField someGwtField = gwtTypeAdapter.adaptField(
        this.getClass().getDeclaredField("someField"), ownerType);
    OwnerField someOwnerField = new OwnerField(someGwtField, MortalLogger.NULL,
        uiBinderCtx);
    assertEquals("someField", someOwnerField.getName());
    assertEquals(labelType, someOwnerField.getType().getRawType());
    assertFalse(someOwnerField.isProvided());

    JField providedGwtField = gwtTypeAdapter.adaptField(
        this.getClass().getDeclaredField("providedField"), ownerType);
    OwnerField providedOwnerField = new OwnerField(providedGwtField,
        MortalLogger.NULL, uiBinderCtx);
    assertEquals("providedField", providedOwnerField.getName());
    assertEquals(buttonType, providedOwnerField.getType().getRawType());
    assertTrue(providedOwnerField.isProvided());

    gwtTypeAdapter.verifyAll();
  }

  public void testOwnerField_badFieldType() throws Exception {
    JField someGwtField = gwtTypeAdapter.adaptField(
        this.getClass().getDeclaredField("badTypeField"), ownerType);
    try {
      new OwnerField(someGwtField, MortalLogger.NULL, uiBinderCtx);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    gwtTypeAdapter.verifyAll();
  }

  public void testOwnerField_missingAnnotation() throws Exception {
    JField someGwtField = gwtTypeAdapter.adaptField(
        this.getClass().getDeclaredField("nonAnnotatedField"), ownerType);
    try {
      new OwnerField(someGwtField, MortalLogger.NULL, uiBinderCtx);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }

    gwtTypeAdapter.verifyAll();
  }
}
