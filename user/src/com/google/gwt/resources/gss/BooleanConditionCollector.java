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

import com.google.gwt.thirdparty.common.css.compiler.ast.CssBooleanExpressionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssBooleanExpressionNode.Type;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompilerPass;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Visitor that collects the simple boolean conditions that are mapped to configuration
 * properties.
 * <p>
 * <code>
 *
 * &#064;if (MY_PROPERTY) {
 * ...
 * }
 * </code>
 * <p>will be evaluated to true if and only if a configuration property with the same name is set
 * to the value "true":
 * {@code
 * <set-configuration-property name="MY_PROPERTY" value="true" />
 * }
 */
public class BooleanConditionCollector extends ExtendedConditionalNodeVisitor
    implements CssCompilerPass {
  private final MutatingVisitController delegate;
  private final Set<String> booleanConditions;

  public BooleanConditionCollector(MutatingVisitController delegate) {
    this.delegate = delegate;
    booleanConditions = new HashSet<String>();
  }

  @Override
  public void enterBooleanExpression(CssBooleanExpressionNode booleanExpressionNode) {
    if (booleanExpressionNode.getType() == Type.CONSTANT) {

      Matcher m = PermutationsCollector.IS_FUNCTION.matcher(booleanExpressionNode.getValue());

      if (!m.matches()) {
        booleanConditions.add(booleanExpressionNode.getValue());
      }
    }
  }

  public Set<String> getBooleanConditions() {
    return ImmutableSet.copyOf(booleanConditions);
  }

  @Override
  public void runPass() {
    delegate.startVisit(this);
  }
}
