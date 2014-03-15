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

import static org.easymock.EasyMock.expect;

import com.google.gwt.core.ext.typeinfo.JClassType;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;

/**
 * Tests for FieldWriterOfExistingType.
 */
public class FieldWriterOfExistingTypeTest extends TestCase {

  private static final String FIELD_NAME = "field_name";
  private static final String QUALIFIED_SOURCE_NAME = "qualified_source_name";

  private IMocksControl control;

  private JClassType type;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    control = EasyMock.createControl();

    type = control.createMock(JClassType.class);
  }

  /**
   * Null type not allowed, must fail.
   */
  public void testNullType() throws Exception {
    control.replay();
    try {
      FieldWriter field = new FieldWriterOfExistingType(null,
          FieldWriterType.DEFAULT, null, FIELD_NAME, MortalLogger.NULL);
      fail("Expected exception not thrown.");
    } catch (IllegalArgumentException e) {
      // Expected
    }
    control.verify();
  }

  public void testType() throws Exception {
    expect(type.getQualifiedSourceName()).andReturn(QUALIFIED_SOURCE_NAME);

    control.replay();
    FieldWriter field = new FieldWriterOfExistingType(null,
        FieldWriterType.DEFAULT, type, FIELD_NAME, MortalLogger.NULL);

    assertSame(type, field.getAssignableType());
    assertSame(type, field.getInstantiableType());
    assertEquals(QUALIFIED_SOURCE_NAME, field.getQualifiedSourceName());
    control.verify();
  }
}
