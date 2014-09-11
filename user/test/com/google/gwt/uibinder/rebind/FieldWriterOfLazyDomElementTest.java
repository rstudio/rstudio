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
package com.google.gwt.uibinder.rebind;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.uibinder.rebind.model.OwnerField;

import junit.framework.TestCase;

/**
 * Tests for FieldWriterOfLazyDomElement.
 */
public class FieldWriterOfLazyDomElementTest extends TestCase {

  private static final String FIELD_NAME = "field_name";
  private static final String QUALIFIED_SOURCE_NAME = "qualified_source_name";
  private static final String ARG_QUALIFIED_SOURCE_NAME = "arg_qualified_source_name";

  private JClassType templateFieldType;
  private OwnerField ownerField;
  private JClassType ownerFieldType;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    templateFieldType = mock(JClassType.class);
    ownerField = mock(OwnerField.class);
    ownerFieldType = mock(JClassType.class);

    when(ownerField.getName()).thenReturn(FIELD_NAME);
    when(ownerField.getRawType()).thenReturn(ownerFieldType);
  }

  /**
   * Not parameterized LazyDomElement must fail. Example:
   * <pre>
   *   @UiField LazyDomElement el;
   * </pre>
   */
  public void testLazyDomElementNotParameterized() throws Exception {
    when(ownerFieldType.isParameterized()).thenReturn(null);

    try {
      FieldWriter field = new FieldWriterOfLazyDomElement(null,
          templateFieldType, ownerField, MortalLogger.NULL);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }
  }

  /**
   * LazyDomElement has parameter but it's not assignable to the template field
   * type. Example:
   * <pre>
   *   @UiField LazyDomElement&lt;DivElement&gt; el;
   * </pre>
   *
   * but in the template 'el' is defined as:
   * <pre>
   *   &lt;span ui:field='el' /&gt;
   * </pre>
   */
  public void testLazyDomElementIncompatibleParameter() throws Exception {
    JParameterizedType parameterClass = mock(JParameterizedType.class);
    when(ownerFieldType.isParameterized()).thenReturn(parameterClass);

    JClassType arg = mock(JClassType.class);
    when(parameterClass.getTypeArgs()).thenReturn(new JClassType[] { arg });

    when(templateFieldType.isAssignableTo(arg)).thenReturn(false);
    when(parameterClass.getQualifiedSourceName()).thenReturn(QUALIFIED_SOURCE_NAME);

    try {
      new FieldWriterOfLazyDomElement(null,
          templateFieldType, ownerField, MortalLogger.NULL);
      fail("Expected exception not thrown.");
    } catch (UnableToCompleteException utce) {
      // Expected
    }
  }

  /**
   * The success test, everything works fine.
   */
  public void testLazyDomElementCompatibleType() throws Exception {
    JParameterizedType parameterClass = mock(JParameterizedType.class);
    when(ownerFieldType.isParameterized()).thenReturn(parameterClass);

    JClassType arg = mock(JClassType.class);
    when(parameterClass.getTypeArgs()).thenReturn(new JClassType[] { arg });

    when(templateFieldType.isAssignableTo(arg)).thenReturn(true);

    when(parameterClass.getQualifiedSourceName()).thenReturn(QUALIFIED_SOURCE_NAME);
    when(arg.getQualifiedSourceName()).thenReturn(ARG_QUALIFIED_SOURCE_NAME);

    FieldWriter field = new FieldWriterOfLazyDomElement(null,
        templateFieldType, ownerField, MortalLogger.NULL);
    assertSame(parameterClass, field.getAssignableType());
    assertSame(parameterClass, field.getInstantiableType());
    assertEquals(QUALIFIED_SOURCE_NAME + "<" + ARG_QUALIFIED_SOURCE_NAME + ">",
      field.getQualifiedSourceName());
  }
}
