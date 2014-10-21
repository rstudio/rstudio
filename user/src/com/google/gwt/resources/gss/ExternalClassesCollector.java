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

import com.google.gwt.thirdparty.common.css.SourceCodeLocation;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompilerPass;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompositeValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssLiteralNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssStringNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssUnknownAtRuleNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.DefaultTreeVisitor;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet.Builder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Compiler pass that collects external styles declared with the {@code @external} at-rule.
 *
 * <p>This pass also removes the {@code @external} nodes from the AST.
 */
public class ExternalClassesCollector extends DefaultTreeVisitor implements CssCompilerPass {
  public static final String EXTERNAL_AT_RULE = "external";
  private static final String STAR_SUFFIX = "*";

  private final MutatingVisitController visitController;
  private final ErrorManager errorManager;

  private Set<String> externalClassNames;
  private List<String> externalClassPrefixes;
  private boolean matchAll;

  public ExternalClassesCollector(MutatingVisitController visitController,
      ErrorManager errorManager) {
    this.visitController = visitController;
    this.errorManager = errorManager;
  }

  @Override
  public void runPass() {
    externalClassNames = new HashSet<String>();
    externalClassPrefixes = new ArrayList<String>();

    visitController.startVisit(this);
  }

  @Override
  public void leaveUnknownAtRule(CssUnknownAtRuleNode node) {
    if (EXTERNAL_AT_RULE.equals(node.getName().getValue())) {
      if (!matchAll) {
        processParameters(node.getParameters(), node.getSourceCodeLocation());
      }
      visitController.removeCurrentNode();
    }
  }

  /**
   * Returns an immutable set of external class names that should not be renamed.
   * The returned set contains all complete class names defined with
   * {@code @external} as well as all of the class names from
   * {@code styleClassesSet} that match prefixes defined with {@code @external}.
   *
   * @param styleClassesSet a set of class names that should be filtered to
   *     return those matching external prefixes. Note that the passed-in set is not
   *     modified.
   * @return an immutable set of class names. Note that the returned names are
   *     not prefixed with "."; they are the raw name.
   */
  public ImmutableSet<String> getExternalClassNames(Set<String> styleClassesSet) {
    if (matchAll) {
      return ImmutableSet.copyOf(styleClassesSet);
    }

    SortedSet<String> classNames = new TreeSet<String>(styleClassesSet);

    Builder<String> externalClassesSetBuilder = ImmutableSet.builder();
    externalClassesSetBuilder.addAll(externalClassNames);

    for (String prefix : externalClassPrefixes) {
      for (String styleClass : classNames.tailSet(prefix)) {
        if (styleClass.startsWith(prefix)) {
          externalClassesSetBuilder.add(styleClass);
        } else {
          break;
        }
      }
    }
    return externalClassesSetBuilder.build();
  }

  private void processParameters(List<CssValueNode> values, SourceCodeLocation sourceCodeLocation) {
    for (CssValueNode value : values) {
      if (value instanceof CssCompositeValueNode) {
        processParameters(((CssCompositeValueNode) value).getValues(), sourceCodeLocation);
      } else if (value instanceof CssStringNode) {
        String selector = ((CssStringNode) value).getConcreteValue();
        if (STAR_SUFFIX.equals(selector)) {
          matchAll = true;
          return;
        } else if (selector.endsWith(STAR_SUFFIX)) {
          externalClassPrefixes.add(selector.substring(0, selector.length() - 1));
        } else {
          externalClassNames.add(selector);
        }
      } else if (value instanceof CssLiteralNode) {
        externalClassNames.add(value.getValue());
      } else {
        errorManager.report(new GssError("External at-rule invalid. The following terms is not " +
            "accepted in an external at-rule [" + value.getValue() + "]", sourceCodeLocation));
      }
    }
  }
}
