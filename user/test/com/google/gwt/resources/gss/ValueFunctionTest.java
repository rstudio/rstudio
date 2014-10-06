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

package com.google.gwt.resources.gss;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gwt.resources.gss.ast.CssDotPathNode;
import com.google.gwt.thirdparty.common.css.SourceCodeLocation;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunctionException;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList.Builder;

import junit.framework.TestCase;

import java.util.List;

/**
 * Test for {@link ValueFunction}.
 */
public class ValueFunctionTest extends TestCase {

  public void testValueFunction() throws GssFunctionException {
    // given
    List<CssValueNode> input = createInput("image.width");

    // when
    List<CssValueNode> result = new ValueFunction().getCallResultNodes(input, null);

    // then
    assertResultIsValid(result, "image().width()");
  }

  public void testValueFunctionWithPrefix() throws GssFunctionException {
    // given
    List<CssValueNode> input = createInput("image.width", "px");

    // when
    List<CssValueNode> result = new ValueFunction().getCallResultNodes(input, null);

    // then
    assertResultIsValid(result, "image().width() + \"px\"");
  }

  public void testValueFunctionWithPrefixAndSuffix() throws GssFunctionException {
    // given
    List<CssValueNode> input = createInput("image.width", "px", "-");

    // when
    List<CssValueNode> result = new ValueFunction().getCallResultNodes(input, null);

    // then
    assertResultIsValid(result, "\"-\" + image().width() + \"px\"");
  }

  private void assertResultIsValid(List<CssValueNode> result, String expectedJavaExpression) {
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof CssDotPathNode);

    CssDotPathNode cssDotPathNode = (CssDotPathNode) result.get(0);

    assertEquals(expectedJavaExpression, cssDotPathNode.getValue());
  }

  private List<CssValueNode> createInput(String... argumentValue) {
    Builder<CssValueNode> listBuilder = ImmutableList.builder();

    for (String arg: argumentValue) {
      CssValueNode input = mock(CssValueNode.class);
      when(input.getValue()).thenReturn(arg);

      SourceCodeLocation sourceCodeLocation = mock(SourceCodeLocation.class);
      when(input.getSourceCodeLocation()).thenReturn(sourceCodeLocation);

      listBuilder.add(input);
    }

    return listBuilder.build();
  }
}
