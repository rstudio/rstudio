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

import com.google.gwt.resources.gss.ast.CssJavaExpressionNode;
import com.google.gwt.thirdparty.common.css.SourceCodeLocation;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunction;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunctionException;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;

import java.util.List;

/**
 * GSS function that creates a {@link com.google.gwt.resources.gss.ast.CssJavaExpressionNode}
 * in order to evaluate a Java expression at runtime.
 */
public class EvalFunction implements GssFunction {

  public static String getName() {
    return "eval";
  }

  @Override
  public List<CssValueNode> getCallResultNodes(List<CssValueNode> args, ErrorManager errorManager)
      throws GssFunctionException {
    CssValueNode functionToEval = args.get(0);

    SourceCodeLocation sourceCodeLocation = extractSourceCodeLocation(functionToEval);

    CssJavaExpressionNode result = new CssJavaExpressionNode(functionToEval.getValue(),
        sourceCodeLocation);

    return ImmutableList.of((CssValueNode) result);
  }

  @Override
  public String getCallResultString(List<String> args) throws GssFunctionException {
    return args.get(0);
  }

  @Override
  public Integer getNumExpectedArguments() {
    return 1;
  }

  private SourceCodeLocation extractSourceCodeLocation(CssValueNode functionToEval) {
    return functionToEval.getParent().getParent().getSourceCodeLocation();
  }
}
