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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssProperty.FunctionValue;
import com.google.gwt.resources.css.ast.CssProperty.IdentValue;
import com.google.gwt.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link UndefinedConstantVisitor}.
 */
public class UndefinedConstantVisitorTest extends TestCase {

  private CssRule cssRule;
  private TreeLogger treeLogger;

  @Override
  protected void setUp() throws Exception {
    cssRule = mock(CssRule.class);
    treeLogger = mock(TreeLogger.class);
  }

  public void testVisit_filterMsFilterFontFamily_shouldBeSkipped() {
    // given
    List<CssProperty> properties = createPropertiesWithNameAndIdentValue(
        Lists.newArrayList("filter", "-ms-filter", "font-family"),
        Lists.newArrayList("UPPERCASE", "UPPERCASE", "UPPERCASE"));
    when(cssRule.getProperties()).thenReturn(properties);

    // when
    UndefinedConstantVisitor undefinedConstantVisitor = new UndefinedConstantVisitor(
        Sets.<String>newHashSet(), false, treeLogger);
    undefinedConstantVisitor.visit(cssRule, null);

    // then
    for (CssProperty property : properties) {
      verify(property, never()).setValue(any(Value.class));
    }
  }

  public void testVisit_notLenientAndUnknownVariable_throwsException() {
    // given
    List<CssProperty> properties = createPropertiesWithNameAndIdentValue(
        Lists.newArrayList("name"),
        Lists.newArrayList("UPPERCASE"));
    when(cssRule.getProperties()).thenReturn(properties);

    // when
    UndefinedConstantVisitor undefinedConstantVisitor = new UndefinedConstantVisitor(
        Sets.<String>newHashSet(), false, treeLogger);

    try {
      undefinedConstantVisitor.visit(cssRule, null);
    } catch (Css2GssConversionException expected) {
      return;
    }

    fail("An Css2GssConversionException should have been thrown");
  }

  public void testVisit_lenientAndUnknownVariable_propertyUpdated() {
    // given
    List<CssProperty> properties = createPropertiesWithNameAndIdentValue(
        Lists.newArrayList("name"),
        Lists.newArrayList("UPPERCASE"));
    when(cssRule.getProperties()).thenReturn(properties);

    // when
    UndefinedConstantVisitor undefinedConstantVisitor = new UndefinedConstantVisitor(
        Sets.<String>newHashSet(), true, treeLogger);
    undefinedConstantVisitor.visit(cssRule, null);

    // then
    verifyPropertyValueIs(properties.get(0), "uppercase");
  }

  public void testVisit_knownVariable_propertyNotUpdated() {
    // given
    List<CssProperty> properties = createPropertiesWithNameAndIdentValue(
        Lists.newArrayList("name"),
        Lists.newArrayList("UPPERCASE"));
    when(cssRule.getProperties()).thenReturn(properties);

    // when
    UndefinedConstantVisitor undefinedConstantVisitor = new UndefinedConstantVisitor(
        Sets.newHashSet("UPPERCASE"), false, treeLogger);
    undefinedConstantVisitor.visit(cssRule, null);

    // then
    verifyPropertyValueIs(properties.get(0), "UPPERCASE");
  }

  public void testVisit_functionWithUnknownConstant_propertyUpdated() {
    // given
    CssProperty property = mock(CssProperty.class);
    when(property.getName()).thenReturn("name");

    List<Value> arguments = new ArrayList<Value>(5);
    for (int i = 0; i < 5; i++) {
      arguments.add(new IdentValue("ARGUMENT" + i));
    }

    ListValue listValue = new ListValue(arguments);
    FunctionValue function = new FunctionValue("fct", listValue);

    when(property.getValues()).thenReturn(new ListValue(Lists.<Value>newArrayList(function)));

    when(cssRule.getProperties()).thenReturn(Lists.newArrayList(property));

    // when
    UndefinedConstantVisitor undefinedConstantVisitor = new UndefinedConstantVisitor(
        Sets.newHashSet("ARGUMENT1", "ARGUMENT3"), true, treeLogger);
    undefinedConstantVisitor.visit(cssRule, null);

    // then
    verifyPropertyValueIs(property, "fct(argument0 ARGUMENT1 argument2 ARGUMENT3 argument4)");
  }

  private List<CssProperty> createPropertiesWithNameAndIdentValue(List<String> names,
      List<String> values) {
    assert names.size() == values.size();

    List<CssProperty> properties = new ArrayList<CssProperty>(names.size());

    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      CssProperty property = mock(CssProperty.class);
      when(property.getName()).thenReturn(name);

      String value = values.get(i);
      IdentValue identValue = new IdentValue(value);

      ListValue listValue = new ListValue(Lists.<Value>newArrayList(identValue));
      when(property.getValues()).thenReturn(listValue);

      properties.add(property);
    }

    return properties;
  }

  private void verifyPropertyValueIs(CssProperty property, String value) {
    ArgumentCaptor<Value> valueCaptor = ArgumentCaptor.forClass(Value.class);
    verify(property).setValue(valueCaptor.capture());
    assertEquals(value, valueCaptor.getValue().toCss());
  }
}
