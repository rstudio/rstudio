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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssSprite;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for {@link AlternateAnnotationCreatorVisitor}.
 */
public class AlternateAnnotationCreatorVisitorTest extends TestCase {

  private CssRule cssRule;

  private CssSprite cssSprite;

  @Override
  protected void setUp() throws Exception {
    cssRule = mock(CssRule.class);
    cssSprite = mock(CssSprite.class);
  }

  public void testVisitCssRule() {
    // given
    // name1 is duplicated (2 times), name3 is duplicated (3 times)
    List<CssProperty> properties = createPropertiesWithName("name0", "name1", "name2", "name1",
        "name3", "name4", "name3", "name3");
    when(cssRule.getProperties()).thenReturn(properties);

    // when
    AlternateAnnotationCreatorVisitor alternateAnnotationCreatorVisitor =
        new AlternateAnnotationCreatorVisitor();
    alternateAnnotationCreatorVisitor.visit(cssRule, null);

    // then
    propertiesName1AndName3AreFlaggedAsAlternateTheOthersNot(properties);
  }

  public void testVisitCssSprite() {
    // given
    // name1 is duplicated (2 times), name3 is duplicated (# times)
    List<CssProperty> properties = createPropertiesWithName("name0", "name1", "name2", "name1",
        "name3", "name4", "name3", "name3");
    when(cssSprite.getProperties()).thenReturn(properties);

    // when
    AlternateAnnotationCreatorVisitor alternateAnnotationCreatorVisitor =
        new AlternateAnnotationCreatorVisitor();
    alternateAnnotationCreatorVisitor.visit(cssSprite, null);

    // then
    propertiesName1AndName3AreFlaggedAsAlternateTheOthersNot(properties);
  }

  private void propertiesName1AndName3AreFlaggedAsAlternateTheOthersNot(List<CssProperty>
      properties) {
    // property with name "name0" has not been modified
    verify(properties.get(0), never()).setName(anyString());
    // properties with name "name1" have been modified but not the first one.
    verify(properties.get(1), never()).setName(anyString());
    verify(properties.get(3)).setName("/* @alternate */ " + "name1");
    // property with name "name2" has not been modified
    verify(properties.get(2), never()).setName(anyString());
    // properties with name "name3" have been modified but not the first one.
    verify(properties.get(4), never()).setName(anyString());
    verify(properties.get(6)).setName("/* @alternate */ " + "name3");
    verify(properties.get(7)).setName("/* @alternate */ " + "name3");
    // property with name "name4" has not been modified
    verify(properties.get(5), never()).setName(anyString());
  }

  private List<CssProperty> createPropertiesWithName(String... names) {
    List<CssProperty> properties = new ArrayList<CssProperty>(names.length);

    for (String name : names) {
      CssProperty property = mock(CssProperty.class);
      when(property.getName()).thenReturn(name);

      properties.add(property);
    }

    return properties;
  }
}
