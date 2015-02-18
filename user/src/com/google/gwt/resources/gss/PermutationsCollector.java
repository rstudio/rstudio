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
import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Visitor that collects the different permutation axis defined in a gss file.
 */
public class PermutationsCollector extends ExtendedConditionalNodeVisitor
    implements CssCompilerPass {
  static final Pattern IS_FUNCTION =
      Pattern.compile("^is\\(([\"'])((?:(?!\\1).)*)\\1(?:,([\"'])((?:(?!\\3).)*)\\3)?\\)$");
  private static final String USER_AGENT_PERMUTATION = "user.agent";

  private final MutatingVisitController delegate;
  private final Set<String> permutationAxesSet;

  public PermutationsCollector(MutatingVisitController delegate) {
    this.delegate = delegate;
    permutationAxesSet = new HashSet<String>();
  }

  public void enterBooleanExpression(CssBooleanExpressionNode booleanExpressionNode) {
    if (booleanExpressionNode.getType() == Type.CONSTANT &&
        booleanExpressionNode.getValue() != null) {

      Matcher m = IS_FUNCTION.matcher(booleanExpressionNode.getValue());

      if (m.matches()) {
        String permutationName = m.group(2);
        String permutationValue = m.group(4);

        if (permutationValue == null) {
          permutationValue = permutationName;
          permutationName = USER_AGENT_PERMUTATION;
        }

        booleanExpressionNode.setValue(permutationName + ":" + permutationValue);

        if (!permutationAxesSet.contains(permutationName)) {
          permutationAxesSet.add(permutationName);
        }
      }
    }
  }

  public List<String> getPermutationAxes() {
    return ImmutableList.copyOf(permutationAxesSet);
  }

  @Override
  public void runPass() {
    delegate.startVisit(this);
  }
}
