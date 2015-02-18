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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.gss.ResourceUrlFunction.MethodByPathHelper;
import com.google.gwt.resources.gss.ast.CssJavaExpressionNode;
import com.google.gwt.thirdparty.common.css.SourceCodeLocation;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssFunctionArgumentsNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssFunctionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunctionException;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.util.List;

/**
 * Test for {@link ResourceUrlFunction}.
 */
public class ResourceUrlFunctionTest extends TestCase {
  private static String JAVA_EXPRESSION_PATTERN = "ResourceUrlFunctionTest.this.%s.getSafeUri()" +
      ".asString()";
  private ResourceContext resourceContext;
  private ErrorManager errorManager;
  private MethodByPathHelper methodByPathHelper;
  private ResourceUrlFunction resourceUrlFunction;
  private JClassType dataResourceType;
  private JClassType imageResourceType;

  @Override
  protected void setUp() {
    errorManager = mock(ErrorManager.class);
    methodByPathHelper = mock(MethodByPathHelper.class);
    dataResourceType = mock(JClassType.class);
    imageResourceType = mock(JClassType.class);
    resourceContext = mockResourceContext();

    resourceUrlFunction = new ResourceUrlFunction(resourceContext, methodByPathHelper);
  }

  public void testValidImageResource() throws GssFunctionException {
    // given
    List<CssValueNode> input = createInput("image");
    when(dataResourceType.isAssignableFrom(any(JClassType.class))).thenReturn(false);
    when(imageResourceType.isAssignableFrom(any(JClassType.class))).thenReturn(true);

    // when
    List<CssValueNode> result = resourceUrlFunction.getCallResultNodes(input, errorManager);

    // then
    assertResultIsValid(result, JAVA_EXPRESSION_PATTERN.replace("%s", "image()"));
  }

  public void testValidDataResource() throws GssFunctionException {
    // given
    List<CssValueNode> input = createInput("data");
    when(dataResourceType.isAssignableFrom(any(JClassType.class))).thenReturn(true);
    when(imageResourceType.isAssignableFrom(any(JClassType.class))).thenReturn(false);

    // when
    List<CssValueNode> result = resourceUrlFunction.getCallResultNodes(input, errorManager);

    // then
    assertResultIsValid(result, JAVA_EXPRESSION_PATTERN.replace("%s", "data()"));
  }

  public void testMultiplePath() throws GssFunctionException {
    // given
    List<CssValueNode> input = createInput("method1.method2.resource");
    when(dataResourceType.isAssignableFrom(any(JClassType.class))).thenReturn(true);

    // when
    List<CssValueNode> result = resourceUrlFunction.getCallResultNodes(input, errorManager);

    // then
    assertResultIsValid(result, JAVA_EXPRESSION_PATTERN.replace("%s",
        "method1().method2().resource()"));
  }

  public void testInvalidResource() {
    // given
    List<CssValueNode> input = createInput("invalidResource");
    when(dataResourceType.isAssignableFrom(any(JClassType.class))).thenReturn(false);
    when(imageResourceType.isAssignableFrom(any(JClassType.class))).thenReturn(false);

    // when
    try {
      resourceUrlFunction.getCallResultNodes(input, errorManager);
    } catch (GssFunctionException expected) {
      // then
      verify(errorManager).report(any(GssError.class));
      return;
    }

    fail("GssFunctionException expected");
  }

  public void testInvalidPath() throws NotFoundException {
    // given
    List<CssValueNode> input = createInput("invalid.path");
    when(methodByPathHelper.getReturnType(any(ResourceContext.class),
        anyList())).thenThrow(NotFoundException.class);

    // when
    try {
      resourceUrlFunction.getCallResultNodes(input, errorManager);
    } catch (GssFunctionException expected) {
      // then
      verify(errorManager).report(any(GssError.class));
      return;
    }

    fail("GssFunctionException expected");
  }

  private void assertResultIsValid(List<CssValueNode> result, String expectedJavaExpression) {
    assertEquals(1, result.size());
    assertTrue(result.get(0) instanceof CssFunctionNode);

    CssFunctionArgumentsNode arguments = ((CssFunctionNode) result.get(0)).getArguments();
    assertEquals(1, arguments.numChildren());
    assertTrue(arguments.getChildAt(0) instanceof CssJavaExpressionNode);

    CssJavaExpressionNode javaExpressionNode = (CssJavaExpressionNode) arguments.getChildAt(0);
    assertEquals(expectedJavaExpression, javaExpressionNode.getValue());

    verify(errorManager, never()).report(any(GssError.class));
    verify(errorManager, never()).reportWarning(any(GssError.class));
  }

  private List<CssValueNode> createInput(String value) {
    CssValueNode input = mock(CssValueNode.class);
    when(input.getValue()).thenReturn(value);

    SourceCodeLocation sourceCodeLocation = mock(SourceCodeLocation.class);
    when(input.getSourceCodeLocation()).thenReturn(sourceCodeLocation);

    return ImmutableList.of(input);
  }

  private ResourceContext mockResourceContext() {
    ResourceContext context = mock(ResourceContext.class);
    when(context.getImplementationSimpleSourceName()).thenReturn("ResourceUrlFunctionTest");
    GeneratorContext generatorContext = mock(GeneratorContext.class);
    TypeOracle oracle = mock(TypeOracle.class);

    when(oracle.findType(DataResource.class.getCanonicalName())).thenReturn(dataResourceType);
    when(oracle.findType(ImageResource.class.getCanonicalName())).thenReturn(imageResourceType);

    when(generatorContext.getTypeOracle()).thenReturn(oracle);
    when(context.getGeneratorContext()).thenReturn(generatorContext);
    return context;
  }
}
