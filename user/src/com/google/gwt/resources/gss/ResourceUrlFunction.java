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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.resources.gss.ast.CssDotPathNode;
import com.google.gwt.resources.gss.ast.CssJavaExpressionNode;
import com.google.gwt.thirdparty.common.css.SourceCodeLocation;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssFunctionArgumentsNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssFunctionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunction;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunctionException;
import com.google.gwt.thirdparty.common.css.compiler.gssfunctions.GssFunctions;
import com.google.gwt.thirdparty.guava.common.annotations.VisibleForTesting;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;

import java.util.List;

/**
 * Gss function that create the needed nodes in order to correctly get the url of a resource.
 */
public class ResourceUrlFunction implements GssFunction {

  @VisibleForTesting
  interface MethodByPathHelper {
    JType getReturnType(ResourceContext context, List<String> pathElements)
        throws NotFoundException;
  }

  private static class MethodByPathHelperImpl implements MethodByPathHelper {
    @Override
    public JType getReturnType(ResourceContext context, List<String> pathElements)
        throws NotFoundException {
      return ResourceGeneratorUtil.getMethodByPath(context.getClientBundleType(),
          pathElements, null).getReturnType();
    }
  }

  private final ResourceContext context;
  private final MethodByPathHelper methodByPathHelper;
  private final JClassType dataResourceType;
  private final JClassType imageResourceType;

  public static String getName() {
    return "resourceUrl";
  }

  public ResourceUrlFunction(ResourceContext context) {
    this(context, new MethodByPathHelperImpl());
  }

  @VisibleForTesting
  ResourceUrlFunction(ResourceContext context, MethodByPathHelper methodByPathHelper) {
    this.context = context;
    this.methodByPathHelper = methodByPathHelper;
    this.dataResourceType = context.getGeneratorContext().getTypeOracle()
        .findType(DataResource.class.getCanonicalName());
    this.imageResourceType = context.getGeneratorContext().getTypeOracle()
        .findType(ImageResource.class.getCanonicalName());
  }

  @Override
  public Integer getNumExpectedArguments() {
    return 1;
  }

  @Override
  public List<CssValueNode> getCallResultNodes(List<CssValueNode> cssValueNodes,
      ErrorManager errorManager) throws GssFunctionException {
    CssValueNode functionToEval = cssValueNodes.get(0);
    String value = functionToEval.getValue();
    SourceCodeLocation location = functionToEval.getSourceCodeLocation();

    String javaExpression = buildJavaExpression(value, location, errorManager);

    CssFunctionNode urlNode = buildUrlNode(javaExpression, location);

    return ImmutableList.<CssValueNode>of(urlNode);
  }

  @Override
  public String getCallResultString(List<String> strings) throws GssFunctionException {
    return strings.get(0);
  }

  private String buildJavaExpression(String value, SourceCodeLocation location,
      ErrorManager errorManager) throws GssFunctionException {
    CssDotPathNode dotPathValue = new CssDotPathNode(value, "", "", location);

    assertMethodIsValidResource(location, dotPathValue.getPathElements(), errorManager);

    return context.getImplementationSimpleSourceName() + ".this."
        + dotPathValue.getValue() + ".getSafeUri().asString()";
  }

  private void assertMethodIsValidResource(SourceCodeLocation location, List<String> pathElements,
      ErrorManager errorManager) throws GssFunctionException {
    JType methodType;

    try {
      methodType = methodByPathHelper.getReturnType(context, pathElements);
    } catch (NotFoundException e) {
      String message = e.getMessage() != null ? e.getMessage() : "Invalid path";
      errorManager.report(new GssError(message, location));
      throw new GssFunctionException(message, e);
    }

    if (!dataResourceType.isAssignableFrom((JClassType) methodType) &&
        !imageResourceType.isAssignableFrom((JClassType) methodType)) {
      String message = "Invalid method type for url substitution: " + methodType + ". " +
          "Only DataResource and ImageResource are supported.";
      errorManager.report(new GssError(message, location));
      throw new GssFunctionException(message);
    }
  }

  private CssFunctionNode buildUrlNode(String javaExpression, SourceCodeLocation location) {
    CssFunctionNode urlNode = GssFunctions.createUrlNode("", location);
    CssJavaExpressionNode cssJavaExpressionNode = new CssJavaExpressionNode(javaExpression);
    CssFunctionArgumentsNode arguments =
        new CssFunctionArgumentsNode(ImmutableList.<CssValueNode>of(cssJavaExpressionNode));
    urlNode.setArguments(arguments);

    return urlNode;
  }
}
