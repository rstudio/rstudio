/*
 * Copyright 2014 Google Inc.
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

package com.google.gwt.resources.converter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;

/**
 * Test for {@link FontFamilyVisitor}.
 */
public class FontFamilyVisitorTest extends TestCase {

  private CssProperty cssProperty;

  @Override
  protected void setUp() throws Exception {
    cssProperty = mock(CssProperty.class);
  }

  public void testVisit() {
    // given
    when(cssProperty.getName()).thenReturn("font-family");

    ListValue values = mock(ListValue.class);
    when(values.toCss()).thenReturn("Arial, Times New Roman, Georgia,'Sans Serif', Trebuchet MS, " +
        "\"Comic Sans MS\"");
    when(cssProperty.getValues()).thenReturn(values);

    // when
    FontFamilyVisitor fontFamilyVisitor = new FontFamilyVisitor();
    fontFamilyVisitor.visit(cssProperty, null);

    // then
    ArgumentCaptor<Value> valueCaptor = ArgumentCaptor.forClass(Value.class);
    verify(cssProperty).setValue(valueCaptor.capture());
    assertEquals("Arial,'Times New Roman',Georgia,'Sans Serif','Trebuchet MS',\\\"Comic Sans MS\\\"",
        valueCaptor.getValue().toCss());
  }
}
